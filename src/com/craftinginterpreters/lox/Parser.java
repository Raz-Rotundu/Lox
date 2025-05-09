package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import static com.craftinginterpreters.lox.TokenType.*;

/**
 * Parser -- A recursive descent parser which converts a flat sequence of Token objects into their corresponding expressions 
 */
public class Parser {

	private static class ParseError extends RuntimeException{}
	
	private final List<Token> tokens;
	private int current = 0;
	
	Parser(List<Token> tokens){
		this.tokens = tokens;
	}
	
	/**
	 * Main method of parser, takes tokens and returns abstract syntax trees
	 * @return A list of statements
	 */
	List<Stmt> parse(){
		List<Stmt> statements = new ArrayList<>();
		while(!isAtEnd()) {
			statements.add(declaration());
		}
		return statements;
	}
	

	

	

	// LOX GRAMMAR RULES
	/*
	 * program        → declaration* EOF ;
	 * declaration    → varDecl | statement | funDecl | classDecl;
	 * classDecl      → "class" IDENTIFIER ( "<" IDENTIFIER )? "{" function* "}" ;
	 * varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;
	 * statement      → exprStmt | printStmt | block | ifStmt | whileStmt | forStmt | returnStmt;
	 * funDecl        → "fun" function ;
	 * function       → IDENTIFIER "(" parameters? ")" block ;
	 * parameters     → IDENTIFIER ( "," IDENTIFIER )* ;
	 * returnStmt     → "return" expression? ";" ;
	 * forStmt        → "for" "(" ( varDecl | exprStmt | ";" ) expression? ";" expression? ")" statement ;
	 * whileStmt      → "while" "(" expression ")" statement ;
	 * ifStmt         → "if" "(" expression ")" statement ( "else" statement )? ;
	 * block          → "{" declaration* "}" ;
	 * expression     → assignment ;
	 * assignment     → ( call "." )? IDENTIFIER "=" assignment | logic_or ;
	 * equality       → comparison ( ( "!=" | "==" ) comparison )* ;
	 * logic_or       → logic_and ( "or" logic_and )* ;
	 * logic_and      → equality ( "and" equality )* ;
     * comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
     * term           → factor ( ( "-" | "+" ) factor )* ;
     * factor         → unary ( ( "/" | "*" ) unary )* ;
     * unary          → ( "!" | "-" ) unary | call ;
     * call           → primary ( "(" arguments? ")" | "." IDENTIFIER )* ;
     * arguments      → expression ( "," expression )* ;
     * primary        → "true" | "false" | "nil" | "this" | NUMBER | STRING | IDENTIFIER | "(" expression ")"| "super" "." IDENTIFIER ;
	 */
	
	// Methods corresponding to Lox grammar rules
	
	/**
	 * Method representing declarations rule
	 * declaration -> varDecl | statement | funDecl | classDecl;
	 * @return Parsed declaration
	 */
	private Stmt declaration() {
		try {
			if(match(CLASS)) return classDeclaration();
			if(match(FUN)) return function("function");
			if(match(VAR)) return varDeclaration();
			
			return statement();
		} catch(ParseError error) {
			synchronize();
			return null;
		}
	}
	
	/**
	 * Function corresponding to the classDecl rule
	 * classDecl --> "class" IDENTIFIER ( "<" IDENTIFIER )? "{" function* "}" ;
	 * After checking for name and left brace, go through body and parse method declarations until the end
	 * @return A parsed class declaration statement
	 */
	private Stmt classDeclaration() {
		Token name = consume(IDENTIFIER, "Expect class name.");
		
		Expr.Variable superclass = null;
		if(match(LESS)) {
			consume(IDENTIFIER, "Expect superclass name.");
			superclass = new Expr.Variable(previous());
		}
		
		consume(LEFT_BRACE, "Expect '{' before class body.");
		
		
		List<Stmt.Function> methods = new ArrayList<>();
		while(!check(RIGHT_BRACE) && !isAtEnd()) {
			methods.add(function("method"));
		}
		
		consume(RIGHT_BRACE, "Expect '}' after class body.");
		return new Stmt.Class(name, superclass, methods);
	}
	/**
	 * Function corresponding to varDecl rule
	 * varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;
	 * Consumes an identifier token, then based on the next token, 
	 * determines if this is a variable declaration or a variable expression.
	 * Declarations means the initializer expression will be parsed
	 * @return a parsed variable declaration statement
	 */
	private Stmt varDeclaration() {
		Token name = consume(IDENTIFIER, "Expect variable name.");
		
		Expr initializer = null;
		
		if(match(EQUAL)) {
			initializer = expression();
		}
		
		consume(SEMICOLON, "Expect ';' after variable declaration.");
		return new Stmt.Var(name, initializer);
	}
	
