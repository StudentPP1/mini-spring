package com.test.bean;

import java.util.List;
import java.util.function.Supplier;

/**
 * Metadata that's describe component for creating bean (instance of class)
 */
public record BeanDefinition(Class<?> beanClass, String name, Supplier<?> factory, Scope scope,
                             List<ConstructorArg> constructorArgs) {
    /**
     * Create instance of BeanDefinition
     *
     * @param beanClass       class type
     * @param name            name of bean (class instance)
     * @param constructorArgs constructor arguments of class
     */
    public BeanDefinition(Class<?> beanClass, String name, List<ConstructorArg> constructorArgs) {
        this(beanClass, name, null, Scope.SINGLETON, constructorArgs);
    }

    /**
     * Create instance of BeanDefinition
     *
     * @param beanClass       class type
     * @param name            bean's name
     * @param factory         how to create instance of class
     * @param scope           set scope of bean (singleton/prototype)
     * @param constructorArgs constructor arguments of class
     */
    public BeanDefinition(Class<?> beanClass, String name, Supplier<?> factory, Scope scope, List<ConstructorArg> constructorArgs) {
        this.beanClass = beanClass;
        this.name = name;
        this.factory = factory;
        this.scope = scope;
        this.constructorArgs = constructorArgs;
    }

    public boolean isSingleton() {
        return scope.equals(Scope.SINGLETON);
    }
}
