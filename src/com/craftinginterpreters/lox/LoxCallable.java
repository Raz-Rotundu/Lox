package com.craftinginterpreters.lox;

import java.util.List;

/**
 * LoxCallable -- Java representation of any Lox object that can be called like a function
 * Callees are cast to LoxCallables, and then the call() method is applied to them
 */
public interface LoxCallable {
	int arity();
	Object call(Interpreter interpreter, List<Object> arguments);

}