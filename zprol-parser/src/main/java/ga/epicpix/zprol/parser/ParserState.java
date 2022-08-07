package ga.epicpix.zprol.parser;

import ga.epicpix.zprol.parser.tokens.LexerToken;
import ga.epicpix.zprol.utils.SeekIterator;

import java.util.Stack;

/**
 * This is thread dependant
 */
public class ParserState {

    private static final ThreadLocal<ParserState> states = new ThreadLocal<>();


    private final SeekIterator<LexerToken> tokens;
    private final Stack<Integer> locations;

    private ParserState(SeekIterator<LexerToken> tokens) {
        this.tokens = tokens;
        locations = new Stack<>();
    }

    public static void create(SeekIterator<LexerToken> tokens) {
        states.set(new ParserState(tokens));
    }

    public static void delete() {
        states.remove();
    }

    private static ParserState getState() {
        return states.get();
    }

    public static int popStartLocation() {
        return getState().tokens.get(getState().locations.pop()).getStart();
    }

    public static int getEndLocation() {
        return getState().tokens.current().getEnd();
    }

    public static void pushLocation() {
        getState().locations.push(getState().tokens.currentIndex());
    }

}
