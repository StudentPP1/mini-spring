package com.test.bean;

import java.util.*;

public final class BeanFactory {
    private final Map<String, BeanDefinition> definitions = new HashMap<>();
    private final Map<String, Object> singletons = new HashMap<>();
    private final ThreadLocal<Set<String>> creating = ThreadLocal.withInitial(HashSet::new);

    public BeanFactory(List<BeanDefinition> definitions) {
        definitions.forEach(definition ->
                this.definitions.putIfAbsent(definition.name(), definition)
        );
        for (BeanDefinition definition : definitions) {
            if (definition.isSingleton()) {
                singletons.putIfAbsent(definition.name(), createBean(definition.name(), definition));
            }
        }
    }

    public Object getBean(String name) {
        BeanDefinition def = definitions.get(name);
        if (def.isSingleton()) {
            return singletons.computeIfAbsent(name, n -> createBean(n, def));
        }
        return createBean(name, def);
    }

    public <T> T getBean(String name, Class<T> type) {
        Object bean = getBean(name);
        if (!type.isInstance(bean))
            throw new ClassCastException("Bean '" + name + "' is not " + type.getName());
        return (T) bean;
    }

    private Object createBean(String name, BeanDefinition definition) {
        if (!creating.get().add(name))
            throw new IllegalStateException("Circular dependency at '" + name + "'");
        try {
            if (definition.factory() != null) {
                return definition.factory().get();
            }
            return instantiateViaConstructor(definition);
        } finally {
            creating.get().remove(name);
        }
    }

    private Object instantiateViaConstructor(BeanDefinition definition) {
        Class<?> classBean = definition.beanClass();
        var constructors = new ArrayList<>(List.of(classBean.getDeclaredConstructors()));
        constructors.sort(Comparator.comparingInt(c -> -c.getParameterCount()));
        for (var constructor : constructors) {
            if (constructor.isVarArgs()) continue;
            Object[] args = tryResolveArgs(constructor.getParameterTypes(), definition.constructorArgs());
            if (args != null) {
                try {
                    constructor.setAccessible(true);
                    return constructor.newInstance(args);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException("Failed to instantiate " + classBean.getName(), e);
                }
            }
        }
        try {
            var noArg = classBean.getDeclaredConstructor();
            noArg.setAccessible(true);
            return noArg.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("No suitable constructor for " + classBean.getName(), e);
        }
    }

    private Object[] tryResolveArgs(Class<?>[] parameterTypes, List<ConstructorArg> constructorArgs) {
        if (constructorArgs.size() < parameterTypes.length) return null;
        Object[] out = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            ConstructorArg arg = constructorArgs.get(i);
            Class<?> parameterType = parameterTypes[i];
            if (arg.type() == ConstructorArg.Type.BEAN) {
                Object injectBean = getBean(resolveBeanNameByType(parameterType, arg.typeRef()));
                if (injectBean == null) return null;
                out[i] = injectBean;
            } else {
                Object v = arg.valueType();
                if (v != null && !parameterType.isInstance(v) && !isAssignablePrimitive(parameterType, v)) return null;
                out[i] = v;
            }
        }
        return out;
    }

    private String resolveBeanNameByType(Class<?> paramType, Class<?> requestedType) {
        Class<?> target = (requestedType != null) ? requestedType : paramType;
        List<BeanDefinition> matches = definitions.values().stream()
                .filter(d -> target.isAssignableFrom(d.beanClass()))
                .toList();
        if (matches.isEmpty())
            throw new IllegalStateException("No bean for type " + target.getName());
        if (matches.size() > 1)
            throw new IllegalStateException("Multiple beans for type " + target.getName() + ": " +
                    matches.stream().map(BeanDefinition::name).toList());
        return matches.get(0).name();
    }


    private boolean isAssignablePrimitive(Class<?> parameterType, Object value) {
        if (!parameterType.isPrimitive()) return false;
        return switch (parameterType.getName()) {
            case "int"     -> value instanceof Integer;
            case "long"    -> value instanceof Long;
            case "double"  -> value instanceof Double;
            case "float"   -> value instanceof Float;
            case "boolean" -> value instanceof Boolean;
            case "char"    -> value instanceof Character;
            case "byte"    -> value instanceof Byte;
            case "short"   -> value instanceof Short;
            default -> false;
        };
    }
}
