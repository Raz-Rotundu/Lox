package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;


/**
 * LoxInstance -- A runtime representation of an instance of a lox class (an object)
 */
public class LoxInstance {
	private LoxClass klass;
	
	LoxInstance(LoxClass klass){
		this.klass = klass;
		
	}
	
	@Override
	public String toString() {
		return klass.name + " instance";
	}
}
