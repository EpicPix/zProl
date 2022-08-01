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
            var start = token.getStartLocation();
            var end = token.getEndLocation();

            System.err.println(getMessage() + ", error at " + token.parser.getFileName() + ":" + (end.line() + 1) + ":" + (end.row() + 1));

            String l = token.parser.getLines()[end.line()];
            System.err.println(l);

            int offset = start.line() == end.line() ? end.row() - start.row() : end.row();

            System.err.println(" ".repeat(end.row() - offset) + "^".repeat(offset));
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
