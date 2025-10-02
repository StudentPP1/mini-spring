package com.test.mapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO: add tokenizer to parse arrays and other types
public final class ObjectMapper {

    private static final Logger log = LogManager.getLogger(ObjectMapper.class);

    public static <T> T parse(String content, Class<T> objectClass) {
        try {
            T classInstance = objectClass.getDeclaredConstructor().newInstance();
            log.trace("Class instance: {}", classInstance);
            String[] fields = content.split("\n");
            int size = fields.length;
            log.trace("Parsed fields in request body: {}", Arrays.toString(fields));
            Pattern pattern = Pattern.compile("\"(.*)\": (.*)");
            for (int i = 0; i < size; i++) {
                String field = fields[i];
                if (i != size - 1) {
                    field = field.replace(",", "");
                }
                Matcher matcher = pattern.matcher(field);
                if (matcher.find()) {
                    String parsedField = matcher.group(1);
                    String parsedValue = matcher.group(2);
                    log.trace("field: {}\t value: {}", parsedField, parsedValue);
                    Field classField = classInstance.getClass().getDeclaredField(parsedField);
                    classField.setAccessible(true);
                    classField.set(classInstance, convert(parsedValue, classField.getType()));
                }
            }
            return classInstance;
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException |
                 NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object convert(String value, Class<?> fieldType) {
        if (value == null) return null;
        if (fieldType.equals(String.class)) return value.replace("\"", "");
        if (fieldType.equals(Integer.class)) return Integer.parseInt(value);
        if (fieldType.equals(Long.class)) return Long.parseLong(value);
        if (fieldType.equals(Boolean.class)) return Boolean.parseBoolean(value);
        if (fieldType.equals(Double.class)) return Double.parseDouble(value);
        if (fieldType.equals(LocalDate.class)) return LocalDate.parse(value);
        if (fieldType.isEnum()) {
            return Enum.valueOf((Class<Enum>) fieldType, value);
        }
        throw new IllegalArgumentException("Unsupported type: " + fieldType);
    }
}
