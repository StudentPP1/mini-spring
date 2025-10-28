package org.spring.mapper.tokenizer.utils;

public class HexParser {
    private HexParser() {

    }

    public static int hexCharTo16(char c) {
        if (c >= '0' && c <= '9') return (c - '0');
        if (c >= 'a' && c <= 'f') return 10 + (c - 'a');
        if (c >= 'A' && c <= 'F') return 10 + (c - 'A');
        throw new IllegalArgumentException("Invalid hex character: " + c);
    }

    public static int hexToUnicodeNumber(char c1, char c2, char c3, char c4) {
        char[] chars = new char[]{c1, c2, c3, c4};
        int sum = 0;
        int weight = 1;
        for (int i = chars.length - 1; i >= 0; i--) {
            sum += hexCharTo16(chars[i]) * weight;
            weight *= 16;
        }
        return sum;
    }

    public static char hexToChar(char char1, char char2, char char3, char char4) {
        return (char) hexToUnicodeNumber(char1, char2, char3, char4);
    }
}
