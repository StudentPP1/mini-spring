package org.spring.mapper.convertor.utils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

import static org.spring.mapper.convertor.ObjectMapper.write;

public final class WriteHelpers {

    private WriteHelpers() {}

    public static String serializeArray(Object array) {
        int len = Array.getLength(array);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < len; i++) {
            if (i > 0) sb.append(',');
            sb.append(write(Array.get(array, i)));
        }
        return sb.append(']').toString();
    }

    public static String serializeCollection(Collection<?> coll) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Object el : coll) {
            if (!first) sb.append(',');
            sb.append(write(el));
            first = false;
        }
        return sb.append(']').toString();
    }

    public static String serializeMap(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (!first) sb.append(',');
            sb.append(quote(String.valueOf(e.getKey())))
                    .append(':')
                    .append(write(e.getValue()));
            first = false;
        }
        return sb.append('}').toString();
    }

    public static String serializeObject(Object obj) {
        StringBuilder sb = new StringBuilder("{");
        Field[] fields = obj.getClass().getDeclaredFields();
        boolean first = true;
        for (Field f : fields) {
            f.setAccessible(true);
            try {
                Object v = f.get(obj);
                if (!first) sb.append(',');
                sb.append(quote(f.getName()))
                        .append(':')
                        .append(write(v));
                first = false;
            } catch (IllegalAccessException ignored) {}
        }
        return sb.append('}').toString();
    }

    public static String quote(String s) {
        return "\"" + escape(s) + "\"";
    }

    public static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }
}
