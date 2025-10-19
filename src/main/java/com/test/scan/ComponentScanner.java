package com.test.scan;

import com.test.annotation.Component;
import com.test.bean.BeanDefinition;
import com.test.bean.ConstructorArg;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public final class ComponentScanner {
    private ComponentScanner() {
    }

    public static List<BeanDefinition> scan(String basePackage) throws IOException, ClassNotFoundException {
        List<Class<?>> componentClasses = findClassesIn(basePackage)
                .stream()
                .filter(ComponentScanner::isClass)
                .filter(c -> isComponent(c, new HashSet<>()))
                .toList();
        return componentClasses.stream()
                .map(component -> createBeanDefinition(component, componentClasses))
                .collect(Collectors.toList());
    }

    private static List<Class<?>> findClassesIn(String basePackage) throws IOException, ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        String basePackagePath = basePackage.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> directories = classLoader.getResources(basePackagePath);
        while (directories.hasMoreElements()) {
            URL elementPath = directories.nextElement();
            File directory = new File(elementPath.getFile());
            for (File file : Objects.requireNonNull(directory.listFiles())) {
                if (file.isDirectory()) {
                    classes.addAll(findClassesIn("%s.%s".formatted(basePackage, file.getName())));
                } else if (file.getName().endsWith(".class")) {
                    String className = "%s.%s".formatted(basePackage, file.getName().replace(".class", ""));
                    classes.add(Class.forName(className));
                }
            }
        }
        return classes;
    }

    private static boolean isComponent(Class<?> element, Set<Class<?>> seenAnnotations) {
        if (element.isAnnotationPresent(Component.class)) return true;
        Annotation[] annotations = element.getAnnotations();
        for (int i = 0, annotationsLength = annotations.length; i < annotationsLength; i++) {
            Annotation annotation = annotations[i];
            Class<? extends Annotation> annotationType = annotation.annotationType();
            // avoid cycle & system annotation
            if (!seenAnnotations.add(annotationType)) continue;
            if (annotationType.getPackageName().startsWith("java.lang")) continue;
            if (annotationType.isAnnotationPresent(Component.class)) return true;
            // recursive for sub annotation
            if (isComponent(annotationType, seenAnnotations)) return true;
        }
        return false;
    }

    private static boolean isClass(Class<?> element) {
        int modifiers = element.getModifiers();
        return !element.isInterface() && !Modifier.isAbstract(modifiers);
    }

    private static BeanDefinition createBeanDefinition(Class<?> component, List<Class<?>> componentClasses) {
        List<ConstructorArg> args = new ArrayList<>();
        Constructor<?> constructor = Arrays.stream(component.getDeclaredConstructors())
                .filter(AccessibleObject::trySetAccessible)
                .max(Comparator.comparingInt(Constructor::getParameterCount))
                .orElseThrow(() ->
                        new RuntimeException("not found constructor in component %s".formatted(component.getName()))
                );
        for (Parameter parameter : constructor.getParameters()) {
            Class<?> parameterType = parameter.getType();
            if (componentClasses.contains(parameterType)) {
                args.add(ConstructorArg.beanType(parameterType));
            }
            else {
                args.add(ConstructorArg.valueType(parameterType));
            }
        }
        return new BeanDefinition(component, resolveBeanName(component), args);
    }

    private static String resolveBeanName(Class<?> element) {
        Component component = element.getAnnotation(Component.class);
        if (component != null && !component.name().isEmpty()) return component.name();
        for (Annotation annotation : element.getAnnotations()) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (annotationType.isAnnotationPresent(Component.class)) {
                try {
                    var method = annotationType.getMethod("name");
                    Object value = method.invoke(annotation);
                    if (value instanceof String s && !s.isEmpty()) return s;
                } catch (Exception _) {
                    // name in child of Component annotation can be null
                }
            }
        }
        String simpleName = element.getSimpleName();
        if (simpleName.isEmpty()) return element.getName();
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }
}
