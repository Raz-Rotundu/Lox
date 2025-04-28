package com.craftinginterpreters.lox;

public class Interpreter implements Expr.Visitor<Object>{
	
	//Methods for each of the four possible expression trees
	
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
	
	private void checkNumberOperand(Token operator, Object operand) {
		if((operand instanceof Double)) return;
		throw new RuntimeError(operator, "Operand must be a number.");
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
				return (double)left - (double)right;
			case PLUS:
				if((left instanceof Double) && (right instanceof Double)) {
					return (double)left + (double)right;
				}
				if((left instanceof String) && (right instanceof String)) {
					return (String)left + (String)right;
				}
				break;
			case SLASH:
				return (double)left / (double)right;
			case STAR:
				return (double)left * (double)right;
				
			// Comparison operators
			case GREATER:
				return (double)left > (double)right;
			case GREATER_EQUAL:
				return (double) left >= (double)right;
			case LESS:
				return (double)left < (double)right;
			case LESS_EQUAL:
				return (double)left <= (double)right;
				
			// Equality operators
			case BANG_EQUAL: return !isEqual(left, right);
			case EQUAL_EQUAL: return isEqual(left, right);
		}
		
		// Unreachable
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
