package lumilang.errors;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.RecognitionException;

import java.util.ArrayList;
import java.util.List;

/** Converts lexer and parser errors to the shared, 1-based JSON error format. */
public final class LumiErrorListener extends BaseErrorListener {
    private final List<LumiError> errors = new ArrayList<>();

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine, String message,
                            RecognitionException exception) {
        errors.add(new LumiError("SYNTAX", line, charPositionInLine + 1, message));
    }

    public List<LumiError> getErrors() {
        return List.copyOf(errors);
    }
}
