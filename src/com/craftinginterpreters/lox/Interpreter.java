package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void>{
	private final Map<Expr, Integer> locals = new HashMap<>();
	final Environment globals = new Environment(); // Fixed reference to outermost environment
	private Environment environment = globals;
	
	/**
	 * Define a new variable named "clock" whose value is an anonymous class implementing LoxCallable
	 */
	Interpreter(){
		globals.define("clock", new LoxCallable(){
			@Override
			public int arity() {return 0;}
			
			@Override
			public Object call(Interpreter interpreter, List<Object> arguments) {
				return (double)System.currentTimeMillis() / 1000.0;
			}
			
			@Override
			public String toString() {return "<native fn>"; }
		});
	}
	
	/**
	 * Public API of the interpreter, calls and interprets expressions
	 * @param expression the expression to be interpreted
	 */
	void interpret(List<Stmt> statements) {
		try {
			for(Stmt statement : statements) {
				execute(statement);
			}
		} catch (RuntimeError error) {
			Lox.runtimeError(error);
		}
	}
	
	/**
	 * Convert expression value to string. 
	 * Lox version of null is "nil", so that is converted
	 * Lox integers are appended with ".0" and that part is removed
	 * @param object the result of a Lox expression
	 * @return Lox equivalent string representation of the given expression
	 */
	private String stringify(Object object) {
		if(object == null) return "nil";
		
		if((object instanceof Double)) {
			String text = object.toString();
			// Remove trailing ".0" on non-decimal numbers
			if(text.endsWith(".0")) {
				text = text.substring(0, text.length() - 2);
			}
			return text;
		}
		return object.toString();
	}
	
	/**
	 * Interprets return statements
	 * If there is a return value it is evaluated, otherwise it is set to nil
	 * That value is then wrapped in a custom exception class and thrown.
	 */
	@Override
	public Void visitReturnStmt(Stmt.Return stmt) {
		Object value = null;
		if(stmt.value != null) value = evaluate(stmt.value);
		
		throw new Return(value);
	}
	
	/**
	 * Interprets function declarations.
	 * Convert function syntax node to runtime representation by wrapping it in a LoxFunction.
	 * Creates a new binding in the current environment and stores a reference to the function there.
	 */
	@Override
	public Void visitFunctionStmt(Stmt.Function stmt) {
		LoxFunction function = new LoxFunction(stmt, environment, false);
		environment.define(stmt.name.lexeme, function);
		return null;
	}
	
	/**
	 * Evaluates functions by casting them to LoxCallable, and calling the call() method on them
	 * Checks if object is an instance of LoxCallable prior to cast, throws error if not
	 */
	@Override
	public Object visitCallExpr(Expr.Call expr) {
		Object callee = evaluate(expr.callee);
		
		List<Object> arguments = new ArrayList<>();
		
		for(Expr argument : expr.arguments) {
			arguments.add(evaluate(argument));
		}
		
		// Correct type check
		if(!(callee instanceof LoxCallable)) {
			throw new RuntimeError(expr.paren, "Can only call functions and classes.");
		}
		
		LoxCallable function = (LoxCallable)callee;
		
		// Arity check
		if(arguments.size() != function.arity()) {
			throw new RuntimeError(expr.paren, 
					"Expected " +
					 function.arity() + " arguments but got " +
					 arguments.size() + ".");
		}
		
		return function.call(this, arguments);
	}
	
	/**
	 * Evaluate the expression whose property is being accessed, 
	 * @exception RuntimeError thrown if object is not an instance of a Lox class
	 * @param the GET expression to be evaluated
	 */
	@Override
	public Object visitGetExpr(Expr.Get expr) {
		Object object = evaluate(expr.object);
		if((object instanceof LoxInstance)) {
			return ((LoxInstance) object).get(expr.name);
		}
		
		throw new RuntimeError(expr.name, "Only instances have properties.");
	}
	
	/**
	 * Evaluates while statements
	 * Checks truthiness of while condition before executing the loop
	 */
	@Override
	public Void visitWhileStmt(Stmt.While stmt) {
		while(isTruthy(evaluate(stmt.condition))) {
			execute(stmt.body);
		}
		return null;
	}
	
	/**
	 * Evaluates logical OR or AND expressions
	 * Checks the left value first to see if we can short-circuit.
	 */
	@Override
	public Object visitLogicalExpr(Expr.Logical expr) {
		Object left = evaluate(expr.left);
		
		if(expr.operator.type == TokenType.OR) {
			if (isTruthy(left)) return left;
		} else {
			if(!isTruthy(left)) return left;  
		}
		
		return evaluate(expr.right);
	}
	
	/**
	 * Evaluates SET expressions on Lox class instances
	 * Evaluates the value being set and stores it in the Lox instance
	 * @exception throws RuntimeError if the object is not an instance of a Lox class
	 * @return the result of the Set expression
	 */
	@Override
	public Object visitSetExpr(Expr.Set expr) {
		Object object = evaluate(expr.object);
		
		if(!(object instanceof LoxInstance)) {
			throw new RuntimeError(expr.name, "Only instances have fields.");
		}
		
		Object value = evaluate(expr.value);
		((LoxInstance) object).set(expr.name, value);
		return value;
	}
	
	/**
	 * Evaluate "this" expressions is the same way as variables
	 * @param expr "this" expression to be evaluated
	 * @return result of lookUpVariable
	 */
	@Override
	public Object visitThisExpr(Expr.This expr) {
		return lookUpVariable(expr.keyword, expr);
		
	}
	/**
	 * Evaluates if statement expression trees
	 * If the condition is truthy, execute then branch, otherwise execute else branch if there is one
	 */
	@Override
	public Void visitIfStmt(Stmt.If stmt) {
		if(isTruthy(evaluate(stmt.condition))) {
			execute(stmt.thenBranch);
		} else if(stmt.elseBranch != null) {
			execute(stmt.elseBranch);
		}
		return null;
	}
	
	/**
	 * Evaluate assignment expression tree
	 * Evaluates the expression, then assigns maps it to the variable's name in the environment
	 * @return Expression's value
	 */
	@Override
	public Object visitAssignExpr(Expr.Assign expr) {
		Object value = evaluate(expr.value);
		
		Integer distance = locals.get(expr);
		if(distance != null) {
			environment.assignAt(distance, expr.name, value);
			
		} else {
			globals.assign(expr.name, value);
		}
		
		return value;
	}
	
	
	
	/**
	 * Evaluate a declaration statement
	 * If the variable has an initializer it gets evaluated, otherwise variable is set to nil if not initialized
	 * @param Variable statement to be evaluated
	 */
	@Override
	public Void visitVarStmt(Stmt.Var stmt) {
		Object value = null;
		if(stmt.initializer != null) {
			value = evaluate(stmt.initializer);
			
		}
		
		environment.define(stmt.name.lexeme, value);
		return null;
	}
	

	/**
	 * Evaluate a variable expression
	 * Forwards to the environment, which will make sure the variable is defined
	 * @param the Variable expression to be evaluated
	 * @return the value corresponding to the variable
	 */
	@Override
	public Object visitVariableExpr(Expr.Variable expr) {
		return lookUpVariable(expr.name, expr);
	}
	
	/**
	 * Looks up resolved distance in the map(locals), if not present, look it up dynamically in the global environment
	 * @throws runtimeError if variable isn't defined
	 * @param name the token containing the name of the variable
	 * @param expr the expression in which the variable is involvec
	 * @return resolved distance of the given variable
	 */
	private Object lookUpVariable(Token name, Expr expr) {
		Integer distance = locals.get(expr);
		
		if(distance != null) {
			return environment.getAt(distance, name.lexeme);
		} else {
			return globals.get(name);
		}
	}
	
	/**
	 * Evaluate a literal
	 * Pulls the runtime value of the literal tree node and returns it
	 * @param expr the literal expression to be evaluated
	 * @return The run time value of the literal node
	 */
	@Override
	public Object visitLiteralExpr(Expr.Literal expr) {
		return expr.Value;
	}
	
	

	/**
	 * Evaluate a grouping
	 * Recursively evaluate the expression within the parentheses and returns it
	 * @param expr the grouping expression to be evaluated
	 * @return evaluation of the grouping expression itself
	 */
	@Override
	public Object visitGroupingExpr(Expr.Grouping expr) {
		return evaluate(expr.expression);
	}
	

	/**
	 * Helper method that sends the expression back into the interpreter's visitor implementation
	 * @param expr the expression to be evaluated
	 * @return the evaluated expression
	 */
	private Object evaluate(Expr expr) {
		return expr.accept(this);
	}
	
	/**
	 * Helper method that sends the statement back into the interpreter's visitor implementation
	 * @param stmt the statement to be evaluated
	 */
	private void execute(Stmt stmt) {
		stmt.accept(this);
	}
	
	/**
	 * Map expression, and number of scoped between it and the scope in which its variables were defined
	 * @param expr the expression to resolve
	 * @param depth the 
	 */
	void resolve(Expr expr, int depth) {
		locals.put(expr, depth);
	}
	
	/**
	 * Evaluate a unary expression
	 * Recursively evaluate the right object, then evaluate the operator after the subexpression
	 * @param expr the Unary expression to be evaluated
	 * @return the evaluated value of the unary expression
	 */
	@Override
	public Object visitUnaryExpr(Expr.Unary expr) {
		Object right = evaluate(expr.right);
		
		switch(expr.operator.type) {
			case BANG:
				return(!(isTruthy(right)));
			case MINUS:
				checkNumberOperand(expr.operator, right);
				return -(double)right;
		}
		
		
		// Unreachable
		return null;
	}
	
	// Unary expresison operand check
	/**
	 * Checks that the operand is a number and throws a RuntimeError if not
	 * @param the token to be inspected
	 * @param the operand whose type needs to be checked
	 */
	private void checkNumberOperand(Token operator, Object operand) {
		if((operand instanceof Double)) return;
		throw new RuntimeError(operator, "Operand must be a number.");
	}
	
	// Binary expression operand check
	/**
	 * Checks that both operands of a binary expression are numbers and throws a RuntimeError if not
	 * @param operator The token to be inspected
	 * @param left operand
	 * @param right operand
	 */
	private void checkNumberOperands(Token operator, Object left, Object right) {
		if((left instanceof Double) && (right instanceof Double)) return;
		throw new RuntimeError(operator, "Operands must be numbers");
	}
	
	/**
	 * Helper to evaluate truthiness of an object
	 * False and nil are falsey, everything else is truthey
	 * @param object the object to evaluate
	 * @return the truthiness value of the given object
	 */
	private boolean isTruthy(Object object) {
		if(object == null) return false;
		if(object instanceof Boolean) return (boolean)object;
		return true;
	}
	
	/**
	 * Evaluate binary expressions
	 * Recursively evaluate left side and right side, then perform the correct operation on the result
	 * @return The value of the binary expression
	 */
	@Override
	public Object visitBinaryExpr(Expr.Binary expr) {
		Object left = evaluate(expr.left);
		Object right = evaluate(expr.right);
		
		switch(expr.operator.type) {
		
			// Arithmetic operators
			case MINUS:
				checkNumberOperands(expr.operator, left, right);
				return (double)left - (double)right;
				
			case PLUS:
				if((left instanceof Double) && (right instanceof Double)) {
					return (double)left + (double)right;
				}
				if((left instanceof String) && (right instanceof String)) {
					return (String)left + (String)right;
				}	
				throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings");
				
			case SLASH:
				checkNumberOperands(expr.operator, left, right);
				return (double)left / (double)right;
			case STAR:
				checkNumberOperands(expr.operator, left, right);
				return (double)left * (double)right;
				
			// Comparison operators
			case GREATER:
				checkNumberOperands(expr.operator, left, right);
				return (double)left > (double)right;
			case GREATER_EQUAL:
				checkNumberOperands(expr.operator, left, right);
				return (double) left >= (double)right;
			case LESS:
				checkNumberOperands(expr.operator, left, right);
				return (double)left < (double)right;
			case LESS_EQUAL:
				checkNumberOperands(expr.operator, left, right);
				return (double)left <= (double)right;
				
			// Equality operators
			case BANG_EQUAL: return !isEqual(left, right);
			case EQUAL_EQUAL: return isEqual(left, right);
		}
		
		// Unreachable
		return null;
	}
	

	/**
	 * Evaluate inner expression, but do not return anything (Expression statements have no return)
	 * @param The statement to be parsed
	 */
	@Override
	public Void visitExpressionStmt(Stmt.Expression stmt) {
		evaluate(stmt.expression);
		return null;
	}
	
	/**
	 * Evaluate inner expression, but do not return anything (Print expressions have no return)
	 * Prints out expression value to stdOut before discarding
	 * @param The print statement to be parsed
	 */
	@Override
	public Void visitPrintStmt(Stmt.Print stmt) {
		Object value = evaluate(stmt.expression);
		System.out.println(stringify(value));
		
		return null;
	}
	
	/**
	 * Executes all statements within the block, passing environment as reference
	 * @param stmt the block statement to be interpreted
	 */
	@Override
	public Void visitBlockStmt(Stmt.Block stmt) {
		executeBlock(stmt.statements, new Environment(environment));
		return null;
	}
	
	/**
	 * Defines class and assigns it to current environment
	 * @param stmt class definition statement
	 */
	@Override
	public Void visitClassStmt(Stmt.Class stmt) {
		environment.define(stmt.name.lexeme, null);
		
		Map<String, LoxFunction> methods = new HashMap<>();
		for(Stmt.Function method : stmt.methods) {
			LoxFunction function = new LoxFunction(method, environment, method.name.lexeme.equals("init"));
			methods.put(method.name.lexeme, function);
		}
		
		LoxClass klass = new LoxClass(stmt.name.lexeme, methods);
		
		environment.assign(stmt.name, klass);
		return null;
	}
	
	/**
	 * Evaluates equality based on Lox's notion of equality
	 * Handles null parameters so that we don't throw nullPointerExceptions from calling equals on null
	 * @param a the first object to be compared
	 * @param b the second object to be compared
	 * @return boolean T/F is the objects are equal to each other
	 */
	private boolean isEqual(Object a, Object b) {
		if((a == null) && (b == null)) return true;
		if(a == null) return false;
		
		return a.equals(b);
	}
	


	// Executes list of statements in the context of a given environment
	void executeBlock(List<Stmt> statements, Environment environment) {
		Environment previous = this.environment;
		
		try {
			this.environment = environment;
			
			for(Stmt statement : statements) {
				execute(statement);
			}
		} finally {
			this.environment = previous;
		}
	}
	
}
