package com.test.mapper.tokenizer.utils;

import com.test.mapper.tokenizer.Parsed;
import com.test.mapper.tokenizer.Token;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.function.Consumer;

import static com.test.mapper.tokenizer.TokenType.FIELD_NAME;
import static com.test.mapper.tokenizer.TokenType.STRING_VALUE;
import static com.test.mapper.tokenizer.utils.HexParser.hexToChar;
import static java.lang.Character.isWhitespace;

public final class StringParser {
    private static final Logger log = LogManager.getLogger(StringParser.class);
    private static final Map<Character, Consumer<StringBuilder>> SIMPLE_ESCAPES = Map.of(
            '"', sb -> sb.append('"'),
            '\\', sb -> sb.append('\\'),
            '/', sb -> sb.append('/'),
            'b', sb -> sb.append('\b'),
            'f', sb -> sb.append('\f'),
            'n', sb -> sb.append('\n'),
            'r', sb -> sb.append('\r'),
            't', sb -> sb.append('\t')
    );

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
        Consumer<StringBuilder> action = SIMPLE_ESCAPES.get(escape);
        if (action != null) {
            action.accept(value);
            return i;
        }
        if (escape == 'u') {
            if (i + 4 > chars.length)
                throw new RuntimeException("Invalid unicode escape at " + (i - 2));
            int hexChar = hexToChar(chars[i], chars[i + 1], chars[i + 2], chars[i + 3]);
            value.append(hexChar);
            return i + 4;
        }
        throw new RuntimeException("Unknown escape: \\" + escape);
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