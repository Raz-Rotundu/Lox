package com.craftinginterpreters.lox;

/**
 * Expr -- An abstract class defining the basic form of an expression if a lox syntax tree
 * Basically a variation of a binary tree node
 */
public abstract class Expr {
	
	
	// Binary expression structure
	static class Binary extends Expr{
		Binary(Expr left, Token operator, Expr right){
			this.left = left;
			this.right=  right;
			this.operator = operator;
		}
		final Expr left;
		final Expr right;
		final Token operator;
	}
	
	// Subclasses packed in here just to keep things tidy
	
	
}
