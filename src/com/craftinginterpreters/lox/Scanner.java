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
		
		// Single char lexemes
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
		
		// Operators
		case('!'):
			addToken(match('=') ? BANG_EQUAL : BANG);
			break;
		case('='):
			addToken(match('=')? EQUAL_EQUAL : EQUAL);
			break;
		case('<'):
			addToken(match('=')? LESS_EQUAL : LESS);
			break;
		case('>'):
			addToken(match('=')? GREATER_EQUAL : GREATER);
			break;
		
		// Special handling for / because it could mean division by itself or comment as '//'
		case('/'):
			if(match('/')) {
				// Commented line
				while(peek() != '\n' && !isAtEnd()) advance();
			} else {
				addToken(SLASH);
			}
		break;
		
		// Ignoring meaningless chars, and starting new line on newline
		case(' '):
		case('\r'):
		case('t'):
			break;
		
		case('\n'):
			line++;
			break;
			
		// String literals
		case('"'): String(); break;
		
		
		// Reserved words and identifiers
		
		default:
			// Number literals
			if(isDigit(c)) {
				number();
			} else {
				Lox.error(line, "Unexpected character");
			}
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
	
	/**
	 * Helper to scanToken. Only consumes the character if it's the one we're looking for
	 * @param expected the expected next character
	 * @return true or false if the given character is the next one
	 */
	private boolean match(char expected) {
		if(isAtEnd()) return false;
		if(source.charAt(current) != expected) return false;
		
		current++;
		return true;
	}
	
	/**
	 * Peeks ahead one character without consuming it
	 * @return next char or null if line is ended
	 */
	private char peek() {
		if(isAtEnd()) return '\0';
		return source.charAt(current);
	}
	
	/**
	 * Consumes characters until the terminal " is reached
	 * Gracefully handles unterminated strings
	 * Token creation also produces the value parser will be using later on 
	 */
	private void String() {
		// Consume chars until either next " or end of line is reached
		while(peek() != '"' && !isAtEnd()) {
			if (peek() == '\n') line++;
			advance();
		}
		
		if(isAtEnd()) {
			Lox.error(line, "Unterminated string");
			return;
		}
		
		// Closing "
		advance();
		
		// Strip quotes and get value
		String value = source.substring(start + 1, current -1);
		addToken(STRING, value);
	}
	
	/**
	 * Helper to scanToken. Checks if character is a digit or not
	 * @param c The character to examine
	 * @return Boolean if the character represents a digit
	 */
	private boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}
	
	/**
	 * Consumes digits until a decimal is reached, then consumes after the decimal point
	 * Creates resulting number token with value that parser will be using later on
	 */
	private void number() {
		while(isDigit(peek())) advance();
		
		// Look for a fractional part
		if(peek() == '.' && isDigit(peekNext())) {
			
			// Consume the '.'
			advance();
			
			while(isDigit(peek())) advance();
		}
		
		// Add the complete token to list
		addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
	}
	
	private char peekNext() {
		if(current + 1 >= source.length()) return '\0';
		return source.charAt(current + 1);
	}
}
