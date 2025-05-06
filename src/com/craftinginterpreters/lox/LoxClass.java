package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Map;

/**
 * LoxClass -- The runtime representation of a Lox class
 */
public class LoxClass implements LoxCallable {
	final String name;
	private final Map<String, LoxFunction> methods;
	
	LoxClass(String name, Map<String, LoxFunction> methods){
		this.name = name;
		this.methods = methods;
	}
	
	LoxFunction findMethod(String name) {
		if(methods.containsKey(name)) {
			return methods.get(name);
		}
		
		return null;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	/**
	 * Calling a class creates a new LoxInstance for the called class and returns it
	 * Checks for an "init" method. If found, bind and invoke like a normal method call
	 * @param interpreter an interpreter
	 * @param arguments the list of arguments to call object with (constructor params
	 * @return a new instance of the called class
	 */
	@Override
	public Object call(Interpreter interpreter, List<Object> arguments) {
		LoxInstance instance = new LoxInstance(this);
		
		LoxFunction initializer = findMethod("init");
		if(initializer != null) {
			initializer.bind(instance).call(interpreter, arguments);
		}
		
		return instance;
	}
	
	/**
	 * Function to determine the arity of the class (arity of constructor)
	 * If there is an initializer, that method's arity determines how many arguments must be passed
	 */
	@Override
	public int arity() {
		LoxFunction initializer = findMethod("init");
		if(initializer == null) return 0;
		return initializer.arity();
	}
}
