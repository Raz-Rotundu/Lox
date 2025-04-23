package com.craftinginterpreters.lox;
import java.io.IOException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
	static boolean hadError = false;
	
	public static void main(String[] args) throws IOException{
		if(args.length > 1) {
			System.out.println("Usage: jlox[Script]");
			System.exit(64);
		} else if(args.length == 1) {
			runFile(args[0]);
		} else {
			runPrompt();
		}
	}
	
	/**
	 * If a file path is given at command line, read and execute given file
	 * @param path -- the path of the file to read
	 * @throws IOException If file not found or problems reading
	 */
	private static void runFile(String path) throws IOException{
		byte[] bytes = Files.readAllBytes(Paths.get(path));
		run(new String(bytes, Charset.defaultCharset()));
	}
	
	/**
	 * If no path is given, enter an interactive mode which lets you enter and execute one line at a time
	 * @throws IOException
	 */
	private static void runPrompt() throws IOException{
		InputStreamReader input = new InputStreamReader(System.in);
		BufferedReader reader = new BufferedReader(input);
		
		for(;;) {
			System.out.println("> ");
			String line = reader.readLine();
			
			if (line == null) {
				break;
			}
			run(line);
		}
	}
	
	/**
	 * Returns the tokens emitted by a scanner object
	 * @param src the source code string
	 */
	private static void run(String src) {
		
		// Cannot run if error occured
		if (hadError) {
			System.exit(65);
		}
		
		Scanner scanner = new Scanner(src);
		
		List<Token> tokens = scanner.scanTokens();
		

		for(Token t : tokens) {
			System.out.println(t);
		}
	}
	
	/**
	 * A basic error handler to print error information to terminal
	 * @param line -- Line on which the error has occured
	 * @param message -- The error message
	 */
	static void error (int line, String message) {
		report(line, "", message);
	}
	
	/**
	 * Helper to the error function, creates error string and signals error has occured
	 * @param line -- Line on which the error has occured
	 * @param where -- Specific location of error on the line
	 * @param message -- Error message
	 */
	private static void report (int line, String where, String message) {
		System.err.println("[Line " + line + "] error " + where +": "+ message);
		hadError = true;
	}
}
