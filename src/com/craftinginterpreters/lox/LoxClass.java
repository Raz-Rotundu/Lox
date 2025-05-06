package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Map;

/**
 * LoxClass -- The runtime representation of a Lox class
 */
public class LoxClass implements LoxCallable {
	final String name;
	
	LoxClass(String name){
		this.name = name;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	/**
	 * Calling a class creates a new LoxInstance for the called class and returns it
	 * @param interpreter an interpreter
	 * @param arguments the list of arguments to call object with (constructor params
	 * @return a new instance of the called class
	 */
	@Override
	public Object call(Interpreter interpreter, List<Object> arguments) {
		LoxInstance instance = new LoxInstance(this);
		
		return instance;
	}
	
	@Override
	public int arity() {
		return 0;
	}
}
