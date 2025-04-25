package com.craftinginterpreters.lox;

import java.util.List;

import static com.craftinginterpreters.lox.TokenType.*;

/**
 * Parser -- A recursive descent parser which converts a flat sequence of Token objects into their corresponding expressions 
 */
public class Parser {

	private final List<Token> tokens;
	private int current = 0;
	
	Parser(List<Token> tokens){
		this.tokens = tokens;
	}
	
	// Methods corresponding to Lox grammar rules
	/*
	 * expression     → equality ;
	 * equality       → comparison ( ( "!=" | "==" ) comparison )* ;
     * comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
     * term           → factor ( ( "-" | "+" ) factor )* ;
     * factor         → unary ( ( "/" | "*" ) unary )* ;
     * unary          → ( "!" | "-" ) unary | primary ;
     * primary        → NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" ;
	 */
	

	/**
	 * Method corresponding to expression rule
	 * expression --> equality ;
	 * @return A reference to an equality
	 */
	private Expr expression() {
		return equality();
	}
	

	/**
	 * Method corresponding to the equality rule
	 * equality --> comparison ( ( "!=" | "==" ) comparison )* ;
	 * On encountering a '=' or '!=' loops over any further instances to create right-assiciative expression
	 * @return A reference to a comparison
	 */
	private Expr equality() {
		Expr expr = comparison();
		
		while(match(BANG_EQUAL, EQUAL_EQUAL)) {
			Token operator = previous();
			Expr right = comparison();
			expr = new Expr.Binary(expr, operator, right);
			}
		
		return expr;
	}
	

	/**
	 * Method corresponding to comparison rule
	 * comparison ->  term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
	 * On encountering a comparison operator, creates new term expression of two terms and comparison operator
	 * @return A refernce to a term
	 */
	private Expr comparison() {
		Expr expr = term();
		while(match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
			Token operator = previous();
			Expr right = term();
			expr = new Expr.Binary(expr, operator, right);
		}
		
		return expr;
	}
	
	
	/**
	 * Method corresponding to the term rule
	 * term --> factor ( ( "-" | "+" ) factor )* ;
	 * On encountering a plus or minus, create new factor expression out of previous, current term plus operator
	 * @return a reference to a factor
	 */
	private Expr term() {
		Expr expr = factor();
		
		while(match(PLUS, MINUS)) {
			Token operator = previous();
			Expr right = factor();
			expr = new Expr.Binary(expr, operator, right);
		}
		
		return expr;
	}
	

	/**
	 * Method corresponding to factor (multiplication and division) rule
	 * factor --> unary ( ( "/" | "*" ) unary )* ;
	 * On encountering a '*' or '/' token, construct new unary expression out of the left, right, expressions plus operator
	 * @return a reference to a unary expression
	 */
	private Expr factor() {
		Expr expr = unary();
		
		while(match(STAR, SLASH)) {
			Token operator = previous();
			Expr right = unary();
			expr = new Expr.Binary(expr, operator, right);
		}
		
		return expr;
	}
	
	
	/**
	 * Method representing Unary operations rule
	 * unary --> ( "!" | "-" ) unary | primary ;
	 * On encountering unary operator, construct unary expression from operator and previous expression
	 * @return a primary expression reference
	 */
	private Expr unary() {
		if(match(BANG, MINUS)) {
			Token operator = previous();
			Expr right = unary();
			return new Expr.Unary(operator, right);
		}
		
		return primary();
	}
	
	
	/**
	 * Method representing primary operations rule
	 * primary --> NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" ;
	 * Check to see which literal it is, return literal expression, or if there is a bracket, a grouping expression
	 * @return a new literal or grouping expression
	 */
	private Expr primary() {
		if(match(FALSE)) return new Expr.Literal(false);
		if(match(TRUE)) return new Expr.Literal(true);
		if(match(NIL)) return new Expr.Literal(null);
		
		if(match(NUMBER, STRING)) {
			return new Expr.Literal(previous().literal);
		}
		
		if(match(LEFT_PAREN)) {
			Expr expr = expression();
			consume(RIGHT_PAREN, "Expect ')' after expression.");
			return new Expr.Grouping(expr);
		}

		
		
	}
	
	
	// Helper function to match a token type, consumes if match, leaves alone if no match
	private boolean match(TokenType... types) {
		for(TokenType type : types) {
			if(check(type)) {
				advance();
				return true;
			}
		}
		
		return false;
	}
	
	// Helper Returns true if token is of given type, but does not consume it
	private boolean check(TokenType type) {
		if(isAtEnd()) return false;
		return peek().type == type;
	}
	
	//  Helper Consumes current token and returns it
	private Token advance() {
		if(!isAtEnd()) current++;
		return previous();
	}
	
	// Helper Returns true if there are no tokens left to parse
	private boolean isAtEnd() {
		return peek().type == EOF;
		
	}
	
	// Helper Returns the current, unconsumed token
	private Token peek() {
		return tokens.get(current);

	}
	
	// Helper Returns most recently consumed token
	private Token previous() {
		return tokens.get(current - 1);
	}

}
