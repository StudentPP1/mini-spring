package com.test.mapper.objectMapper;

import com.test.mapper.tokenizer.Token;
import com.test.mapper.tokenizer.TokenType;
import com.test.mapper.tokenizer.Tokenizer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.*;

import static com.test.mapper.tokenizer.TokenType.*;


public final class ObjectMapper {
    private static final Logger log = LogManager.getLogger(ObjectMapper.class);

    private ObjectMapper() {
    }

    public static <T> T parse(String content, Class<T> objectClass) {
        try {
            List<Token> tokens = Tokenizer.getTokens(content);
            T instance = objectClass.getDeclaredConstructor().newInstance();
            int pos = 0;
            Token startToken = tokens.get(pos);
            log.trace("check is first token is: {");
            if (startToken.type() != START_OBJECT) {
                throw new RuntimeException("Expected { at " + pos);
            }
            int startObjectIndex = pos + 1;
            pos = parseObject(startObjectIndex, instance, tokens);
            if (tokens.get(pos).type() != TokenType.EOF) {
                throw new RuntimeException("Extra data after root object at " + pos);
            }
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Parse failed: " + e.getMessage(), e);
        }
    }

    record ObjectValue(Object value, int nextPos) {}

    static int parseObject(int pos, Object instance, List<Token> tokens) throws Exception {
        if (tokens.get(pos).type() == END_OBJECT) {
            return pos + 1;
        }

        while (true) {
            Token fieldName = tokens.get(pos++);
            if (fieldName.type() != FIELD_NAME) {
                throw new RuntimeException("Expected FIELD_NAME at " + (pos - 1));
            }

            if (tokens.get(pos++).type() != COLON) {
                throw new RuntimeException("Expected : at " + pos);
            }

            String name = fieldName.stringValue();
            Field field = getField(instance.getClass(), name);
            field.setAccessible(true);

            switch (tokens.get(pos).type()) {
                case START_OBJECT: {
                    Object nested = newInstance(field.getType());
                    pos = parseObject(pos, nested, tokens);
                    field.set(instance, nested);
                }
                case START_ARRAY: {
                    int startArrayIndex = pos + 1;
                    pos = parseArray(startArrayIndex, instance, field, tokens);
                }
                default: {
                    ObjectValue objectValue = readPrimitive(pos, field.getType(), tokens);
                    field.set(instance, objectValue.value());
                    pos = objectValue.nextPos();
                }
            }
            if (tokens.get(pos).type() == COMMA) {
                pos++;
                continue;
            }
            if (tokens.get(pos).type() == END_OBJECT) {
                return pos + 1;
            }
            throw new RuntimeException("Expected , or } at " + pos);
        }
    }

    static int parseArray(int pos, Object instance, Field field, List<Token> tokens) throws Exception {
        Class<?> elementType = resolveElementType(field);
        List<Object> out = new ArrayList<>();
        if (tokens.get(pos).type() == END_ARRAY) {
            setArrayOrList(instance, field, out);
            return pos + 1;
        }
        while (true) {
            ObjectValue value = readArrayElement(pos, elementType, tokens);
            out.add(value.value());
            pos = value.nextPos();

            if (tokens.get(pos).type() == COMMA) {
                pos++;
                continue;
            }
            if (tokens.get(pos).type() == END_ARRAY) {
                setArrayOrList(instance, field, out);
                return pos + 1;
            }
            throw new RuntimeException("Expected , or ] at " + pos);
        }
    }

    static ObjectValue readArrayElement(int pos, Class<?> targetType, List<Token> tokens) throws Exception {
        switch (tokens.get(pos).type()) {
            case START_OBJECT: {
                Object nested = newInstance(targetType);
                int next = parseObject(pos, nested, tokens);
                return new ObjectValue(nested, next);
            }
            case START_ARRAY: {
                List<Object> list = new ArrayList<>();
                int i = pos + 1;
                if (tokens.get(i).type() != END_ARRAY) {
                    while (true) {
                        Class<?> elementType = targetType.isArray() ? targetType.getComponentType() : Object.class;
                        ObjectValue value = readArrayElement(i, elementType, tokens);
                        list.add(value.value());
                        i = value.nextPos();
                        if (tokens.get(i).type() == COMMA) {
                            i++;
                            continue;
                        }
                        break;
                    }
                }
                if (tokens.get(i).type() != END_ARRAY) throw new RuntimeException("Unclosed array");
                i++;
                Object value = targetType.isArray()
                        ? listToArray(list, targetType.getComponentType())
                        : list;
                return new ObjectValue(value, i);
            }
            default:
                return readPrimitive(pos, targetType, tokens);
        }
    }

    static ObjectValue readPrimitive(int pos, Class<?> targetType, List<Token> tokens) {
        Token token = tokens.get(pos);
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
                return new ObjectValue(value, pos + 1);
            }
            case NUMBER_VALUE -> {
                Object value = convertFromNumber(token.numberValue(), targetType);
                return new ObjectValue(value, pos + 1);
            }
            default -> throw new RuntimeException("Not a primitive at " + pos + ": " + token.type());
        }
    }

    static Object convertFromString(String s, Class<?> target) {
        if (target == String.class) return s;
        if (target.isEnum()) return Enum.valueOf((Class<Enum>) target, s);
        if (target == UUID.class) return UUID.fromString(s);
        if (target == LocalDate.class) return LocalDate.parse(s);
        return s;
    }

    static Object convertFromNumber(java.math.BigDecimal n, Class<?> target) {
        if (target == int.class    || target == Integer.class) return n.intValue();
        if (target == long.class   || target == Long.class)    return n.longValue();
        if (target == double.class || target == Double.class)  return n.doubleValue();
        if (target == float.class  || target == Float.class)   return n.floatValue();
        if (target == short.class  || target == Short.class)   return n.shortValue();
        if (target == byte.class   || target == Byte.class)    return n.byteValue();
        if (target == java.math.BigDecimal.class)              return n;
        if (target == java.math.BigInteger.class)              return n.toBigInteger();
        return n;
    }

    static Field getField(Class<?> cls, String name) throws NoSuchFieldException {
        Class<?> currentClass = cls;
        while (currentClass != null) {
            try {
                return currentClass.getDeclaredField(name);
            }
            catch (NoSuchFieldException ignore) {
                currentClass = currentClass.getSuperclass();
            }
        }
        assert cls != null;
        throw new NoSuchFieldException("Field " + name + " not found in " + cls.getName());
    }

    static <T> T newInstance(Class<T> cls) {
        try {
            var constructor = cls.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("No default ctor for " + cls.getName(), e);
        }
    }

    static void setArrayOrList(Object instance, Field field, List<Object> list) throws IllegalAccessException {
        if (field.getType().isArray()) {
            Object array = listToArray(list, field.getType().getComponentType());
            field.set(instance, array);
        } else {
            field.set(instance, list);
        }
    }

    static Object listToArray(List<?> list, Class<?> componentType) {
        Object arr = java.lang.reflect.Array.newInstance(componentType, list.size());
        for (int i = 0; i < list.size(); i++) {
            java.lang.reflect.Array.set(arr, i, list.get(i));
        }
        return arr;
    }

    static Class<?> resolveElementType(Field field) {
        if (field.getType().isArray()) {
            return field.getType().getComponentType();
        }
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType parameterizedType) {
            var args = parameterizedType.getActualTypeArguments();
            if (args.length == 1 && args[0] instanceof Class<?> classType) {
                return classType;
            }
        }
        return Object.class;
    }
}