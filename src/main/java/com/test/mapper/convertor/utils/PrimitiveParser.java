package com.test.mapper.convertor.utils;

import com.test.mapper.convertor.ObjectValue;
import com.test.mapper.tokenizer.Token;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.test.mapper.tokenizer.TokenType.TRUE_VALUE;

public class PrimitiveParser {
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
                Object value = convertFromString(string, targetType);
                log.trace("parsing string value: [ {} ]", value);
                return new ObjectValue(value, pos + 1);
            }
            case NUMBER_VALUE -> {
                Object value = convertFromNumber(token.numberValue(), targetType);
                log.trace("parsing number value: [ {} ]", value);
                return new ObjectValue(value, pos + 1);
            }
            default -> throw new RuntimeException("Not a primitive at " + pos + ": " + token.type());
        }
    }

    private static Object convertFromString(String s, Class<?> target) {
        if (target == String.class) return s;
        if (target.isEnum()) return Enum.valueOf((Class<Enum>) target, s);
        if (target == UUID.class) return UUID.fromString(s);
        if (target == LocalDate.class) return LocalDate.parse(s);
        return s;
    }

    private static Object convertFromNumber(BigDecimal n, Class<?> target) {
        if (target == int.class    || target == Integer.class) return n.intValue();
        if (target == long.class   || target == Long.class)    return n.longValue();
        if (target == double.class || target == Double.class)  return n.doubleValue();
        if (target == float.class  || target == Float.class)   return n.floatValue();
        if (target == short.class  || target == Short.class)   return n.shortValue();
        if (target == byte.class   || target == Byte.class)    return n.byteValue();
        if (target == BigDecimal.class)              return n;
        if (target == BigInteger.class)              return n.toBigInteger();
        return n;
    }
}
