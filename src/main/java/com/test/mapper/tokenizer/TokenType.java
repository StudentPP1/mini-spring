package com.test.mapper.tokenizer;

public enum TokenType {
    START_OBJECT,
    END_OBJECT,
    START_ARRAY,
    END_ARRAY,

    FIELD_NAME,
    COLON,
    COMMA,

    STRING_VALUE,
    NULL_VALUE,
    NUMBER_VALUE,
    TRUE_VALUE,
    FALSE_VALUE,

    EOF
}
