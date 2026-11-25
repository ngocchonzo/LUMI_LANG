package lumilang.errors;

public record LumiError(String type, int line, int column, String message) { }
