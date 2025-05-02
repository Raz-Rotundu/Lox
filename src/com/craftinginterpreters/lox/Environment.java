package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

/**
 * Environment- Class that contains bindings associating variables to values
 */
public class Environment {
	
	final Environment enclosing;
	
	private final Map<String, Object> values = new HashMap<String, Object>();
	
	Environment(){
		enclosing = null;
		
	}
	
	Environment(Environment enclosing){
		this.enclosing = enclosing;
	}
	/**
	 * Assigns a value to an existing variable. Throws runtime error if the variable does not exist
	 * @param name the token representing the variable
	 * @param value the value to be assigned to the variable
	 */
	void assign(Token name, Object value) {
		if(values.containsKey(name.lexeme)) {
			values.put(name.lexeme, value);
			return;
		}
		
		if(enclosing != null) {
			enclosing.assign(name, value);
			return;
		}
		
		throw new RuntimeError(name,
				"Undefined variable '" + name.lexeme + "'.");
	}
	
	/**
	 * Retrieves the appropriate mapping from the environment, throws RuntimeError if not present
	 * @param name the token containing the variable name
	 * @return value mapped to that variable if it exists
	 */
	Object get(Token name) {
		if(values.containsKey(name.lexeme)) {
			return values.get(name.lexeme);
		}
		
		if(enclosing != null) return enclosing.get(name);
		
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
	
	/**
	 * Returns the value of the given variable in the map of given distance
	 * It does not check for prescence of the variable at the given distance, because the resolver would have found it there first 
	 * @param distance the distance of the target environment from the current one
	 * @param name the name of the target variable
	 * @return the value of the target variable
	 */
	Object getAt(int distance, String name) {
		return ancestor(distance).values.get(name);
	}

	/**
	 * Maps variable to value in the environment a certain distance away
	 * @param distance the distance between this environment and the enclosing one
	 * @param name the token containing the variable name
	 * @param value the value of the variable
	 */
	void assignAt(int distance, Token name, Object value) {
		ancestor(distance).values.put(name.lexeme, value);
	}
	/**
	 * Walks a number of environments up the chain and returns the environment there
	 * @param distance the numbe of enviromnments up the chain to go
	 * @return the environment at the given distance from the current one
	 */
	Environment ancestor(int distance) {
		Environment environment = this;
		
		for(int i = 0; i < distance; i++) {
			environment = environment.enclosing;
		}
		
		return environment;
	}
}
