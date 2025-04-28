package com.craftinginterpreters.lox;


/**
 * Exception raised at runtime if the interpreter encounters expressions with incorrect operands
 */ 
public class RuntimeError extends RuntimeException {
	final Token token;
	
	RuntimeError(Token token, String message){
		super(message);
		this.token = token;
	}
}
