package org.spring.mapper.tokenizer.utils;

import static java.lang.Character.isDigit;

public class IntegerParser {
    private IntegerParser() {

    }

    public static int parseMinus(char[] chars, int i, int len) {
        if (i < len && chars[i] == '-') i++;
        return i;
    }

    public static int parseIntegerPart(char[] chars, int i, int len) {
        if (i >= len || !isDigit(chars[i])) {
            throw new RuntimeException("Digit expected at " + i);
        }
        if (chars[i] == '0') {
            return i + 1;
        }
        while (i < len && isDigit(chars[i])) i++;
        return i;
    }

    public static int parseExponentPart(char[] chars, int i, int length) {
        if (i < length && (chars[i] == 'e' || chars[i] == 'E')) {
            i++;
            if (i < length && (chars[i] == '+' || chars[i] == '-')) {
                i++;
            }
            if (i >= length || !isDigit(chars[i])) {
                throw new RuntimeException("Digit expected in exponent at " + i);
            }
            while (i < length && isDigit(chars[i])) {
                i++;
            }
        }
        return i;
    }

    public static int parseFractionPart(char[] chars, int i, int length) {
        if (i < length && chars[i] == '.') {
            i++;
            if (i >= length || !isDigit(chars[i])) {
                throw new RuntimeException("Digit expected after '.' at " + i);
            }
            while (i < length && isDigit(chars[i])) {
                i++;
            }
        }
        return i;
    }
}
