package ga.epicpix.zprol.parser.exceptions;

import ga.epicpix.zprol.parser.DataParser;
import ga.epicpix.zprol.parser.ParserLocation;
import ga.epicpix.zprol.parser.tokens.Token;
import ga.epicpix.zprol.parser.tree.ITree;
import ga.epicpix.zprol.utils.SeekIterator;

public class TokenLocatedException extends RuntimeException {

    private final DataParser parser;
    private final int start, end;

    public TokenLocatedException(String s) {
        super(s);
        parser = null;
        start = -1;
        end = -1;
    }

    public TokenLocatedException(String s, Token token) {
        super(s);
        parser = token.parser;
        start = token.getStart();
        end = token.getEnd();
    }

    public TokenLocatedException(String s, ITree tree, DataParser parser) {
        super(s);
        this.parser = parser;
        start = tree.getStartIndex();
        end = tree.getEndIndex();
    }

    public void printError() {
        if(parser != null) {
            try {
                ParserLocation startLoc = parser.getLocation(start);
                ParserLocation endLoc = parser.getLocation(end);
                System.err.println(getMessage() + ", error at " + parser.getFileName() + ":" + (endLoc.line + 1) + ":" + (startLoc.row + 1));

                String l = parser.getLines()[endLoc.line];
                System.err.println(l);

                StringBuilder b = new StringBuilder();
                for(int i = 0; i<startLoc.row; i++) b.append(" ");
                for(int i = 0; i<endLoc.row - startLoc.row; i++) b.append("^");
                System.err.println(b);
            } finally {
                if(Boolean.parseBoolean(System.getProperty("SHOW_STACK_TRACE"))) {
                    System.err.println("DEBUG INFO:");
                    StackTraceElement[] trace = getStackTrace();
                    for(StackTraceElement traceElement : trace)
                        System.err.println("\tat " + traceElement);
                }
            }
        } else {
            printStackTrace();
        }
    }
}
