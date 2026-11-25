# LumiLang

LumiLang is a small, beginner-friendly programming language with a Python- and JavaScript-inspired style. This repository contains the language grammar and Java interpreter, a Visual Studio Code extension for editing and running `.lumi` files, and sample programs.

> **Project status:** MVP. The current implementation supports variables, expressions, output, conditional statements, and counted repetition. See [Current language features](#current-language-features) for the supported syntax and [Known limits](#known-limits) for what is not implemented yet.

## Contents

- [Project structure](#project-structure)
- [Requirements](#requirements)
- [Build the backend](#build-the-backend)
- [Run a LumiLang program](#run-a-lumilang-program)
- [Use the VS Code extension](#use-the-vs-code-extension)
- [Current language features](#current-language-features)
- [Examples](#examples)
- [Backend JSON interface](#backend-json-interface)
- [Development commands](#development-commands)
- [Known limits](#known-limits)

## Project structure

```text
.
├── backend/
│   ├── pom.xml                         # Maven build and Java dependencies
│   └── src/main/
│       ├── antlr4/lumilang/antlr/
│       │   └── LumiLang.g4              # Language grammar
│       └── java/lumilang/
│           ├── Main.java                # JSON-over-stdin CLI entry point
│           ├── interpreter/             # Interpreter and variable environment
│           ├── errors/                  # Syntax, semantic, and runtime errors
│           └── protocol/                # Execution result and trace records
├── vscode-extension/
│   ├── package.json                     # Extension manifest and commands
│   ├── src/                             # Run command, backend runner, diagnostics
│   ├── syntaxes/                        # LumiLang syntax highlighting grammar
│   └── language-configuration.json      # Comments, brackets, and quotes
├── docs/
│   └── json-contract.md                 # Backend/extension communication format
├── examples/
│   ├── valid/                           # Example program that runs successfully
│   └── invalid/                         # Syntax and semantic error examples
└── README.md
```

## Requirements

- Java Development Kit (JDK) 17 or newer
- Apache Maven
- Node.js and npm (to compile the VS Code extension)
- Visual Studio Code (to use the extension)

Check that Java and Maven are available:

```sh
java -version
mvn -version
```

## Build the backend

From the repository root, package the Java interpreter as an executable JAR:

```sh
cd backend
mvn package
cd ..
```

Maven generates `backend/target/lumilang-backend.jar`. The build also generates the ANTLR lexer, parser, and visitor from `LumiLang.g4`.

## Run a LumiLang program

The backend reads the entire program from standard input and writes one JSON result to standard output. From the repository root, run:

```sh
java -jar backend/target/lumilang-backend.jar < examples/valid/score.lumi
```

The JSON result includes `success`, `output`, `errors`, `trace`, and `variables`. A successful run returns the program's display output and final variable values. If the program has a syntax, semantic, or runtime error, the result includes its type, line, column, and message.

To try the intentionally invalid samples:

```sh
java -jar backend/target/lumilang-backend.jar < examples/invalid/syntax-error.lumi
java -jar backend/target/lumilang-backend.jar < examples/invalid/undefined-variable.lumi
```

## Use the VS Code extension

The extension is designed to run from this repository checkout. Its runner expects the built JAR at `backend/target/lumilang-backend.jar` relative to the repository layout, so build the backend first.

1. Build the backend using the steps above.
2. In a terminal, compile the extension:

   ```sh
   cd vscode-extension
   npm install
   npm run compile
   cd ..
   ```

3. Open the repository in VS Code and open a `.lumi` file, such as `examples/valid/score.lumi`.
4. Run **LumiLang: Run** from the Command Palette or the editor title actions menu.
5. Read the program output and any error messages in the **LumiLang** Output panel. Errors are also marked in the editor as diagnostics.

The extension currently provides `.lumi` language recognition, syntax highlighting, basic editor bracket/comment support, the run command, and editor diagnostics. The VS Code extension's backend process requires Java 17 or newer to be on the system path.

## Current language features

### Statements

| Feature | Syntax | Notes |
| --- | --- | --- |
| Declare a variable | `let name = expression;` | A name can be declared once in the global environment. |
| Update a variable | `set name = expression;` | The variable must already be declared. |
| Display a value | `say(expression);` | Adds a formatted value to program output. |
| Conditional | `if (condition) { ... } else { ... }` | `else` is optional; chained `else if` is supported. |
| Repeat a block | `repeat count { ... }` | `count` must evaluate to a whole number from 0 to 1,000. |

Statements inside blocks still use semicolons where required. `if` and `repeat` blocks do not need a trailing semicolon.

### Values and operators

- **Values:** numbers, strings, `true`, and `false`.
- **Arithmetic:** `+`, `-`, `*`, `/`, and unary `-` (numbers only).
- **Comparison:** `>`, `>=`, `<`, `<=`, `==`, and `!=`.
- **Boolean logic:** `&&`, `||`, and `!` (Boolean values only).
- **Grouping:** parentheses, for example `(score + bonus) >= 80`.
- **Comments:** `//` starts a comment that continues to the end of the line.
- **String escapes:** `\n`, `\r`, `\t`, `\"`, and `\\`.

Arithmetic and comparison operators require numeric values. Conditions and Boolean operators require Boolean values. Comparing values with `==` or `!=` checks both their type and value.

### Example program

```lumi
// Calculate a score and choose a message.
let score = 70;
let bonus = 15;

set score = score + bonus;
say("Final score:");
say(score);

if (score >= 80) {
    say("Excellent!");
} else {
    say("Keep learning!");
}

repeat 3 {
    say("Learning LumiLang");
}
```

This program is also available at [`examples/valid/score.lumi`](examples/valid/score.lumi).

## Backend JSON interface

The Java CLI reads source code from standard input and writes exactly one JSON object to standard output. Technical details from internal errors are written to standard error so the JSON output remains machine-readable. The extension uses this interface to display program output and editor diagnostics.

The result fields are:

- `success`: whether parsing and execution completed without errors.
- `output`: display-ready output lines produced by `say`.
- `errors`: error type, 1-based source line and column, and message.
- `trace`: execution snapshots containing a step number, source line, statement, variables, and output at that point.
- `variables`: final global variable values.

Error types are `SYNTAX`, `SEMANTIC`, `RUNTIME`, and `INTERNAL`. The full contract and an example response are documented in [`docs/json-contract.md`](docs/json-contract.md).

## Development commands

Run these commands from the repository root:

| Goal | Commands |
| --- | --- |
| Package backend | `cd backend && mvn package` |
| Compile extension | `cd vscode-extension && npm install && npm run compile` |
| Watch extension TypeScript | `cd vscode-extension && npm run watch` |
| Run valid sample | `java -jar backend/target/lumilang-backend.jar < examples/valid/score.lumi` |

The extension's `npm run compile` writes compiled JavaScript to `vscode-extension/out/`.

## Known limits

- The interpreter uses one global variable scope; blocks do not create local scopes.
- A program stops after 1,000 executed statements to limit unbounded execution.
- The `repeat` count must be a whole number from 0 through 1,000.
- Functions, arrays, user input, and user-defined types are not part of the current grammar/interpreter.
- The extension records backend execution traces, but an interactive step-by-step/variables viewer is not implemented yet.
