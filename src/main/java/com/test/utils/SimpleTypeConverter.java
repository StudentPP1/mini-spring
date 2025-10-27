package com.test.utils;

public final class SimpleTypeConverter {
    private SimpleTypeConverter() {}
    public static <T> T convert(String raw, Class<T> target) {
        if (raw == null) return null;
        if (target == String.class) return (T) raw;
        if (target == int.class || target == Integer.class) return (T) Integer.valueOf(raw);
        if (target == long.class || target == Long.class) return (T) Long.valueOf(raw);
        if (target == boolean.class || target == Boolean.class) return (T) Boolean.valueOf(raw);
        if (target == double.class || target == Double.class) return (T) Double.valueOf(raw);
        if (target == float.class  || target == Float.class)  return (T) Float.valueOf(raw);
        if (target == short.class  || target == Short.class)  return (T) Short.valueOf(raw);
        if (target == byte.class   || target == Byte.class)   return (T) Byte.valueOf(raw);
        if (target == char.class   || target == Character.class) {
            if (raw.length() != 1) throw new IllegalArgumentException("char requires 1-length string");
            return (T) Character.valueOf(raw.charAt(0));
        }
        if (target.isEnum()) {
            Class<? extends Enum> et = (Class<? extends Enum>) target;
            T e = (T) Enum.valueOf(et, raw);
            return e;
        }
        throw new IllegalArgumentException("No converter for " + target.getName());
    }
}
