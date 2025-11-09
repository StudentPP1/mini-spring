package org.spring.utils;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public final class SimpleTypeConverter {
    private SimpleTypeConverter() {
    }

    private static final Map<Class<?>, Converter<?>> CONVERTERS = Map.ofEntries(
            Map.entry(String.class, s -> s),
            Map.entry(Boolean.class, s -> Boolean.valueOf(s.trim())),
            Map.entry(Integer.class, s -> Integer.valueOf(s.trim())),
            Map.entry(Long.class, s -> Long.valueOf(s.trim())),
            Map.entry(Double.class, s -> Double.valueOf(s.trim())),
            Map.entry(Float.class, s -> Float.valueOf(s.trim())),
            Map.entry(Short.class, s -> Short.valueOf(s.trim())),
            Map.entry(Byte.class, s -> Byte.valueOf(s.trim())),
            Map.entry(UUID.class, s -> UUID.fromString(s.trim())),
            Map.entry(LocalDate.class, s -> LocalDate.parse(s.trim())),
            Map.entry(Character.class, SimpleTypeConverter::parseChar)
    );

    private static final Map<Class<?>, Class<?>> PRIMITIVE_ALIASES = Map.of(
            boolean.class, Boolean.class,
            int.class, Integer.class,
            long.class, Long.class,
            double.class, Double.class,
            float.class, Float.class,
            short.class, Short.class,
            byte.class, Byte.class,
            char.class, Character.class
    );

    private static Character parseChar(String s) {
        String t = s.trim();
        if (t.length() != 1) {
            throw new IllegalArgumentException("char requires a single character");
        }
        return t.charAt(0);
    }

    public static <T> T convert(String raw, Class<T> target) {
        if (raw == null) return null;
        if (target.isEnum()) {
            Class<? extends Enum> enumClass = (Class<? extends Enum>) target;
            return target.cast(Enum.valueOf(enumClass, raw));
        }
        Class<?> key = PRIMITIVE_ALIASES.getOrDefault(target, target);
        Converter<?> converter = CONVERTERS.get(key);
        try {
            Object value = converter.apply(raw);
            return target.cast(value);
        } catch (Exception ignored) {
            throw new IllegalArgumentException("No converter for " + target.getName());
        }
    }
}
