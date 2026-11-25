package lumilang.protocol;

import lumilang.errors.LumiError;

import java.util.List;
import java.util.Map;

/** JSON contract consumed by the VS Code extension. */
public record ExecutionResult(boolean success, List<String> output, List<LumiError> errors,
                              List<ExecutionStep> trace, Map<String, Object> variables) {
    public static ExecutionResult success(List<String> output, List<ExecutionStep> trace,
                                          Map<String, Object> variables) {
        return new ExecutionResult(true, output, List.of(), trace, variables);
    }

    public static ExecutionResult failure(List<LumiError> errors) {
        return new ExecutionResult(false, List.of(), errors, List.of(), Map.of());
    }

    public static ExecutionResult failure(LumiError error, List<String> output,
                                          List<ExecutionStep> trace, Map<String, Object> variables) {
        return new ExecutionResult(false, List.copyOf(output), List.of(error),
                List.copyOf(trace), Map.copyOf(variables));
    }
}
