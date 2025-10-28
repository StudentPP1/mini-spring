package org.spring.mapper.convertor.utils;

import org.spring.mapper.convertor.ObjectValue;
import org.spring.mapper.tokenizer.Token;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.spring.mapper.convertor.utils.ObjectParser.parseObject;
import static org.spring.mapper.convertor.utils.PrimitiveParser.readPrimitive;
import static org.spring.mapper.convertor.utils.ReflectionUtils.newClassInstance;
import static org.spring.mapper.convertor.utils.ReflectionUtils.resolveElementType;
import static org.spring.mapper.tokenizer.TokenType.COMMA;
import static org.spring.mapper.tokenizer.TokenType.END_ARRAY;
import static java.lang.reflect.Array.newInstance;
import static java.lang.reflect.Array.set;

public class ArrayParser {
    private static final Logger log = LogManager.getLogger(ArrayParser.class);

    private ArrayParser() {

    }
    public static int parseArray(int pos, Object instance, Field field, List<Token> tokens) throws Exception {
        Class<?> elementType = resolveElementType(field);
        log.trace("type of array element: {}", elementType);
        List<Object> out = new ArrayList<>();
        log.trace("check if array not empty");
        if (tokens.get(pos).type() == END_ARRAY) {
            setArrayOrList(instance, field, out);
            return pos + 1;
        }
        log.trace("starting parsing array elements");
        while (true) {
            ObjectValue value = readArrayElement(pos, elementType, tokens);
            out.add(value.value());
            pos = value.nextPos();

            if (tokens.get(pos).type() == COMMA) {
                log.trace("skip comma token in array");
                pos++;
                continue;
            }
            if (tokens.get(pos).type() == END_ARRAY) {
                setArrayOrList(instance, field, out);
                log.trace("parsing ARRAY is finished");
                return pos + 1;
            }
            throw new RuntimeException("Expected , or ] at " + pos);
        }
    }

    private static ObjectValue getSubArray(int pos, Class<?> targetType, List<Token> tokens) throws Exception {
        List<Object> list = new ArrayList<>();
        int i = pos + 1;
        if (tokens.get(i).type() != END_ARRAY) {
            while (true) {
                Class<?> elementType = targetType.isArray() ? targetType.getComponentType() : Object.class;
                ObjectValue value = readArrayElement(i, elementType, tokens);
                log.trace("parse element of sub array: {}", value);
                list.add(value.value());
                i = value.nextPos();
                if (tokens.get(i).type() == COMMA) {
                    log.trace("skip comma in sub array");
                    i++;
                    continue;
                }
                break;
            }
        }
        if (tokens.get(i).type() != END_ARRAY) {
            throw new RuntimeException("Unclosed array");
        }
        i++;
        log.trace("finish parsing sub array");
        Object value = targetType.isArray()
                ? listToArray(list, targetType.getComponentType())
                : list;
        return new ObjectValue(value, i);
    }

    private static ObjectValue readArrayElement(int pos, Class<?> targetType, List<Token> tokens) throws Exception {
        switch (tokens.get(pos).type()) {
            case START_OBJECT: {
                log.trace("element of array: OBJECT");
                Object nested = newClassInstance(targetType);
                int next = parseObject(pos, nested, tokens);
                return new ObjectValue(nested, next);
            }
            case START_ARRAY: {
                log.trace("element of array: ARRAY");
                return getSubArray(pos, targetType, tokens);
            }
            default:
                log.trace("element of array: PRIMITIVE");
                ObjectValue objectValue = readPrimitive(pos, targetType, tokens);
                log.trace("element: [ {} ]", objectValue.value());
                return objectValue;
        }
    }

    private static Object listToArray(List<?> list, Class<?> componentType) {
        Object array = newInstance(componentType, list.size());
        for (int i = 0; i < list.size(); i++) {
            set(array, i, list.get(i));
        }
        return array;
    }

    private static void setArrayOrList(Object instance, Field field, List<Object> list) throws IllegalAccessException {
        if (field.getType().isArray()) {
            Object array = listToArray(list, field.getType().getComponentType());
            log.trace("set ARRAY: {} to FIELD: {}", list, field.getName());
            field.set(instance, array);
        } else {
            log.trace("set LIST: {} to FIELD: {}", list, field.getName());
            field.set(instance, list);
        }
    }
}
