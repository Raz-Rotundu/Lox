package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Resolver -- A class which visits each node of the syntax tree, performing static analysis.
 * Each time it visits a variable, it will tell the interpreter how many scopes are between the current scope and the scope in which it was defined
 * AKA At runtime counts the number of environments between THIS one and the enclosing one where the interpreter can find the variable's value
 */
public class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void>{

	private final Interpreter interpreter;
	private final Stack<Map<String, Boolean>> scopes = new Stack<>();
	
	private FunctionType currentFunction = FunctionType.NONE;
	
	Resolver(Interpreter interpreter){
		this.interpreter = interpreter;
	}
	
	private enum FunctionType{
		NONE,
		FUNCTION
	}
	
	/**
	 * Begins new scope, traverses into the methods inside the block, then discards scope
	 * @param a syntax tree statement
	 */
	@Override
	public Void visitBlockStmt(Stmt.Block stmt) {
		beginScope();
		
		resolve(stmt.statements);
		endScope();
		return null;
	}
	
	/**
	 * Declare and define a class using its name for now
	 * @param A class declaration statement
	 */
	@Override
	public Void visitClassStmt(Stmt.Class stmt) {
		declare(stmt.name);
		define(stmt.name);
		return null;
		
	}
	/**
	 * Resolves a declaration by adding it to the innermost scope's map
	 * @param stmt variable declaration statement
	 */
	@Override
	public Void visitVarStmt(Stmt.Var stmt) {
		declare(stmt.name);
		if(stmt.initializer != null) {
			resolve(stmt.initializer);
		}
		
		define(stmt.name);
		return null;
	}
	
	/**
	 * Visitor to resolve variable expressions
	 * @param the variable expression to resolve
	 */
	@Override
	public Void visitVariableExpr(Expr.Variable expr) {
		if(!scopes.isEmpty() && 
				scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
			Lox.error(expr.name, "Can't read local variable in its own initializer.");
		};
		
		resolveLocal(expr, expr.name);
		return null;
	}
	
	/**
	 * Visitor to resolve variable assignment expressions
	 * Expression is resolved first in case it contains reference to other variables, followed by the variable assignment
	 * @param expr the assignment expression to be resolved
	 */
	@Override 
	public Void visitAssignExpr(Expr.Assign expr) {
		resolve(expr.value);
		resolveLocal(expr, expr.name);
		return null;
	}
	
	/**
	 * Visitor to resolve function declarations
	 * Name of the function is bound to the outer scope
	 * Function parameters are bound to the inner scope
	 * @param stmt the statement containing the function declaration
	 */
	@Override
	public Void visitFunctionStmt(Stmt.Function stmt) {
		declare(stmt.name);
		define(stmt.name);
		
		resolveFunction(stmt, FunctionType.FUNCTION);
		return null;
	}
	
	
	// Boring ones
	/**
	 * Visit singular expression
	 * @param stmt statement containing the expression to traverse
	 */
	@Override
	public Void visitExpressionStmt(Stmt.Expression stmt) {
		resolve(stmt.expression);
		return null;
	}
	
	/**
	 * Visit if statement
	 * @param stmt statement containing the if statement to traverse
	 */
	@Override
	public Void visitIfStmt(Stmt.If stmt) {
		resolve(stmt.condition);
		resolve(stmt.thenBranch);
		
		if(stmt.elseBranch != null) resolve(stmt.elseBranch);
		return null;
	}
	
	/**
	 * Visit print statements
	 * @param stmt the print statement to traverse
	 */
	@Override
	public Void visitPrintStmt(Stmt.Print stmt) {
		resolve(stmt.expression);
		return null;
	}
	
	/**
	 * Visit return statements
	 * @param stmt the return statement to traverse
	 */
	@Override
	public Void visitReturnStmt(Stmt.Return stmt) {
		if(currentFunction == FunctionType.NONE) {
			Lox.error(stmt.keyword, "Can't return from top-level code.");
		}
		if(stmt.value != null) {
			resolve(stmt.value);
		}
		return null;
	}
	
	/**
	 * Visit while statements
	 * Resolve its condition and resolve the body exactly once
	 * @param stmt the while statement to be resolved
	 */
	@Override
	public Void visitWhileStmt(Stmt.While stmt) {
		resolve(stmt.condition);
		resolve(stmt.body);
		return null;
	}
	
