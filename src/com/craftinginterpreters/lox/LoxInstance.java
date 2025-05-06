package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;


/**
 * LoxInstance -- A runtime representation of an instance of a lox class (an object)
 */
public class LoxInstance {
	private LoxClass klass; // Class this object is an instance of
	private final Map<String, Object> fields = new HashMap<>();
	
	
	LoxInstance(LoxClass klass){
		this.klass = klass;
		
	}
	
	/**
	 * Look up a property in the instance's fields
	 * @param name the name of the property to look up
	 * @return the value of that field
	 * @exception RuntimeError if querying a property that doesn't exist
	 */
	Object get(Token name) {
		if(fields.containsKey(name.lexeme)) {
			return fields.get(name.lexeme);
		}
		
		LoxFunction method = klass.findMethod(name.lexeme);
		if(method != null) return method.bind(this);
		
		throw new RuntimeError(name, "Undefined property '" + name.lexeme + "'.");
		
	}
	
	/**
	 * Stuff values directly into java map where fields are stored
	 * @param name the name of the field
	 * @param value the value of the field
	 */
	void set(Token name, Object value) {
		fields.put(name.lexeme, value);
	}
	
	@Override
	public String toString() {
		return klass.name + " instance";
	}
}
