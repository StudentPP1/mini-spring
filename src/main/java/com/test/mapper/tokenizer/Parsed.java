package com.test.mapper.tokenizer;

public class Parsed<T> {
    final T token;
    final int nextPos;

    public Parsed(T token, int nextPos) {
        this.token = token;
        this.nextPos = nextPos;
    }
}
