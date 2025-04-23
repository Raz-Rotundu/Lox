package com.craftinginterpreters.lox;

/**
 * Token -- The object containing token data Lox tokens.
 * Tracks token type, lexeme, literal representation, and line on which the token is found
 */
public class Token {
	final TokenType type;
	final String lexeme;
	final Object literal;
	final int line;
	
	Token(TokenType type, String lexeme, Object literal, int line){
		this.type = type;
		this.lexeme = lexeme;
		this.literal = literal;
		this.line = line;
	}
	
	public String toString() {
		return type + " " + lexeme + " " + literal + " ";
	}
}
