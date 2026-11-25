package lumilang.interpreter;

import lumilang.errors.LumiException;
import org.antlr.v4.runtime.Token;

import java.util.LinkedHashMap;
import java.util.Map;

/** MVP uses one global environment, as agreed in the project overview. */
public final class Environment {
    private final Map<String, Object> values = new LinkedHashMap<>();

    public void declare(String name, Object value, Token token) {
        if (values.containsKey(name)) {
            throw error("SEMANTIC", token, "Variable '" + name + "' is already declared.");
        }
        values.put(name, value);
    }

    public void assign(String name, Object value, Token token) {
        if (!values.containsKey(name)) {
            throw error("SEMANTIC", token, "Undefined variable '" + name + "'.");
        }
        values.put(name, value);
    }

    public Object get(String name, Token token) {
        if (!values.containsKey(name)) {
            throw error("SEMANTIC", token, "Undefined variable '" + name + "'.");
        }
        return values.get(name);
    }

    public Map<String, Object> snapshot() {
        return new LinkedHashMap<>(values);
    }

    private static LumiException error(String type, Token token, String message) {
        return new LumiException(type, token.getLine(), token.getCharPositionInLine() + 1, message);
    }
}
