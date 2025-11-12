# Lox Java Parser
Lox is the complete, object oriented scripting language featured in Robert Nystrom's book [Crafting Interpreters](http://www.craftinginterpreters.com/)

This project represents the Java implementation of Lox, a tree-walk interpreter where the scripts are parsed by recursive descent, and then interpreted by going through each node of the resulting syntax tree.
## Dependencies
Java 16+

Maven 3.13+

A UNIX shell

## Installation
1. Download the repository to the chosen directory
2. Run the following command:
``` mvn clean compile ```
3. Program is now installed, execute with java -jar ...

## The Lox Language

### Overview

Lox is a complete, dynamically, typed programming language. It can be used as you would any other scripting language like JavaScript, and supports Object Oriented programming.
To execute the traditional "Hello World" statement (or any other print statement) write the following code:

`print "Hello World";`

### Data Types

Lox has 4 fundamental data types:
#### **Booleans**

The fundamental True or False operators

```
true;  // Not false.
false; // Not true
```

#### **Numbers**

Lox only supports double precision floating point integers. This is to be able to represent a wide variety of integers, as well as some decimals while maintaining the simplicity of the language.

```
1234;  // An integer.
12.34; // A decimal number.
```

#### **Strings**

Lox supports strings in the same way as most other languages: A sequence of chars enclosed by the " " quotation marks.

```
"I am a string";
"";    // The empty string.
"123"; // This is a string, not a number.
```

#### **Nil**

Data type representing "no value". The Lox equivalent of Java and C's Null

```
c = nil; // This variable has no value
```

### Expressions

Lox supports 4 types of expressions

#### **Arithmetic**
Lox supports binary and unary arithmetic expressions using the classical math operators
	
```
add + me;
subtract - me;
multiply * me;
divide / me;
-negateMe
```
All of these operators work on NUMBERS ONLY, and will throw an error if anything else is passed to them.
The exception to this is the plus (+) operator, which can also be used to concatenate strings

#### **Comparison/Equality**

Lox supports comparison operations using the classic comparison operators

```
less < than;
lessThan <= orEqual;
greater > than;
greaterThan >= orEqual;
```

These operations are for NUMBERS ONLY, and will throw an error if anything else is passed to them.

Lox does not support implicit conversions. As such, any two values can be checked for equality, but if they are of different types, the result will always be "false"

```
1 == 2;         // false.
"cat" != "dog"; // true.
314 == "pi"; // false.
123 == "123"; // false.

```

#### **Logical Operators**
Lox supports 3 logical operators

- **NOT**
The NOT operator is a prefix which flips the boolean value of whatever it is prefixing

```
!true;  // false.
!false; // true.

```

- **AND**
The AND operator determines if two boolean values are both true

```
true and false; // false.
true and true;  // true.

```

- **OR**
The OR operator determines if at least one of two boolean values are true

```
false or false; // false.
true or false;  // true.
```

Both AND and OR operations short circuit;

#### **Precedence and Grouping**
Lox implements the usual C precedence and associativity order for operations (BEDMAS). Brackets can be used to group expressions.

```
var average = (min + max) / 2;
```

Lox DOES NOT support:

- Modulo(%)
- Bitwise shifts
- Conditional operators(&, |, ^,  etc.)

### Statements
Lox is denotes statements and blocks in the same way as Java:

**Statement**: Denoted by (;) at the end

```
print "Hello, world!";
```

**Block**: Denoted by a group of statements between curly braces {}

```
{
	print "First statement";
	print "Second statement";
}
```

### Variables
In Lox, variable are declared using the **var** statement. Variables without initializers have their value set to nil by default.

```
var thisIsaVariable = "Hello";
var thisOneisNil; // nil value

```

Afterwards, variables can be accessed and assigned by name as you would in any other language


```
var foo = "bar";
print foo; // "Bar"
foo = "Baz";
print foo; // "Baz"

```

### Control Flow
Lox implements control flow through three statements

#### if statement
If statements executes one of two blocks based on some  boolean condition

```
if (condition){
	print "Condition is true";
} else {
	print "Condition is false";
```
#### while loop
A while loop continues to execute the same block of code as long as the condition evaluates to true

```
var a = 1;
while(a < 10){
	print a;
	a = a + 1;
}
```

#### for loop
A for loop does the same thing as the while loop, but in a cleaner, easier to read way

```
for (var a = 1; a < 10; a = a + 1) {
  print a;
}
```


Lox DOES NOT implement the following:

- for-in loop
- for-each loop

### Functions 
#### Function Calls
Lox functions are called in the same way you would call functions in C. They can be called with or without passing arguments

```
myFunction();
myOtherFunction(arg1, arg2);
```

#### Function definitions
Lox functions are defined with the **fun** keyword

```
fun sumFunction(a, b){
	print (a + b);
}
```

#### Closure
Lox functions are **First Class Functions**, meaning they can be referenced, stored in values and passed around, just like you would in languages like Python.

```
fun addPair(a, b) {
  return a + b;
}

fun identity(a) {
  return a;
}

print identity(addPair)(1, 2); // Prints "3".
```
Since function declarations are statements, they can be declared inside other functions. Such functions might need to keep references to the outer variables even after the outer function has returned, and Lox supports such functionality, thus supporting closures.

```
fun returnFunction() {
  var outside = "outside";

  fun inner() {
    print outside;
  }

  return inner;
}

var fn = returnFunction();
fn();

```