	/**
	 * Method to parse a single expression statement, corresponding to statement rule
	 * statement --> exprStmt | printStmt | block | ifStmt | whileStmt | forStmt | returnStmt;
	 * @return a parsed expression statement
	 */
	private Stmt statement() {
		if(match(FOR)) return forStatement();
		
		if(match(PRINT)) return printStatement();
		
		if(match(RETURN)) return returnStatement();
		
		if(match(WHILE)) return whileStatement();
		
		if(match(LEFT_BRACE)) return new Stmt.Block(block());
		
		if(match(IF)) return ifStatement();
		
		return expressionStatement();
	}

	/**
	 * Method corresponding to function grammar rule
	 * function       → IDENTIFIER "(" parameters? ")" block ;
	 * Throws error if function has more than 255 parameters
	 * @param kind the kind of method declaration being parsed (function or class)
	 * @return A parsed function reference
	 */
	private Stmt.Function function(String kind) {
		Token name = consume(IDENTIFIER, "Expect " + kind + " name.");
		consume(LEFT_PAREN, "Expect '(' after " + kind + " name.");
		List<Token> parameters = new ArrayList<>();
		
		if(!check(RIGHT_PAREN)) {
			do {
				if (parameters.size() >= 255) {
					error(peek(), "Can't have more than 255 parameters.");
				}
				
				parameters.add(consume(IDENTIFIER, "Expect parameter name"));
				
			} while(match(COMMA));			
		}
		consume(RIGHT_PAREN, "Expect ')' after parameters.");
		
		consume(LEFT_BRACE, "Expect '{' before " + kind + " body.");
		List<Stmt> body = block();
		return new Stmt.Function(name, parameters, body);
	}
	
	/**
	 * Method corresponding to the forStmt rule
	 * forStmt --> "for" "(" ( varDecl | exprStmt | ";" ) expression? ";" expression? ")" statement ;
	 * The for statement is "de-sugared" into a while loop, and then parsed as such
	 * @return A parsed while loopS
	 */
	private Stmt forStatement() {
		consume(LEFT_PAREN, "Expect '(' after 'for'.");
		
		// Initializer
		// If the token after '(' is semicolon, there is no initializer
		//Otherwise it is either a variable or expression
		Stmt initializer;
		if(match(SEMICOLON)) {
			initializer = null;
			
		} else if(match(VAR)) {
			initializer = varDeclaration();
			
		} else {
			initializer = expressionStatement();
		}
		
		// Condition
		// Check for semicolon to see if clause has been omitted
		Expr condition = null;
		if(!check(SEMICOLON)) {
			condition = expression();	
		}
		consume(SEMICOLON, "Expect ';' after loop condition.");
		
		
		// Increment
		Expr increment = null;
		if(!check(RIGHT_PAREN)) {
			increment = expression();
		}
		consume(RIGHT_PAREN, "Expect ')' after for clauses.");
		
		
		Stmt body = statement();
		
		// Desugaring to while loop
		if(increment != null) {
			body = new Stmt.Block(
					Arrays.asList(
							body,
							new Stmt.Expression(increment)));		
		}
		
		if(condition == null) condition = new Expr.Literal(true);
		body = new Stmt.While(condition, body);
		
		if(initializer != null) {
			body = new Stmt.Block(Arrays.asList(initializer, body));
		}
		
		return body;
	}
	
	/**
	 * Method corresponding to whileStmt rule
	 * * whileStmt --> "while" "(" expression ")" statement ;
	 * @return
	 */
	private Stmt whileStatement() {
		consume(LEFT_PAREN, "Expect '(' after 'while'.");
		Expr condition = expression();
		consume(RIGHT_PAREN, "Expect ')' after condition.");
		
		Stmt body = statement();
		return new Stmt.While(condition, body);
	}
	
	
	/**
	 * Method corresponding to returnStmt grammar rule
	 * returnStmt     → "return" expression? ";" ;
	 * @return A parsed return expression
	 */
	private Stmt returnStatement() {
		Token keyword = previous();
		Expr value = null;
		
		if(!check(SEMICOLON)) {
			value = expression();
			
		}
		
		consume(SEMICOLON, "Expect ';' after return value.");
		return new Stmt.Return(keyword, value);
	}
	
