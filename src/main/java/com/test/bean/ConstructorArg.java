package com.test.bean;

/**
 * kind=BEAN  -> find bean by beanType
 * kind=VALUE -> resolve expression by PropertyResolver and convert to primitive
 */
public record ConstructorArg(Type type, Class<?> beanType, String expression) {
    public enum Type { BEAN, VALUE }

    public static ConstructorArg bean(Class<?> beanType) { // injection by type
        return new ConstructorArg(Type.BEAN, beanType, null);
    }

    public static ConstructorArg value(String expression) { // @Value("${http.timeout:5000}")
        return new ConstructorArg(Type.VALUE, null, expression);
    }
}