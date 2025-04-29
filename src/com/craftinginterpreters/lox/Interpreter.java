package com.craftinginterpreters.lox;

import java.util.List;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void>{
	
	private Environment environment = new Environment();
	
	
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
	
	
	//Methods for each of the expression trees
	
	// Evaluate a declaration statement
	@Override
	public Void visitVarStmt(Stmt.Var stmt) {
		Object value = null;
		if(stmt.initializer != null) {
			value = evaluate(stmt.initializer);
			
		}
		
		environment.define(stmt.name.lexeme, value);
		return null;
	}
	
	// Evaluate a variable expression
	@Override
	public Object visitVariableExpr(Expr.Variable expr) {
		return environment.get(expr.name);
	}
	/**
	 * Evaluate a literal
	 * Pulls the runtime value of the literal tree node and returns it
	 * @return The run time value of the literal node
	 */
	@Override
	public Object visitLiteralExpr(Expr.Literal expr) {
		return expr.Value;
	}
	
	

	/**
	 * Evaluate a grouping
	 * Recursively evaluate the expression within the parentheses and returns it
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
	
	
}
