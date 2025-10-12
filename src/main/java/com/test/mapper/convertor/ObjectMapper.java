package com.test.mapper.convertor;

import com.test.mapper.tokenizer.Token;
import com.test.mapper.tokenizer.TokenType;
import com.test.mapper.tokenizer.Tokenizer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.test.mapper.convertor.utils.ObjectParser.parseObject;
import static com.test.mapper.tokenizer.TokenType.START_OBJECT;


public final class ObjectMapper {
    private static final Logger log = LogManager.getLogger(ObjectMapper.class);

    private ObjectMapper() {
    }
    // TODO: refactor & add logs
    public static <T> String write(T obj) {
        if (obj == null) return "null";

        if (obj instanceof String s)
            return "\"" + escape(s) + "\"";
        if (obj instanceof Number || obj instanceof Boolean)
            return obj.toString();
        if (obj.getClass().isArray()) {
            int len = Array.getLength(obj);
            StringBuilder arr = new StringBuilder("[");
            for (int i = 0; i < len; i++) {
                if (i > 0) arr.append(',');
                arr.append(write(Array.get(obj, i)));
            }
            return arr.append(']').toString();
        }
        if (obj instanceof Collection<?> coll) {
            StringBuilder arr = new StringBuilder("[");
            boolean first = true;
            for (Object el : coll) {
                if (!first) arr.append(',');
                arr.append(write(el));
                first = false;
            }
            return arr.append(']').toString();
        }
        if (obj instanceof Map<?,?> map) {
            StringBuilder json = new StringBuilder("{");
            boolean first = true;
            for (var e : map.entrySet()) {
                if (!first) json.append(',');
                json.append('"').append(escape(String.valueOf(e.getKey()))).append('"')
                        .append(':').append(write(e.getValue()));
                first = false;
            }
            return json.append('}').toString();
        }

        StringBuilder json = new StringBuilder("{");
        Field[] fields = obj.getClass().getDeclaredFields();
        boolean first = true;
        for (Field f : fields) {
            f.setAccessible(true);
            try {
                Object v = f.get(obj);
                if (!first) json.append(',');
                json.append('"').append(f.getName()).append('"')
                        .append(':').append(write(v));
                first = false;
            } catch (IllegalAccessException ignored) {}
        }
        return json.append('}').toString();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
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
            throw new RuntimeException("Parse failed: " + e.getMessage(), e);
        }
    }
}