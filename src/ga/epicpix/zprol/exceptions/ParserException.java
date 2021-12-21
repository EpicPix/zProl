package ga.epicpix.zprol.exceptions;

import ga.epicpix.zprol.DataParser;
import ga.epicpix.zprol.ParserLocation;

public class ParserException extends RuntimeException {

    private final DataParser parser;
    private final ParserLocation location;

    public ParserException(String message, DataParser parser) {
        super(message);
        this.parser = parser;
        this.location = parser.getLastLocation();
    }

    public void printError() {
        int line = location.line();
        int row = location.row();

        String msg = getMessage();
        if(msg.contains("\n")) {
            System.err.println(msg);
            System.err.println("Error is at " + parser.getFileName() + ":" + (line + 1) + ":" + (row + 1));
        }else {
            System.err.println(getMessage() + ", error at " + parser.getFileName() + ":" + (line + 1) + ":" + (row + 1));
        }
        System.err.println(parser.getLines()[line]);
        System.err.println(" ".repeat(Math.max(0, row)) + "^");
        if(Boolean.parseBoolean(System.getProperty("DEBUG"))) {
            System.err.println("DEBUG INFO:");
            StackTraceElement[] trace = getStackTrace();
            for(StackTraceElement traceElement : trace)
                System.err.println("\tat " + traceElement);
        }
    }

}
