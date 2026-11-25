package lumilang.errors;

public final class LumiException extends RuntimeException {
    private final String type;
    private final int line;
    private final int column;

    public LumiException(String type, int line, int column, String message) {
        super(message);
        this.type = type;
        this.line = line;
        this.column = column;
    }

    public LumiError toError() {
        return new LumiError(type, line, column, getMessage());
    }
}
