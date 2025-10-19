package com.test.bean;

/**
 * @param valueType when VALUE
 * @param typeRef when BEAN
 */
public record ConstructorArg(Type type, Object valueType, Class<?> typeRef) {
    public enum Type {VALUE, BEAN}

    public static ConstructorArg valueType(Object v) {
        return new ConstructorArg(Type.VALUE, v, null);
    }

    public static ConstructorArg beanType(Class<?> t) {
        return new ConstructorArg(Type.BEAN, null, t);
    }
}
