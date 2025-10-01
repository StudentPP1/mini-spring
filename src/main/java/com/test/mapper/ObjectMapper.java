package com.test.mapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ObjectMapper {

    private static final Logger log = LogManager.getLogger(ObjectMapper.class);

    public static <T> T parse(String content, Class<T> objectClass) {
        try {
            T classInstance = objectClass.getDeclaredConstructor().newInstance();
            log.trace("Class instance: {}", classInstance);
            String[] fields = content.split("\n");
            log.trace("Parsed fields in request body: {}", Arrays.toString(fields));
            Pattern pattern = Pattern.compile("\"(.*)\": \"(.*)\"");
            for (String field : fields) {
                Matcher matcher = pattern.matcher(field);
                if (matcher.find()) {
                    String parsedField = matcher.group(1);
                    String parsedValue = matcher.group(2);
                    log.trace("field: {}\t value: {}", parsedField, parsedValue);
                    Field classField = classInstance.getClass().getDeclaredField(parsedField);
                    classField.setAccessible(true);
                    classField.set(classInstance, parsedValue);
                }
            }
            return classInstance;
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException |
                 NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
}
