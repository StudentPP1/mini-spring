package org.spring.hibernate.annotation;

public enum FetchType {
    EAGER,
    LAZY // TODO: proxy from entities and override getNotes(), getPerson()
}
