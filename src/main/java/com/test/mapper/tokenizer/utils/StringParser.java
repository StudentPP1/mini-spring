package com.test.mapper.tokenizer.utils;

import com.test.mapper.tokenizer.Parsed;
import com.test.mapper.tokenizer.Token;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.test.mapper.tokenizer.TokenType.FIELD_NAME;
import static com.test.mapper.tokenizer.TokenType.STRING_VALUE;
import static com.test.mapper.tokenizer.utils.HexParser.hexToChar;
import static java.lang.Character.isWhitespace;

public class StringParser {
    private static final Logger log = LogManager.getLogger(StringParser.class);

    private StringParser() {

    }

    public static int skipWhitespace(char[] chars, int pos) {
        while (pos < chars.length && isWhitespace(chars[pos])) pos++;
        return pos;
    }

    public static int parseEscape(int i, char[] chars, StringBuilder value) {
        if (i >= chars.length) {
            throw new RuntimeException("Bad escape at end");
        }
        char escape = chars[i++];
        switch (escape) {
            case '"':
                value.append('"');
                return i;
            case '\\':
                value.append('\\');
                return i;
            case '/':
                value.append('/');
                return i;
            case 'b':
                value.append('\b');
                return i;
            case 'f':
                value.append('\f');
                return i;
            case 'n':
                value.append('\n');
                return i;
            case 'r':
                value.append('\r');
                return i;
            case 't':
                value.append('\t');
                return i;
            case 'u': {
                if (i + 4 > chars.length)
                    throw new RuntimeException("Invalid unicode escape at " + (i - 2));
                int hexChar = hexToChar(chars[i], chars[i + 1], chars[i + 2], chars[i + 3]);
                value.append(hexChar);
                return i + 4;
            }
            default:
                throw new RuntimeException("Unknown escape: \\" + escape);
        }
    }

    public static Parsed<Token> parseStringToken(int afterQuote, char[] chars, StringBuilder value) {
        int peekQuoteIndex = skipWhitespace(chars, afterQuote);
        boolean isFieldName = peekQuoteIndex < chars.length && chars[peekQuoteIndex] == ':';
        Token token = isFieldName
                ? Token.stringToken(FIELD_NAME, value.toString())
                : Token.stringToken(STRING_VALUE, value.toString());
        log.trace("find: {} with valueType: [ {} ]", token.type(), token.stringValue());
        return new Parsed<>(token, afterQuote);
    }
}