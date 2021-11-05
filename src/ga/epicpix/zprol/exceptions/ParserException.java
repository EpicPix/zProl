package ga.epicpix.zprol.exceptions;

import ga.epicpix.zprol.DataParser;
import ga.epicpix.zprol.ParserLocation;

public class ParserException extends RuntimeException {

    private final DataParser parser;
    private final ParserLocation location;

    public ParserException(String message, DataParser parser, ParserLocation location) {
        super(message);
        this.parser = parser;
        this.location = location;
    }

    public void printError() {
        System.err.println(getMessage() + ", error at " + (location.line + 1) + ":" + (location.row + 1));
        System.err.println(parser.getLines()[location.line]);
        StringBuilder str = new StringBuilder();
        for(int i = 0; i<location.row; i++) {
            str.append(' ');
        }
        System.err.println(str + "^");
        if(Boolean.parseBoolean(System.getProperty("DEBUG"))) {
            System.err.println("DEBUG INFO:");
            StackTraceElement[] trace = getStackTrace();
            for(StackTraceElement traceElement : trace)
                System.err.println("\tat " + traceElement);
        }
    }

}
