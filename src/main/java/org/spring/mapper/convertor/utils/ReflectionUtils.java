package org.spring.mapper.convertor.utils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class ReflectionUtils {
    private ReflectionUtils() {

    }

    public static Class<?> resolveElementType(Field field) {
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

    public static Field getField(Class<?> cls, String name) throws NoSuchFieldException {
        Class<?> currentClass = cls;
        while (currentClass != null) {
            try {
                return currentClass.getDeclaredField(name);
            }
            catch (NoSuchFieldException ignored) {
                currentClass = currentClass.getSuperclass();
            }
        }
        throw new NoSuchFieldException("Field " + name + " not found in " + cls.getName());
    }

    public static <T> T newClassInstance(Class<T> cls) {
        try {
            var constructor = cls.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("No default ctor for " + cls.getName(), e);
        }
    }
}
