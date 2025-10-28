package org.spring.mapper.convertor.utils;

import org.spring.mapper.convertor.ObjectValue;
import org.spring.mapper.tokenizer.Token;
import org.spring.utils.SimpleTypeConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

import static org.spring.mapper.tokenizer.TokenType.TRUE_VALUE;

public class PrimitiveParser {
    private PrimitiveParser() {
    }

    private static final Logger log = LogManager.getLogger(PrimitiveParser.class);

    public static ObjectValue readPrimitive(int pos, Class<?> targetType, List<Token> tokens) {
        Token token = tokens.get(pos);
        log.trace("primitive type: [ {} ]", token.type());
        switch (token.type()) {
            case NULL_VALUE -> {
                if (targetType.isPrimitive()) {
                    throw new RuntimeException("Null for primitive " + targetType.getName());
                }
                return new ObjectValue(null, pos + 1);
            }
            case TRUE_VALUE, FALSE_VALUE -> {
                Object value = token.type() == TRUE_VALUE;
                return new ObjectValue(value, pos + 1);
            }
            case STRING_VALUE -> {
                String string = token.stringValue();
                Object value = SimpleTypeConverter.convert(string, targetType);
                log.trace("parsing string valueType: [ {} ]", value);
                return new ObjectValue(value, pos + 1);
            }
            case NUMBER_VALUE -> {
                Object value = SimpleTypeConverter.convert(token.numberValue().toString(), targetType);
                log.trace("parsing number valueType: [ {} ]", value);
                return new ObjectValue(value, pos + 1);
            }
            default -> throw new RuntimeException("Not a primitive at " + pos + ": " + token.type());
        }
    }
}