	/**
	 * Method representing ifStmt rule
	 * ifStmt --> "if" "(" expression ")" statement ( "else" statement )? ;
	 * @return A parsed reference to an if statement
	 */
	private Stmt ifStatement() {
		consume(LEFT_PAREN, "Expect '(' after 'if'.");
		Expr condition = expression();
		consume(RIGHT_PAREN, "Expect ')' after if condition.");
		
		Stmt thenBranch = statement();
		Stmt elseBranch = null;
		
		if(match(ELSE)) {
			elseBranch = statement();
		}
		
		return new Stmt.If(condition, thenBranch, elseBranch);
	}
	
	/**
	 * Method to parse blocks according to block rule
	 * block  --> "{" declaration* "}" ;
	 * @return A parsed code block
	 */
	private List<Stmt> block(){
		List<Stmt> statements = new ArrayList<>();
		
		while(!check(RIGHT_BRACE) && !isAtEnd()) {
			statements.add(declaration());
		}
		
		consume(RIGHT_BRACE, "Expect '}' after block.");
		return statements;
	}
	/**
	 * Method corresponding to expression rule
	 * expression --> equality ;
	 * @return A reference to an assignment
	 */
	private Expr expression() {
		return assignment();
	}
	
	
	/**
	 * Method corresponding to the assignment rule
	 * assignment --> ( call "." )? IDENTIFIER "=" assignment | logic_or ;
	 * Before an assignment expression node is created, check left hand side to see what kind of assignment target it is
	 * Convert r-value expression node into l-value representation
	 * @return Parsed equality reference
	 */
	private Expr assignment() {
		Expr expr = or();
		
		if(match(EQUAL)) {
			Token equals = previous();
			Expr value = assignment();
			
			if(expr instanceof Expr.Variable) {
				Token name = ((Expr.Variable)expr).name;
				return new Expr.Assign(name, value);
			} else if(expr instanceof Expr.Get) {
				Expr.Get get = (Expr.Get) expr;
				return new Expr.Set(get.object, get.name, value);
			}
			
			error(equals, "Invalid assignment target");
		}
		
		return expr;
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
	 * Method corresponding to logic_or rule
	 * logic_or --> logic_and ( "or" logic_and )*;
	 * @return and expression reference or parsed OR expression syntax tree
	 */
	private Expr or() {
		Expr expr = and();
		
		while(match(OR)) {
			Token operator = previous();
			Expr right = and();
			
			expr = new Expr.Logical(expr, operator, right);
		}
		
		return expr;
	}
	
	/** Method corresponding to logic_and rule
	 * logic_and --> equality ( "and" equality )* ;
	 * @return equality expression reference or parsed AND expression syntax tree
	 */
	private Expr and() {
		Expr expr = equality();
		
		while(match(AND)) {
			Token operator = previous();
			Expr right = equality();
			expr = new Expr.Logical(expr, operator, right);
		}
		
		return expr;
	}
	
	/**
	 * Method corresponding to comparison rule
	 * comparison ->  term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
	 * On encountering a comparison operator, creates new term expression of two terms and comparison operator
	 * @return term reference or parsed comparison syntax tree
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
	 * unary --> ( "!" | "-" ) unary | call ;
	 * On encountering unary operator, construct unary expression from operator and previous expression
	 * @return a primary expression reference
	 */
	private Expr unary() {
		if(match(BANG, MINUS)) {
			Token operator = previous();
			Expr right = unary();
			return new Expr.Unary(operator, right);
		}
		
		return call();
	}
	
	/**
	 * Method representing call grammar rule
	 *  call --> primary ( "(" arguments? ")" | "." IDENTIFIER )* ;
	 *  Go through tokens, building up a chain of calls until parentheses and dots are encountered
	 *  Anytime an left parentheses is encountered, finish an expression call
	 * @return A called expression or primary expression reference
	 */
	private Expr call() {
		Expr expr = primary();
		
		while(true) {
			if(match(LEFT_PAREN)) {
				expr = finishCall(expr);
			} else if(match(DOT)) {
				Token name = consume(IDENTIFIER,"Expect property name after '.'.");
				expr = new Expr.Get(expr, name);
			} else {
				break;
			}
		}
		
		return expr;
	}
	
	/**
	 * Method representing arguments grammar rule
	 * arguments --> expression ( "," expression )* ;
	 * @param callee The expression which is being called
	 * @return an parsed callee with all of its arguments syntax tree
	 */
	private Expr finishCall(Expr callee) {
		List<Expr> arguments = new ArrayList<>();
		if(!check(RIGHT_PAREN)) {
			do {
				// Argument limit in case I decide to do the bytecode compiler too
				if(arguments.size() >= 255) {
					error(peek(), "Can't have more than 255 arguments");
				}
				arguments.add(expression());
			} while (match(COMMA));
		}
		
		Token paren = consume(RIGHT_PAREN,
				"Expect ')' after arguments.");
		
		return new Expr.Call(callee, paren, arguments);
	}
	
	/**
	 * Method representing primary operations rule
	 * primary --> "true" | "false" | "nil" | "this" | NUMBER | STRING | IDENTIFIER | "(" expression ")"| "super" "." IDENTIFIER ;
	 * Check to see which literal it is, return literal expression, or if there is a bracket, a grouping expression
	 * @return a new literal, super, variable, or grouping expression
	 */
	private Expr primary() {
		if(match(FALSE)) return new Expr.Literal(false);
		if(match(TRUE)) return new Expr.Literal(true);
		if(match(NIL)) return new Expr.Literal(null);
		
		if(match(NUMBER, STRING)) {
			return new Expr.Literal(previous().literal);
		}
		
		if(match(SUPER)) {
			Token keyword = previous();
			consume(DOT, "Expect '.' after 'super'.");
			Token method = consume(IDENTIFIER, "Expect superclass method name.");
			return new Expr.Super(keyword, method);
		}
		
		if(match(LEFT_PAREN)) {
			Expr expr = expression();
			consume(RIGHT_PAREN, "Expect ')' after expression.");
			return new Expr.Grouping(expr);
		}
		
		if(match(THIS)) return new Expr.This(previous());
		
		if(match(IDENTIFIER)) {
			return new Expr.Variable(previous());
		}
		
		throw error(peek(), "Expect expression.");
		
	}
	

	
	/**
	 * Helper function to match a token type.
	 * Consumes the token if it matches, leaves it alone if not
	 * @param types The types to match the token against
	 * @return boolean
	 */
	private boolean match(TokenType... types) {
		for(TokenType type : types) {
			if(check(type)) {
				advance();
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Method to look check if the next token is the expected. If not, throw an error
	 * @param type
	 * @param message
	 * @return
	 */
	private Token consume(TokenType type, String message) {
		if(check(type)) return advance();
		
		throw error(peek(), message);
	}
	
	/**
	 * Helper function that returns true if token is of given type
	 * @param type the type of token to match
	 * @return boolean
	 */
	private boolean check(TokenType type) {
		if(isAtEnd()) return false;
		return peek().type == type;
	}
	
	/**
	 * Helper method consumes token and returns it
	 * @return the token consumed
	 */
	private Token advance() {
		if(!isAtEnd()) current++;
		return previous();
	}
	
	
	/**
	 * Helper returns true if there are no tokens left to parse
	 * @return
	 */
	private boolean isAtEnd() {
		return peek().type == EOF;
		
	}
	
	/**
	 * Helper returns the current, unconsumed token
	 * @return current unconsumed token
	 */
	private Token peek() {
		return tokens.get(current);

	}
	
	/**
	 * Helper returns most recently consumed token
	 * @return the most recently consumed token
	 */
	private Token previous() {
		return tokens.get(current - 1);
	}
	

	/**
	 * Reports the error to the user by printing to terminal
	 * @param token the offending token
	 * @param message the error message
	 * @return a ParseError
	 */
	private ParseError error(Token token, String message) {
		Lox.error(token, message);
		return new ParseError();
	}
	
	

	/**
	 * Resets the state and stream of the parser to be on the next available token
	 * Next available token is the first token matching the rule currently being evaluated
	 */
	private void synchronize() {
		advance();
		
		while(!isAtEnd()) {
			if(previous().type == SEMICOLON) return;
			
			switch(peek().type) {
				case CLASS:
				case FUN:
				case VAR:
				case FOR:
				case IF:
				case WHILE:
				case PRINT:
				case RETURN:
					return;
			
			}
			advance();
		}
	}
	
	/**
	 * Parsing a print statement
	 * @return Syntax tree of the print statement
	 */
	private Stmt printStatement() {
		Expr value = expression();
		consume(SEMICOLON, "Expect ';' after value.");
		return new Stmt.Print(value);
	}
	
	
	/**
	 * Parse non-print expression, consume semicolon and return syntax tree
	 * @return
	 */
	private Stmt expressionStatement() {
		Expr expr = expression();
		consume(SEMICOLON, "Expect ';' after value.");
		return new Stmt.Expression(expr);
	}
	
	
}
