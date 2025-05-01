package com.craftinginterpreters.lox;

/**
 * Return --  Wrapper exception which holds the return value of a return statement
 * This is used to pass return value up the interpreter call stack all the way back to the code that began executing the body
 */
public class Return extends RuntimeException{
	final Object value;
	
	Return(Object value){
		super(null, null, false, false);
		this.value = value;
	}
}
