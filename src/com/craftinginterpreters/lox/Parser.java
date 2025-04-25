package com.craftinginterpreters.lox;

import java.util.List;

import static com.craftinginterpreters.lox.TokenType.*;

/**
 * Parser -- converts a list of tokens into their corresponding expressions 
 */
public class Parser {

	private final List<Token> tokens;
	private int current = 0;
	
	Parser(List<Token> tokens){
		this.tokens = tokens;
	}
}
