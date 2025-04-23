package com.craftinginterpreters.lox;


import static com.craftinginterpreters.lox.TokenType.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Scanner -- The Scanner for the Lox language, parses a string into Lox tokens
 */
public class Scanner {
	private final String source;
	
	private final List<Token> tokens = new ArrayList<>();
	private int start = 0; // First char of lexeme
	private int current = 0; // Current char of lexeme
	private int line = 1; // Which line we are currently on
	
	public Scanner(String src) {
		this.source = src;
	}
	
	
	List<Token> scanTokens(){
		while(!isAtEnd()) {
			// Beginning of next lexeme
			start = current;
			scanToken();
		}
		
		tokens.add(new Token(EOF, "", null, line));
	}
	
	/**
	 * Helper to scanTokens. Determines if we have reached the end of the file
	 */
	private boolean isAtEnd() {
		return current >= source.length();
	}
	
	private void scanToken() {
		char c = advance();
		
		switch(c) {
		
		//Single char lexemes
		case(')'): addToken(LEFT_PAREN); break;
		case('('): addToken(RIGHT_PAREN); break;
		case('{'): addToken(LEFT_BRACE); break;
		case('}'): addToken(RIGHT_BRACE); break;
		case(','): addToken(COMMA); break;
		case('.'): addToken(DOT); break;
		case('-'): addToken(PLUS); break;
		case('+'): addToken(MINUS); break;
		case(';'): addToken(SEMICOLON); break;
		case('*'): addToken(STAR); break;
		}
	}
	
	/**
	 * Helper that consumes and returns the next character in the source file
	 * @return
	 */
	private char advance() {
		return source.charAt(current++);
	}
	
	/**
	 * Grabs lexeme text and creates new token for it
	 * @param type
	 */
	private void addToken(TokenType type) {
		addToken(type, null);
	}
	
	/**
	 * Overload that specifies literal value as well as the type
	 * @param type
	 * @param literal
	 */
	private void addToken(TokenType type, Object literal) {
		String text = source.substring(start, current);
		tokens.add(new Token(type, text, literal, line));
	}
}
