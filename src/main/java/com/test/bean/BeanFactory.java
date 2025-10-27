package com.test.bean;

import com.test.utils.PropertyResolver;
import com.test.utils.SimpleTypeConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public final class BeanFactory {
    private static final Logger log = LogManager.getLogger(BeanFactory.class);
    private final Map<String, BeanDefinition> definitions = new HashMap<>();
    private final Map<String, Object> singletons = new HashMap<>();
    private final ThreadLocal<Set<String>> creating = ThreadLocal.withInitial(HashSet::new);
    private final List<BeanPostProcessor> postProcessors = new ArrayList<>();

    public BeanFactory(List<BeanDefinition> definitions) {
        definitions.forEach(definition ->
                this.definitions.putIfAbsent(definition.name(), definition)
        );
    }

    public void addPostProcessor(BeanPostProcessor postProcessor) {
        this.postProcessors.add(postProcessor);
    }

    public void preInstanceSingletons() {
        log.trace("create instance for singletons");
        for (BeanDefinition definition : this.definitions.values()) {
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
        log.trace("start creating bean with name: {}", name);
        if (!creating.get().add(name))
            throw new IllegalStateException("Circular dependency at '" + name + "'");
        try {
            Object bean = (definition.factory() != null)
                    ? definition.factory().get()
                    : instantiateViaConstructor(definition);
            log.trace("start post process before initialization: {}", name);
            for (BeanPostProcessor p : postProcessors) {
                bean = p.postProcessBeforeInitialization(bean, name);
            }
            log.trace("skip finding init methods");
            log.trace("start post process after initialization: {}", name);
            for (BeanPostProcessor p : postProcessors) {
                bean = p.postProcessAfterInitialization(bean, name);
            }

            return bean;
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
            log.trace("resolve args for creating bean: {}", classBean);
            Object[] args = tryResolveArgs(constructor.getParameterTypes(), definition.constructorArgs());
            if (args != null) {
                try {
                    constructor.setAccessible(true);
                    log.trace("create instance of bean: {}", classBean);
                    return constructor.newInstance(args);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException("Failed to instantiate " + classBean.getName(), e);
                }
            }
        }
        try {
            var noArg = classBean.getDeclaredConstructor();
            noArg.setAccessible(true);
            log.trace("create instance with no args of bean: {}", classBean);
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
            if (arg.type().equals(ConstructorArg.Type.BEAN)) {
                String beanName = resolveBeanNameByType(parameterType, arg.beanType());
                log.trace("inject bean: {}", beanName);
                out[i] = getBean(beanName);
            } else {
                String raw = PropertyResolver.resolve(arg.expression());
                try {
                    out[i] = SimpleTypeConverter.convert(raw, parameterType);
                    log.trace("inject @Value: {}", out[i]);
                } catch (Exception e) {
                    throw new IllegalStateException("Cannot convert value '" + raw +
                            "' to " + parameterType.getName(), e);
                }
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

    public Collection<Object> getAllBeans() {
        return Collections.unmodifiableCollection(this.singletons.values());
    }

    public <T> Map<String, T> getBeanByType(Class<T> type) {
        Map<String, T> result = new HashMap<>();
        for (Map.Entry<String, BeanDefinition> entry : definitions.entrySet()) {
            String name = entry.getKey();
            BeanDefinition definition = entry.getValue();
            Class<?> beanType = definition.beanClass();
            if (beanType.isAssignableFrom(type) || Arrays.asList(beanType.getInterfaces()).contains(type)) {
                T bean = (T) getBean(name);
                result.put(name, bean);
            }
        }
        return result;
    }
}
