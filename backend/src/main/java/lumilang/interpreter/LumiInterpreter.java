package lumilang.interpreter;

import lumilang.antlr.LumiLangBaseVisitor;
import lumilang.antlr.LumiLangParser;
import lumilang.errors.LumiException;
import lumilang.protocol.ExecutionStep;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Small visitor-based interpreter for the agreed LumiLang MVP syntax. */
public final class LumiInterpreter extends LumiLangBaseVisitor<Object> {
    private static final int MAX_EXECUTED_STATEMENTS = 1_000;

    private final Environment environment = new Environment();
    private final List<String> output = new ArrayList<>();
    private final List<ExecutionStep> trace = new ArrayList<>();
    private final String source;
    private int executedStatements;

    public LumiInterpreter(String source) {
        this.source = source;
    }

    public List<String> getOutput() { return List.copyOf(output); }
    public List<ExecutionStep> getTrace() { return List.copyOf(trace); }
    public Map<String, Object> getVariables() { return environment.snapshot(); }

    @Override
    public Object visitProgram(LumiLangParser.ProgramContext ctx) {
        for (LumiLangParser.StatementContext statement : ctx.statement()) {
            visit(statement);
        }
        return null;
    }

    @Override
    public Object visitVariableDeclarationStatement(LumiLangParser.VariableDeclarationStatementContext ctx) {
        int step = beginStatement(ctx);
        var declaration = ctx.variableDeclaration();
        Object value = visit(declaration.expression());
        environment.declare(declaration.IDENTIFIER().getText(), value, declaration.IDENTIFIER().getSymbol());
        finishStatement(ctx, step);
        return null;
    }

    @Override
    public Object visitAssignmentStatement(LumiLangParser.AssignmentStatementContext ctx) {
        int step = beginStatement(ctx);
        var assignment = ctx.assignment();
        Object value = visit(assignment.expression());
        environment.assign(assignment.IDENTIFIER().getText(), value, assignment.IDENTIFIER().getSymbol());
        finishStatement(ctx, step);
        return null;
    }

    @Override
    public Object visitSayStatementOnly(LumiLangParser.SayStatementOnlyContext ctx) {
        int step = beginStatement(ctx);
        output.add(format(visit(ctx.sayStatement().expression())));
        finishStatement(ctx, step);
        return null;
    }

    @Override
    public Object visitIfStatementOnly(LumiLangParser.IfStatementOnlyContext ctx) {
        int step = beginStatement(ctx);
        // Record the branch decision before visiting its body so trace order follows execution.
        finishStatement(ctx, step);
        visit(ctx.ifStatement());
        return null;
    }

    @Override
    public Object visitRepeatStatementOnly(LumiLangParser.RepeatStatementOnlyContext ctx) {
        int step = beginStatement(ctx);
        // The repeat entry is a step of its own; each executed body statement follows it.
        finishStatement(ctx, step);
        visit(ctx.repeatStatement());
        return null;
    }

    @Override
    public Object visitIfStatement(LumiLangParser.IfStatementContext ctx) {
        boolean condition = requireBoolean(visit(ctx.expression()), ctx.expression().getStart(),
                "if condition");
        if (condition) {
            visit(ctx.block(0));
        } else if (ctx.ELSE() != null) {
            if (ctx.block().size() > 1) {
                visit(ctx.block(1));
            } else if (ctx.ifStatement() != null) {
                visit(ctx.ifStatement());
            }
        }
        return null;
    }

    @Override
    public Object visitRepeatStatement(LumiLangParser.RepeatStatementContext ctx) {
        Object value = visit(ctx.expression());
        if (!(value instanceof Double number) || number < 0 || number != Math.floor(number)
                || number > MAX_EXECUTED_STATEMENTS) {
            throw error("RUNTIME", ctx.expression().getStart(),
                    "repeat requires a whole number between 0 and 1,000.");
        }
        int count = number.intValue();
        for (int i = 0; i < count; i++) {
            visit(ctx.block());
        }
        return null;
    }

    @Override
    public Object visitBlock(LumiLangParser.BlockContext ctx) {
        for (LumiLangParser.StatementContext statement : ctx.statement()) {
            visit(statement);
        }
        return null;
    }

    @Override
    public Object visitExpression(LumiLangParser.ExpressionContext ctx) {
        return visit(ctx.logicalOr());
    }

    @Override
    public Object visitLogicalOr(LumiLangParser.LogicalOrContext ctx) {
        Object value = visit(ctx.logicalAnd(0));
        for (int i = 1; i < ctx.logicalAnd().size(); i++) {
            boolean left = requireBoolean(value, ctx.getStart(), "'||' operands");
            value = left || requireBoolean(visit(ctx.logicalAnd(i)), ctx.logicalAnd(i).getStart(), "'||' operands");
        }
        return value;
    }

    @Override
    public Object visitLogicalAnd(LumiLangParser.LogicalAndContext ctx) {
        Object value = visit(ctx.equality(0));
        for (int i = 1; i < ctx.equality().size(); i++) {
            boolean left = requireBoolean(value, ctx.getStart(), "'&&' operands");
            value = left && requireBoolean(visit(ctx.equality(i)), ctx.equality(i).getStart(), "'&&' operands");
        }
        return value;
    }

