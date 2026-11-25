package lumilang;

import com.fasterxml.jackson.databind.ObjectMapper;
import lumilang.antlr.LumiLangLexer;
import lumilang.antlr.LumiLangParser;
import lumilang.errors.LumiError;
import lumilang.errors.LumiErrorListener;
import lumilang.errors.LumiException;
import lumilang.interpreter.LumiInterpreter;
import lumilang.protocol.ExecutionResult;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** CLI entry point. Reads LumiLang source from stdin and writes one JSON object to stdout. */
public final class Main {
    private static final ObjectMapper JSON = new ObjectMapper();

    private Main() { }

    public static void main(String[] args) throws IOException {
        String source = new String(System.in.readAllBytes(), StandardCharsets.UTF_8);
        ExecutionResult result = run(source);
        JSON.writeValue(System.out, result);
        System.out.println();
    }

    static ExecutionResult run(String source) {
        LumiErrorListener syntaxErrors = new LumiErrorListener();
        LumiLangLexer lexer = new LumiLangLexer(CharStreams.fromString(source));
        lexer.removeErrorListeners();
        lexer.addErrorListener(syntaxErrors);

        LumiLangParser parser = new LumiLangParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(syntaxErrors);
        LumiLangParser.ProgramContext program = parser.program();

        if (!syntaxErrors.getErrors().isEmpty()) {
            return ExecutionResult.failure(syntaxErrors.getErrors());
        }

        LumiInterpreter interpreter = new LumiInterpreter(source);
        try {
            interpreter.visit(program);
            return ExecutionResult.success(interpreter.getOutput(), interpreter.getTrace(),
                    interpreter.getVariables());
        } catch (LumiException exception) {
            return ExecutionResult.failure(exception.toError(), interpreter.getOutput(),
                    interpreter.getTrace(), interpreter.getVariables());
        } catch (RuntimeException exception) {
            // Keep stdout machine-readable; diagnostic details go to stderr.
            System.err.println("[LumiLang] Internal error: " + exception.getMessage());
            LumiError error = new LumiError("INTERNAL", 1, 1,
                    "An internal error stopped execution.");
            return ExecutionResult.failure(error, interpreter.getOutput(),
                    interpreter.getTrace(), interpreter.getVariables());
        }
    }
}
