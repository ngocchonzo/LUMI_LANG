package lumilang.protocol;

import java.util.List;
import java.util.Map;

public record ExecutionStep(int step, int line, String statement,
                            Map<String, Object> variables, List<String> output) { }
