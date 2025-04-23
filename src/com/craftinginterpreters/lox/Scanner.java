package com.craftinginterpreters.lox;


import static com.craftinginterpreters.lox.TokenType.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scanner -- The Scanner for the Lox language, parses a string into Lox tokens
 */
public class Scanner {
	private final String source;
	
	private final List<Token> tokens = new ArrayList<>();
	private int start = 0; // First char of lexeme
	private int current = 0; // Current char of lexeme
	private int line = 1; // Which line we are currently on
	
	private static final Map<String, TokenType> keywords;
	
	// Map of reserved word strings to token types
	static {
		keywords = new HashMap<String, TokenType>();
		
		keywords.put("and", AND);
		keywords.put("class", CLASS);
		keywords.put("else", ELSE);
		keywords.put("false", FALSE);
		
		keywords.put("for", FOR);
		keywords.put("fun", FUN);
		keywords.put("if", IF);
		keywords.put("NIL", NIL);
		
		keywords.put("or", OR);
		keywords.put("print", PRINT);
		keywords.put("return", RETURN);
		keywords.put("super", SUPER);
		
		keywords.put("this", THIS);
		keywords.put("true", TRUE);
		keywords.put("var", VAR);
		keywords.put("while", WHILE);
	}
	
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
		return tokens;
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
		
		
		// Reserved words "OR"
		case('o'):
			if(match('r')) {
				addToken(OR);
				
			}
			break;
			
		default:
			// Number literals
			if(isDigit(c)) {
				number();
			} else if(isAlpha(c)){
				identifier();
			}
			else {
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
	
	/**
	 * Peeks beyond the next char, 2 spaces over
	 * @return the character beyond the next character
	 */
	private char peekNext() {
		if(current + 1 >= source.length()) return '\0';
		return source.charAt(current + 1);
	}
	
	/**
	 * Go all the way to the end of the word to determine identifier, then create token of corresponding type
	 */
	private void identifier() {
		while(isAlphaNumeric(peek())) advance();
		
		String text = source.substring(start, current);
		TokenType type = keywords.get(text);
		
		if(type == null) type = IDENTIFIER;
		addToken(type);
		
		addToken(IDENTIFIER);
	}
	
	/**
	 * Determines if the given character is alphabetical
	 * @param c the character to examine
	 * @return boolean is the char is in a-z or A-Z
	 */
	private boolean isAlpha(char c) {
		return (c >= 'a' && c <= 'z') ||
				(c >= 'A' && c <= 'Z') ||
				c == '_';
	}
	
	/**
	 * Determines if given character is alphanumeric
	 * @param c the character to examine
	 * @return boolean if the character is in the alphabet or a digit
	 */
	private boolean isAlphaNumeric(char c) {
		return isAlpha(c) || isDigit(c);
	}
}
