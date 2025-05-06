package com.craftinginterpreters.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * GenerateAST -- Automated script which prints out class definitions representing the differernt ASTs
 * Takes name and fields and uses them to define a class with that name and state
 */
public class GenerateAST {
	
	public static void main(String[] args ) throws IOException{
		if(args.length != 1) {
			System.err.println("Usage: generate_ast <output directory>");
			System.exit(64);
		}
		String outputDir = args[0];
		defineAST(outputDir, "Expr", Arrays.asList(
				"Assign   : Token name, Expr value",
				"Binary   : Expr left, Token operator, Expr right",
				"Call     : Expr callee, Token paren, List<Expr> arguments",
				"Get      : Expr object, Token name",
				"Grouping : Expr expression",
				"Literal  : Object Value",
				"Logical  : Expr left, Token operator, Expr right",
				"Set      : Expr object, Token name, Expr value",
				"This     : Token keyword",
				"Unary    : Token operator, Expr right",
				"Variable : Token name"
				));
		
		defineAST(outputDir, "Stmt", Arrays.asList(
				"Block      : List<Stmt> statements",
				"Class      : Token name, List<Stmt.Function> methods",
				"Expression : Expr expression",
				"Function   : Token name, List<Token> params," + " List<Stmt> body",
				"If         : Expr condition, Stmt thenBranch," + " Stmt elseBranch",
				"Print      : Expr expression",
				"Return     : Token keyword, Expr value",
				"Var        : Token name, Expr initializer",
				"While      : Expr condition, Stmt body"
				));
	}
	
	/**
	 * Defines all classes representing the different ASTs possible in Lox
	 * @param outputDir - The name of the directory to which the java files will be ouput
	 * @param baseName - The name of the abstract class which all the expressions will be extending
	 * @param types -  A list of all the class names and arguments Eg: "Literal : Object Value"
	 * @throws IOException
	 */
	public static void defineAST(String outputDir, String baseName, List<String> types) throws IOException{
		
		String path = outputDir + "/" + baseName + ".java";
		
		PrintWriter writer = new PrintWriter(path, "UTF-8");
		
		// Printing the abstract class definition
		writer.println("package com.craftinginterpreters.lox;");
		writer.println();
		writer.println("import java.util.List;");
		writer.println();
		writer.println("abstract class " + baseName + " {");
		
		defineVisitor(writer, baseName, types);
		
		// Printing out all the ast classes
		for (String type : types) {
			String className = type.split(":")[0].trim();
			String fields = type.split(":")[1].trim();
			
			defineType(writer, baseName, className, fields);
		}
		
		// Base class accept() method
		writer.println();
		writer.println("  abstract <R> R accept(Visitor<R> visitor);");
		
		writer.println("}");
		writer.close();
		
		

	}
	
	/**
	 * Helper to defineAST -- Defines an individual type representing an abstract search tree
	 * @param writer -- The printWriter used to write to file
	 * @param baseName -- Abstract class name which will be inherited
	 * @param className -- The name of the static class
	 * @param fieldList --  A string representing the fields of the class to be created Eg: "Expr left, Token operator, Expr right"
	 */
	private static void defineType(PrintWriter writer, String baseName, String className, String fieldList) {
		
		// Individual class definition
		writer.println(" static class " + className + " extends " + baseName + " {");
		
		// Static class constructor
		writer.println("    " + className + "(" + fieldList + ") {");
		
		// Constructor field assignment
		String[] fields = fieldList.split(", ");
		for(String field : fields) {
			String name = field.split(" ")[1];
			writer.println("      this." + name + " = " + name + ";");
		}
		writer.println("    }");
		
		// Visitor pattern
		writer.println();
		writer.println("    @Override");
		writer.println("    <R> R accept(Visitor<R> visitor) {");
		writer.println("      return visitor.visit" + className + baseName + "(this);");
		writer.println("    }");

		
		// Declaring the fields within the class
		writer.println();
		for (String field : fields) {
			writer.println("    final " + field + ";");
		}
		writer.println("  }");

	}
	
	/**
	 * Iterate through all subclasses and declare a visit method for each one
	 * @param writer
	 * @param baseName
	 * @param types
	 */
	private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
		writer.println("  interface Visitor<R> {");
		
		for(String type : types) {
			String typeName = type.split(":")[0].trim();
			writer.println("    R visit" + typeName + baseName + "(" + typeName + " " + baseName.toLowerCase() + ");");		
		}
		writer.println(" }");
	}
}
