grammar LumiLang;

program
    : statement* EOF
    ;

statement
    : variableDeclaration SEMI                    # variableDeclarationStatement
    | assignment SEMI                             # assignmentStatement
    | sayStatement SEMI                           # sayStatementOnly
    | ifStatement                                 # ifStatementOnly
    | repeatStatement                             # repeatStatementOnly
    ;

variableDeclaration
    : LET IDENTIFIER ASSIGN expression
    ;

assignment
    : SET IDENTIFIER ASSIGN expression
    ;

sayStatement
    : SAY LPAREN expression RPAREN
    ;

ifStatement
    : IF LPAREN expression RPAREN block (ELSE (block | ifStatement))?
    ;

repeatStatement
    : REPEAT expression block
    ;

block
    : LBRACE statement* RBRACE
    ;

expression
    : logicalOr
    ;

logicalOr
    : logicalAnd (OR logicalAnd)*
    ;

logicalAnd
    : equality (AND equality)*
    ;

equality
    : comparison ((EQUAL | NOT_EQUAL) comparison)*
    ;

comparison
    : additive ((GREATER | GREATER_EQUAL | LESS | LESS_EQUAL) additive)*
    ;

additive
    : multiplicative ((PLUS | MINUS) multiplicative)*
    ;

multiplicative
    : unary ((STAR | SLASH) unary)*
    ;

unary
    : (NOT | MINUS) unary
    | primary
    ;

primary
    : NUMBER
    | STRING
    | TRUE
    | FALSE
    | IDENTIFIER
    | LPAREN expression RPAREN
    ;

LET          : 'let';
SET          : 'set';
SAY          : 'say';
IF           : 'if';
ELSE         : 'else';
REPEAT       : 'repeat';
TRUE         : 'true';
FALSE        : 'false';

ASSIGN       : '=';
EQUAL        : '==';
NOT_EQUAL    : '!=';
GREATER_EQUAL: '>=';
LESS_EQUAL   : '<=';
GREATER      : '>';
LESS         : '<';
AND          : '&&';
OR           : '||';
NOT          : '!';
PLUS         : '+';
MINUS        : '-';
STAR         : '*';
SLASH        : '/';
LPAREN       : '(';
RPAREN       : ')';
LBRACE       : '{';
RBRACE       : '}';
SEMI         : ';';

NUMBER       : [0-9]+ ('.' [0-9]+)?;
STRING       : '"' ('\\' . | ~["\\\r\n])* '"';
IDENTIFIER   : [a-zA-Z_] [a-zA-Z_0-9]*;
LINE_COMMENT : '//' ~[\r\n]* -> skip;
WS           : [ \t\r\n]+ -> skip;
