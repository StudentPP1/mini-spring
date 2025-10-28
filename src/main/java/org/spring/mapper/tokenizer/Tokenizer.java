package org.spring.mapper.tokenizer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.spring.mapper.tokenizer.TokenType.*;
import static org.spring.mapper.tokenizer.utils.IntegerParser.*;
import static org.spring.mapper.tokenizer.utils.StringParser.*;
import static java.lang.Character.isDigit;
import static java.lang.Character.isWhitespace;

public class Tokenizer {
    private static final Logger log = LogManager.getLogger(Tokenizer.class);

    private Tokenizer() {

    }

    public static List<Token> getTokens(String content) {
        List<Token> tokens = new ArrayList<>();
        char[] chars = content.toCharArray();
        int pos = 0;

        while (pos < chars.length) {
            pos = skipWhitespace(chars, pos);
            if (pos >= chars.length) {
                break;
            }
            char c = chars[pos];
            pos = parseChar(c, chars, pos, tokens);
        }
        tokens.add(Token.createToken(EOF));
        return tokens;
    }

    private static int parseChar(char startChar, char[] chars, int pos, List<Token> tokens) {
        return switch (startChar) {
            case '{' -> {
                log.trace("find START_OBJECT at pos: {}", pos);
                tokens.add(Token.createToken(START_OBJECT));
                yield pos + 1;
            }
            case '}' -> {
                log.trace("find END_OBJECT at pos: {}", pos);
                tokens.add(Token.createToken(END_OBJECT));
                yield pos + 1;
            }
            case '[' -> {
                log.trace("find START_ARRAY at pos: {}", pos);
                tokens.add(Token.createToken(START_ARRAY));
                yield pos + 1;
            }
            case ']' -> {
                log.trace("find END_ARRAY at pos: {}", pos);
                tokens.add(Token.createToken(END_ARRAY));
                yield pos + 1;
            }
            case ':' -> {
                log.trace("find COLON at pos: {}", pos);
                tokens.add(Token.createToken(COLON));
                yield pos + 1;
            }
            case ',' -> {
                log.trace("find COMMA at pos: {}", pos);
                tokens.add(Token.createToken(COMMA));
                yield pos + 1;
            }
            default -> parseValue(startChar, chars, pos, tokens);
        };
    }

    private static int parseValue(char startChar, char[] chars, int pos, List<Token> tokens) {
        if (startChar == '"') {
            log.trace("start parsing string at pos: {}", pos);
            Parsed<Token> parsed = parseString(chars, pos);
            tokens.add(parsed.token);
            return parsed.nextPos;
        } else if (isDigit(startChar) || startChar == '-') {
            log.trace("start parsing number at pos: {}", pos);
            Parsed<Token> parsed = parseNumber(chars, pos);
            tokens.add(parsed.token);
            return parsed.nextPos;
        } else if (startsWith(chars, pos, "true")) {
            checkValueBoundary(chars, pos + 4);
            tokens.add(Token.createToken(TRUE_VALUE));
            log.trace("find TRUE valueType at pos {}", pos);
            return pos + 4;
        } else if (startsWith(chars, pos, "false")) {
            checkValueBoundary(chars, pos + 5);
            tokens.add(Token.createToken(FALSE_VALUE));
            log.trace("find FALSE valueType at pos: {}", pos);
            return pos + 5;
        } else if (startsWith(chars, pos, "null")) {
            checkValueBoundary(chars, pos + 4);
            tokens.add(Token.createToken(NULL_VALUE));
            log.trace("find NULL valueType at pos: {}", pos);
            return pos + 4;
        } else {
            throw new RuntimeException("Unexpected char '" + startChar + "' at " + pos);
        }
    }

    private static boolean startsWith(char[] chars, int startIndex, String pattern) {
        int patternLength = pattern.length();
        if (startIndex + patternLength > chars.length) {
            return false;
        }
        for (int i = 0; i < patternLength; i++) {
            if (chars[startIndex + i] != pattern.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static void checkValueBoundary(char[] chars, int pos) {
        if (pos >= chars.length) return;
        char c = chars[pos];
        if (!(isWhitespace(c) || c == ',' || c == ']' || c == '}')) {
            throw new RuntimeException("Invalid valueType boundary at " + pos + ", got '" + c + "'");
        }
    }

    private static Parsed<Token> parseString(char[] chars, int doubleQuoteIndex) {
        int i = doubleQuoteIndex + 1;
        StringBuilder value = new StringBuilder();
        while (i < chars.length) {
            char c = chars[i++];
            if (c == '"') {
                return parseStringToken(i, chars, value);
            }
            if (c == '\\') {
                log.trace("find escape string at pos: {}", i);
                i = parseEscape(i, chars, value);
            } else {
                value.append(c);
            }
        }
        throw new RuntimeException("Unterminated string at " + doubleQuoteIndex);
    }

    private static Parsed<Token> parseNumber(char[] chars, int start) {
        int length = chars.length;
        int i = start;
        i = parseMinus(chars, i, length);
        i = parseIntegerPart(chars, i, length);
        i = parseFractionPart(chars, i, length);
        i = parseExponentPart(chars, i, length);
        checkValueBoundary(chars, i);
        String parsedNumber = String.valueOf(chars, start, i - start);
        log.trace("find NUMBER with valueType: [ {} ] at pos: {}", parsedNumber, start);
        return new Parsed<>(Token.numberToken(NUMBER_VALUE, new BigDecimal(parsedNumber)), i);
    }
}