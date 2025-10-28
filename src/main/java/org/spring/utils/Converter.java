package org.spring.utils;

@FunctionalInterface
public interface Converter<T> {
    T apply(String raw) throws Exception;
}
