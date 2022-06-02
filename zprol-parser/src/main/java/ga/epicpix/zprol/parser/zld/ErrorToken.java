package ga.epicpix.zprol.parser.zld;

import ga.epicpix.zprol.parser.exceptions.ParserException;

public class ErrorToken extends LanguageTokenFragment {

    ErrorToken(String errorMessage) {
        super(p -> {
            throw new ParserException(errorMessage, p);
        }, "");
    }

}
