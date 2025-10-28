package org.spring.mapper.tokenizer;

import java.math.BigDecimal;

public record Token(TokenType type, String stringValue, BigDecimal numberValue) {

    public static Token createToken(TokenType type) {
        return new Token(type, null, null);
    }

    public static Token stringToken(TokenType type, String value) {
        return new Token(type, value, null);
    }

    public static Token numberToken(TokenType type, BigDecimal value) {
        return new Token(type, null, value);
    }

    @Override
    public String toString() {
        return "Token{" +
                "type=" + type +
                ", stringValue='" + stringValue + '\'' +
                ", numberValue=" + numberValue +
                '}';
    }
}
