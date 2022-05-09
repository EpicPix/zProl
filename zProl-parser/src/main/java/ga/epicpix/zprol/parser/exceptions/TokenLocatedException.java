package ga.epicpix.zprol.parser.exceptions;

import ga.epicpix.zprol.parser.tokens.Token;

public class TokenLocatedException extends RuntimeException {

    private final Token token;

    public TokenLocatedException(String s) {
        super(s);
        token = null;
    }

    public TokenLocatedException(String s, Token token) {
        super(s);
        this.token = token;
    }

    public void printError() {
        if(token != null) {
            int line = token.endLocation.line();
            int row = token.endLocation.row();

            System.err.println(getMessage() + ", error at " + token.parser.getFileName() + ":" + (line + 1) + ":" + (row + 1));

            String l = token.parser.getLines()[line];
            System.err.println(l);

            int offset = token.startLocation.line() == line ? row - token.startLocation.row() : row;

            System.err.println(" ".repeat(row - offset) + "^".repeat(offset));
            if (Boolean.parseBoolean(System.getProperty("SHOW_STACK_TRACE"))) {
                System.err.println("DEBUG INFO:");
                StackTraceElement[] trace = getStackTrace();
                for (StackTraceElement traceElement : trace)
                    System.err.println("\tat " + traceElement);
            }
        }else {
            printStackTrace();
        }
    }
}