	/**
	 * Visit binary expressions
	 * @param expr the binary expression to traverse
	 */
	@Override
	public Void visitBinaryExpr(Expr.Binary expr) {
		resolve(expr.left);
		resolve(expr.right);
		return null;
	}
	
	/**
	 * Visit call expressions
	 * Resolve the callee, then iterate through argument list and resolve them all
	 * @param expr the call expression to be resolved
	 */
	@Override
	public Void visitCallExpr(Expr.Call expr) {
		resolve(expr.callee);
		
		for(Expr argument : expr.arguments) {
			resolve(argument);
		}
		
		return null;
	}
	
	/**
	 * Visit grouping (bracket) expressions
	 * @param expr the bracket expression to be resolved
	 */
	@Override
	public Void visitGroupingExpr(Expr.Grouping expr) {
		resolve(expr.expression);
		return null;
	}
	
	/**
	 * Visit literal expressions
	 * Do nothing, since literals have no variables involved
	 * @param texpr the literal expression to be resolved
	 */
	@Override
	public Void visitLiteralExpr(Expr.Literal expr) {
		return null;
	}
	
	/**
	 * Visit logical expressions
	 * No control flow or short-circuiting, so handled same as other binary expressions
	 * @param expr the binary expression to resolve
	 */
	@Override
	public Void visitLogicalExpr(Expr.Logical expr) {
		resolve(expr.left);
		resolve(expr.right);
		return null;
	}
	
	/**
	 * Visit unary expression
	 * Resolves its one operand
	 * @param the unary expression to resolve
	 */
	@Override
	public Void visitUnaryExpr(Expr.Unary expr) {
		resolve(expr.right);
		return null;
	}
	
	/**
	 * For resolving a list of statements
	 * @param stmt
	 */
	void resolve(List<Stmt> statements) {
		for(Stmt statement : statements) {
			resolve(statement);
		}
	}
	
	/**
	 * Resolving an individiual statement
	 * applies the visitor to the syntax tree node
	 * @param stmt
	 */
	private void resolve(Stmt stmt) {
		stmt.accept(this);
	}
	 
	/**
	 * Push a scope onto the scope stack
	 */
	private void beginScope() {
		scopes.push(new HashMap<String, Boolean>());
	}
	
	/**
	 * Pop a scope off the scope stack
	 */
	private void endScope() {
		scopes.pop();
	}
	
	/** 
	 * Adds the variable to the innermost scope so that it shadows any outer one
	 * Marked as "not ready" by being mapped to false in the scope
	 * @param the token representing the variable's name
	 */
	private void declare(Token name) {
		if(scopes.isEmpty()) return;
		
		Map<String, Boolean> scope = scopes.peek();
		if(scope.containsKey(name.lexeme)) {
			Lox.error(name, "Already a variable with this name in this scope.");
		}
		scope.put(name.lexeme, false);
	}
	
	/**
	 * Set the variables value to true in the scope map to mark it as fully initialized
	 * @param name the token containing the name of the variable
	 */
	private void define(Token name) {
		if(scopes.isEmpty()) return;
		scopes.peek().put(name.lexeme, true);
	}
	
	/**
	 * Helper method to actually resolve the variable itself
	 * @param expr the expression in which the variable is used
	 * @param name the token representing the variable's name
	 */
	private void resolveLocal(Expr expr, Token name) {
		for(int i = scopes.size() - 1; i >= 0; i --) {
			if(scopes.get(i).containsKey(name.lexeme)) {
				interpreter.resolve(expr, scopes.size() - 1 - i); //todo implement the resolve method
				return;
			}
		}
	}
	
	/**
	 * For resolving an expression
	 * @param expr the expression to be resolved
	 */
	private void resolve(Expr expr) {
		expr.accept(this);
	}
	
	/**
	 * Helper function for resolving the function's body
	 * Creates a new scope for the body and binds variables for each of the function's parameters
	 * @param function
	 */
	private void resolveFunction(Stmt.Function function, FunctionType type) {
		FunctionType enclosingFunction = currentFunction;
		currentFunction = type;
		
		beginScope();
		for(Token param : function.params) {
			declare(param);
			define(param);	
		}
		resolve(function.body);
		endScope();
		
		currentFunction = enclosingFunction;
	}
	
}
