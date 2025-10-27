package com.test.mapper.convertor;

import com.test.mapper.tokenizer.Token;
import com.test.mapper.tokenizer.TokenType;
import com.test.mapper.tokenizer.Tokenizer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.test.mapper.convertor.utils.ObjectParser.parseObject;
import static com.test.mapper.convertor.utils.WriteHelpers.*;
import static com.test.mapper.tokenizer.TokenType.START_OBJECT;


public final class ObjectMapper {
    private static final Logger log = LogManager.getLogger(ObjectMapper.class);

    private ObjectMapper() {
    }

    public static String write(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof String s) return quote(s);
        if (obj instanceof Number || obj instanceof Boolean) return obj.toString();
        if (obj.getClass().isArray()) return serializeArray(obj);
        if (obj instanceof Collection<?> coll) return serializeCollection(coll);
        if (obj instanceof Map<?, ?> map) return serializeMap(map);
        return serializeObject(obj);
    }

    public static <T> T parse(String content, Class<T> objectClass) {
        try {
            List<Token> tokens = Tokenizer.getTokens(content);
            log.trace("input tokens: {}", tokens);
            T instance = objectClass.getDeclaredConstructor().newInstance();
            int pos = 0;
            Token startToken = tokens.get(pos);
            log.trace("check if first token is: {");
            if (startToken.type() != START_OBJECT) {
                throw new RuntimeException("Expected { at " + pos);
            }
            int startObjectIndex = pos + 1;
            log.trace("start parsing root object: {}", objectClass);
            pos = parseObject(startObjectIndex, instance, tokens);
            if (tokens.get(pos).type() != TokenType.EOF) {
                throw new RuntimeException("Extra data after root object at " + pos);
            }
            log.trace("finish parsing root object, return instance: {}", instance);
            return instance;
        } catch (Exception e) {
            log.error("Parse failed: {}", e.getMessage());
            throw new RuntimeException("Parse failed: " + e.getMessage(), e);
        }
    }
}