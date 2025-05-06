package com.craftinginterpreters.lox;

import java.util.List;

/**
 * LoxFunction -- Wrapper for Stmt.Function which also implements LoxCallable
 * 
 */
public class LoxFunction implements LoxCallable {
	private final Environment closure;
	
	private final boolean isInitializer;
	
	private final Stmt.Function declaration;
	
	LoxFunction(Stmt.Function declaration, Environment closure, boolean isInitializer){
		this.closure = closure;
		this.declaration = declaration;
		this.isInitializer = isInitializer;
	}
	
	/**
	 * Declare "this" as a variable and binds it to the generated instance, the instance which the method is being called from
	 * @param instance the instance from which the method is being called from
	 * @return Lox function with "this" closure
	 */
	LoxFunction bind(LoxInstance instance) {
		Environment environment = new Environment(closure);
		environment.define("this", instance);
		return new LoxFunction(declaration, environment, isInitializer);
	}
	
	@Override
	public String toString() {
		return "<fn " + declaration.name.lexeme + ">";
	}
	
	@Override
	public int arity() {
		return declaration.params.size();
	}

	@Override
	public Object call(Interpreter interpreter, List<Object> arguments) {
		
		Environment environment = new Environment(closure);
		for(int i = 0; i < declaration.params.size(); i++) {
			environment.define(declaration.params.get(i).lexeme, arguments.get(i));
		}
		
		try {
			interpreter.executeBlock(declaration.body, environment);
		} catch (Return returnValue) {
			if(isInitializer) return closure.getAt(0, "this");
			return returnValue.value;
		}
		
		// Always returns "this" even when directly called. Will be useful if I do CLox later
		if(isInitializer) return closure.getAt(0, "this");
		return null;
	}

}
