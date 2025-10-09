package com.test.mapper.convertor.utils;

import com.test.mapper.convertor.ObjectValue;
import com.test.mapper.tokenizer.Token;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.List;

import static com.test.mapper.convertor.utils.ArrayParser.parseArray;
import static com.test.mapper.convertor.utils.PrimitiveParser.readPrimitive;
import static com.test.mapper.convertor.utils.ReflectionUtils.getField;
import static com.test.mapper.convertor.utils.ReflectionUtils.newClassInstance;
import static com.test.mapper.tokenizer.TokenType.*;

public class ObjectParser {
    private static final Logger log = LogManager.getLogger(ObjectParser.class);

    private ObjectParser() {

    }

    public static int parseObject(int pos, Object instance, List<Token> tokens) throws Exception {
        log.trace("check if OBJECT: {} is not empty", instance.getClass());
        if (tokens.get(pos).type() == END_OBJECT) {
            return pos + 1;
        }
        log.trace("start parsing object fields");
        while (true) {
            Token fieldName = tokens.get(pos++);
            log.trace("get FIELD_NAME: {}", fieldName.stringValue());
            if (fieldName.type() != FIELD_NAME) {
                throw new RuntimeException("Expected FIELD_NAME at " + (pos - 1));
            }
            if (tokens.get(pos++).type() != COLON) {
                throw new RuntimeException("Expected : at " + pos);
            }
            log.trace("skip colon token");
            String name = fieldName.stringValue();
            Field field = getField(instance.getClass(), name);
            field.setAccessible(true);
            log.trace("starting parsing value for field");
            switch (tokens.get(pos).type()) {
                case START_OBJECT: {
                    log.trace("value of field: OBJECT");
                    Object nested = newClassInstance(field.getType());
                    pos = parseObject(pos, nested, tokens);
                    field.set(instance, nested);
                    break;
                }
                case START_ARRAY: {
                    log.trace("value of field: ARRAY");
                    int startArrayIndex = pos + 1;
                    pos = parseArray(startArrayIndex, instance, field, tokens);
                    break;
                }
                default: {
                    log.trace("value of field: PRIMITIVE");
                    ObjectValue objectValue = readPrimitive(pos, field.getType(), tokens);
                    log.trace("value: [ {} ]", objectValue.value());
                    field.set(instance, objectValue.value());
                    pos = objectValue.nextPos();
                }
            }
            if (tokens.get(pos).type() == COMMA) {
                log.trace("skip comma token in object");
                pos++;
                continue;
            }
            if (tokens.get(pos).type() == END_OBJECT) {
                log.trace("parsing OBJECT is finished");
                return pos + 1;
            }
            throw new RuntimeException("Expected , or } at " + pos);
        }
    }
}
