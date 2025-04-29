package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

/**
 * Environment- Class that contains bindings associating variables to values
 */
public class Environment {
	
	private final Map<String, Object> values = new HashMap<String, Object>();
	
	
	/**
	 * Retrieves the appropriate mapping from the environment, throws RuntimeError if not present
	 * @param name the token containing the variable name
	 * @return value mapped to that variable if it exists
	 */
	Object get(Token name) {
		if(values.containsKey(name.lexeme)) {
			return values.get(name.lexeme);
		}
		
		throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
	}
	
	/**
	 * Defines a binding between variable and value. Can also be used to redefine variables
	 * @param name The variable name
	 * @param value The variable value
	 */
	void define(String name, Object value) {
		values.put(name, value);
	}
	

}