    @Override
    public Object visitEquality(LumiLangParser.EqualityContext ctx) {
        Object value = visit(ctx.comparison(0));
        for (int i = 1; i < ctx.comparison().size(); i++) {
            Object right = visit(ctx.comparison(i));
            String operator = ctx.getChild(2 * i - 1).getText();
            boolean equal = value != null && value.getClass() == right.getClass() && value.equals(right);
            value = operator.equals("==") ? equal : !equal;
        }
        return value;
    }

    @Override
    public Object visitComparison(LumiLangParser.ComparisonContext ctx) {
        Object value = visit(ctx.additive(0));
        for (int i = 1; i < ctx.additive().size(); i++) {
            Object right = visit(ctx.additive(i));
            double leftNumber = requireNumber(value, ctx.getStart(), "comparison");
            double rightNumber = requireNumber(right, ctx.additive(i).getStart(), "comparison");
            value = switch (ctx.getChild(2 * i - 1).getText()) {
                case ">" -> leftNumber > rightNumber;
                case ">=" -> leftNumber >= rightNumber;
                case "<" -> leftNumber < rightNumber;
                case "<=" -> leftNumber <= rightNumber;
                default -> throw new IllegalStateException("Unknown comparison operator");
            };
        }
        return value;
    }

    @Override
    public Object visitAdditive(LumiLangParser.AdditiveContext ctx) {
        Object value = visit(ctx.multiplicative(0));
        for (int i = 1; i < ctx.multiplicative().size(); i++) {
            Object right = visit(ctx.multiplicative(i));
            double leftNumber = requireNumber(value, ctx.getStart(), "arithmetic");
            double rightNumber = requireNumber(right, ctx.multiplicative(i).getStart(), "arithmetic");
            value = ctx.getChild(2 * i - 1).getText().equals("+")
                    ? leftNumber + rightNumber : leftNumber - rightNumber;
        }
        return value;
    }

    @Override
    public Object visitMultiplicative(LumiLangParser.MultiplicativeContext ctx) {
        Object value = visit(ctx.unary(0));
        for (int i = 1; i < ctx.unary().size(); i++) {
            Object right = visit(ctx.unary(i));
            double leftNumber = requireNumber(value, ctx.getStart(), "arithmetic");
            double rightNumber = requireNumber(right, ctx.unary(i).getStart(), "arithmetic");
            if (ctx.getChild(2 * i - 1).getText().equals("/")) {
                if (rightNumber == 0) {
                    throw error("RUNTIME", ctx.unary(i).getStart(), "Cannot divide by zero.");
                }
                value = leftNumber / rightNumber;
            } else {
                value = leftNumber * rightNumber;
            }
        }
        return value;
    }

    @Override
    public Object visitUnary(LumiLangParser.UnaryContext ctx) {
        if (ctx.primary() != null) return visit(ctx.primary());
        Object value = visit(ctx.unary());
        if (ctx.NOT() != null) return !requireBoolean(value, ctx.getStart(), "'!' operand");
        return -requireNumber(value, ctx.getStart(), "unary '-' operand");
    }

    @Override
    public Object visitPrimary(LumiLangParser.PrimaryContext ctx) {
        if (ctx.NUMBER() != null) return Double.parseDouble(ctx.NUMBER().getText());
        if (ctx.STRING() != null) return decodeString(ctx.STRING().getText());
        if (ctx.TRUE() != null) return true;
        if (ctx.FALSE() != null) return false;
        if (ctx.IDENTIFIER() != null) {
            return environment.get(ctx.IDENTIFIER().getText(), ctx.IDENTIFIER().getSymbol());
        }
        return visit(ctx.expression());
    }

    private int beginStatement(ParserRuleContext ctx) {
        int step = ++executedStatements;
        if (executedStatements > MAX_EXECUTED_STATEMENTS) {
            throw error("RUNTIME", ctx.getStart(),
                    "Execution stopped after reaching the 1,000-statement safety limit.");
        }
        return step;
    }

    private void finishStatement(ParserRuleContext ctx, int step) {
        Token start = ctx.getStart();
        int startIndex = start.getStartIndex();
        int stopIndex = ctx.getStop().getStopIndex();
        String statement = startIndex >= 0 && stopIndex >= startIndex
                ? start.getInputStream().getText(Interval.of(startIndex, stopIndex)).trim()
                : ctx.getText();
        trace.add(new ExecutionStep(step, start.getLine(), statement,
                environment.snapshot(), List.copyOf(output)));
    }

    private static boolean requireBoolean(Object value, Token token, String context) {
        if (value instanceof Boolean bool) return bool;
        throw error("RUNTIME", token, context + " must be Boolean.");
    }

    private static double requireNumber(Object value, Token token, String context) {
        if (value instanceof Double number) return number;
        throw error("RUNTIME", token, context + " must use Number values.");
    }

    private static LumiException error(String type, Token token, String message) {
        return new LumiException(type, token.getLine(), token.getCharPositionInLine() + 1, message);
    }

    private static String format(Object value) {
        if (value instanceof Double number) {
            if (number == Math.rint(number)) return Long.toString((long) number.doubleValue());
            return Double.toString(number);
        }
        return String.valueOf(value);
    }

    private static String decodeString(String literal) {
        String body = literal.substring(1, literal.length() - 1);
        return body.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