### Classes
#### Class Overview
Lox supports the basics of object oriented programming. Classes can be defined, containing fields and methods, and inheritance is supported.

In terms of OOP Lox does not support:
- Method and field access modifiers (private, public, etc)
- Abstract classes
- Interfaces

Classes are defined using the **class** keyword. Within the body, fields are defined as normal, and methods are defined without the "fun" keyword.

```
class Breakfast {
  cook() {
    print "Eggs a-fryin'!";
  }

  serve(who) {
    print "Enjoy your breakfast, " + who + ".";
  }
}

```

#### Instantiation/Initialization
Defined Lox classes are instantiated by calling the class like you would a function. In Lox the class acts as a factory function for its own instances.

```
var myObject = myClass();
```
The fields of an instance can be accessed with dot (.) operator. Lox lets you freely add properties on objects by creating that field if it is not found

```
var myObject = myClass(); // has fields foo, bar
myObject.nonExistentField = "hello"; // Allowed
```
Lox supports accessing a instance's fields from within its own methods with the **this** keyword

```
class Breakfast {
  serve(who) {
    print "Enjoy your " + this.meat + " and " +
        this.bread + ", " + who + ".";
  }

  // ...
}
```
You can also define a constructor for your class by naming one of its method **init**. This method will automatically be called whenever a new object is instantiated.

```
class Breakfast {
  init(meat, bread) {
    this.meat = meat;
    this.bread = bread;
  }

  // ...
}
```

#### Inheritance
Lox supports single inheritance through the less than **(<)** operator. 

```
class Brunch < Breakfast {
  drink() {
    print "How about a Bloody Mary?";
  }
}
```
Access Overridden methods with the **super** keyword

```
class Brunch < Breakfast {
  init(meat, bread, drink) {
    super.init(meat, bread);
    this.drink = drink;
  }
}
```

### The Standard Library
There is only one function in the Lox standard library, and that is clock(). This function returns the current time, and was used mostly for benchmarking.

## The Java Interpreter
This section corresponds to a general overview of all the important classes in the Java Interpreter.

### Overview
The Java implementation of lox is a Tree walk Interpreter. Characters are scanned into tokens by scanner, then passes to parser, where they are parsed into their respective Syntax trees via recursive descent. The resolver is used to track which environment a variable was declared in or which environment a function was called in. The interpreter then traverses the created syntax tree, executing each node in turn.


### Lox
Lox.java is the main class in the interpreter and the entry point into the program. It can be run in two modes:
1. **File mode**: If the user provides a text file as command line argument, it will be interpreted as a Lox program
2. **Interactive mode**: If the user provides no command line arguments, the program enters an interactive mode, where the user can write out Lox commands and have them interpreted directly from the command line, similar to Ruby's irb.

Lox.java is responsible for running the scanner, parser, resolver and interpreter in succession, as well as detecting any errors that might have occurred and printing the error text to stderr.

### GenerateAST
This implementation treats each different expression syntax tree as a separate class. However, since these classes are similar in structure, the proccess of creating them has been automated through the generateAST class in the tools package. By taking in  a list of strings of the format "className : Type Arg1, Arg2 ...", as a parameter to the .defineAST function, the listed AST classes are printed with the correct fields, interface, and constructors as static classes under a stated abstract class in the target directory.

```
defineAST(outputDir, "Expr", Arrays.asList(
				"Assign   : Token name, Expr value",
				"Binary   : Expr left, Token operator, Expr right",
				
					// All other expression ASTs 
					
				"Variable : Token name"
				));
```
### Scanner
The scanner iterates through each character of the Lox text, ignoring irrelevant chars like newlines or comments, while detecting relevant lexemes and parsing them into a sequence of tokens. The position of the scanner within the text is tracked by three variables:
1. **start** tracks the position of the first char of the current lexeme
2. **current** tracks the position of the current char of the current lexeme
3. **line** tracks which line of the text is currently being processed	

Lexemes are determined based on a hardcoded set of reserved keywords. When one is identified, the scanner consumes chars until it reaches the end of the lexeme, and then outputs the appropriate token. 

String literals are consumed until the end of line or closing quotation (") is encountered. Number literals are consumed until the end of number or end of line is reached. If a decimal point is encountered, then the scanner "looks over" it to determine if it is a decimal point or and of sentence.

### Parser
The parser iterates through a sequence of tokens outputted by a scanner, and parses then into their respective syntax trees. The Lox language has 26 syntactical grammar rules, and each one corresponds to a method within the parser. 

Upon encountering a token that does not match the rule being evaluated, the parser will attempt to synchronize, resetting its state and the sequence of the upcoming tokens to match the currently evaluated rule. The parser will also emit an error, and print the token which is causing the error.

### Resolver
The resolver's job is to traverse the nodes of a parsed syntax tree, and to track the scoping of each variable expression within. For each variable visited, it will tell the interpreter how many nested scopes lie between the scope in which the variable is referred to, and the scope in which the variable was defined.

The resolver is an implementation of the visitor pattern. In this interpreter (project, not class) each syntax tree is represented by its own class, each of which needs to be resolved differently based on the grammar rule corresponding to that tree. As such, the resolver is mostly a series of private visitSomething methods, where something is a statement or expression class.

### Interpreter
The interpreter's job is to traverse a fully parsed and resolved expression tree, executing the expression at each node. Like Resolver, it is an implementation of the visitor pattern, where each of its methods corresponds to a way to interpret a particular AST class. g
