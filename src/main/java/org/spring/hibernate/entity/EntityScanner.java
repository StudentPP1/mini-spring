package org.spring.hibernate.entity;

import org.spring.hibernate.annotation.Column;
import org.spring.hibernate.annotation.Entity;
import org.spring.hibernate.annotation.Id;
import org.spring.hibernate.annotation.Table;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class EntityScanner {
    private EntityScanner() {}

    public static Map<Class<?>, EntityMetadata> scan(String basePackage) throws IOException, ClassNotFoundException {
        return findClassesIn(basePackage).stream()
                .filter(EntityScanner::isEntity)
                .collect(Collectors.toMap(Function.identity(), EntityScanner::getMetadata));
    }

    private static List<Class<?>> findClassesIn(String basePackage) throws IOException, ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        String path = basePackage.replace(".", "/");
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> directories = loader.getResources(path);
        while (directories.hasMoreElements()) {
            URL pathToDirectory = directories.nextElement();
            File directory = new File(pathToDirectory.getFile());
            for (File file : Objects.requireNonNull(directory.listFiles())) {
                if (file.isDirectory()) {
                    classes.addAll(findClassesIn("%s.%s".formatted(basePackage, file.getName())));
                } else if (file.isFile()) {
                    String className = "%s.%s".formatted(basePackage, file.getName().replace(".class", ""));
                    classes.add(Class.forName(className));
                }
            }
        }
        return classes;
    }

    private static boolean isEntity(Class<?> element) {
        return element.isAnnotationPresent(Entity.class);
    }

    private static EntityMetadata getMetadata(Class<?> element) {
        String tableName = resolveTableName(element);
        String idField = null;
        String idColumn = null;
        Map<String, Field> columns = new LinkedHashMap<>();

        for (Field field : element.getDeclaredFields()) {
            String fieldName = field.getName();
            String columnName = Character.toLowerCase(fieldName.charAt(0)) + fieldName.substring(1);
            Column columnAnnotation = field.getAnnotation(Column.class);
            if (columnAnnotation != null && columnAnnotation.name() != null) {
                columnName = columnAnnotation.name();
            }
            Id idAnnotation = field.getAnnotation(Id.class);
            if (idAnnotation != null && idField != null) {
                throw new IllegalArgumentException("entity must have only one @Id annotation");
            }
            else if (idAnnotation != null) {
                idField = field.getName();
                idColumn = columnName;
            }
            else {
                columns.put(columnName, field);
            }
        }
        return new EntityMetadata(tableName, idField, idColumn, columns);
    }

    private static String resolveTableName(Class<?> element) {
        Table tableAnnotation = element.getAnnotation(Table.class);
        if (tableAnnotation != null && tableAnnotation.name() != null) {
            return tableAnnotation.name();
        }
        else {
            String simpleClassName = element.getSimpleName();
            return Character.toLowerCase(simpleClassName.charAt(0)) + simpleClassName.substring(1);
        }
    }
}
