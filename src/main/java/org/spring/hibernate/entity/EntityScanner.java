package org.spring.hibernate.entity;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spring.hibernate.annotation.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class EntityScanner {
    private static final Logger log = LogManager.getLogger(EntityScanner.class);

    private EntityScanner() {
    }

    public static Map<Class<?>, EntityMetadata> scan(String basePackage) throws IOException, ClassNotFoundException {
        return findClassesIn(basePackage).stream()
                .filter(EntityScanner::isEntity)
                .collect(Collectors.toMap(Function.identity(), EntityScanner::getMetadata));
    }

    private static List<Class<?>> findClassesIn(String basePackage) throws IOException, ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        String path = basePackage.replace(".", "/");
        log.trace("find entities in path: {}", path);
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
        List<SimpleField> simpleFields = new ArrayList<>();
        List<RelationField> relationFields = new ArrayList<>();
        int eagerBagCount = 0;
        for (Field field : element.getDeclaredFields()) {
            String columnName = resolveColumnName(field);
            if (field.isAnnotationPresent(Id.class)) {
                if (idField != null) {
                    throw new IllegalArgumentException("entity must have only one @Id annotation");
                }
                idField = field.getName();
                idColumn = columnName;
            }
            RelationData relationData = extractRelation(field);
            if (relationData == null) {
                simpleFields.add(new SimpleField(
                        columnName,
                        field
                ));
            } else {
                relationFields.add(new RelationField(
                        columnName,
                        field,
                        relationData.foreignKey(),
                        relationData.fetchType(),
                        relationData.mappedBy(),
                        relationData.isRelationOwner()
                ));
            }
            eagerBagCount = validateBagCount(element, field, relationData, eagerBagCount);
        }
        log.trace("get metadata from entity: {}", element.getSimpleName());
        return new EntityMetadata(tableName, idField, idColumn, relationFields, simpleFields);
    }

    private static int validateBagCount(Class<?> element, Field field, RelationData relationData, int eagerBagCount) {
        if (relationData != null
                && !relationData.isRelationOwner()
                && relationData.fetchType() == FetchType.EAGER
                && List.class.isAssignableFrom(field.getType())) {
            eagerBagCount++;
            if (eagerBagCount > 1) {
                throw new RuntimeException(
                        "MultipleBagFetchException: cannot simultaneously fetch multiple bags: " + element.getName() +
                                ". Use Set instead of List, or change fetch type to LAZY."
                );
            }
        }
        return eagerBagCount;
    }

    private static String resolveColumnName(Field field) {
        Column columnAnnotation = field.getAnnotation(Column.class);
        if (columnAnnotation != null && columnAnnotation.name() != null) {
            return columnAnnotation.name();
        }
        String fieldName = field.getName();
        return Character.toLowerCase(fieldName.charAt(0)) + fieldName.substring(1);
    }

    private static RelationData extractRelation(Field field) {
        boolean isOneToMany = field.isAnnotationPresent(OneToMany.class);
        boolean isManyToOne = field.isAnnotationPresent(ManyToOne.class);
        boolean isJoinColumn = field.isAnnotationPresent(JoinColumn.class);
        String className = field.getDeclaringClass().getSimpleName();
        String propertyName = className + "." + field.getName();
        validateRelationAnnotations(isOneToMany, isManyToOne, propertyName, isJoinColumn);
        String foreignKey = null;
        FetchType fetchType = null;
        String mappedBy = null;
        Boolean isRelationOwner = null;
        // child (Person)
        if (isOneToMany) {
            OneToMany oneToMany = field.getAnnotation(OneToMany.class);
            fetchType = oneToMany.type();
            mappedBy = oneToMany.mappedBy();
            if (mappedBy == null || mappedBy.isEmpty()) {
                throw new IllegalArgumentException("Field '" + field.getName() + "': @OneToMany must have a valid mappedBy attribute!");
            }
            isRelationOwner = false;
        }
        // parent (Note)
        if (isManyToOne) {
            // TODO: FetchType.LAZY for not collection field
            fetchType = FetchType.EAGER;
            isRelationOwner = true;
            if (isJoinColumn) {
                JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
                foreignKey = joinColumn.name();
            } else {
                foreignKey = field.getName() + "_id";
            }
        }
        return fetchType != null ? new RelationData(foreignKey, fetchType, mappedBy, isRelationOwner) : null;
    }

    private static void validateRelationAnnotations(boolean isOneToMany, boolean isManyToOne, String propertyName, boolean isJoinColumn) {
        if (isOneToMany && isManyToOne) {
            throw new RuntimeException(
                    "Property '" + propertyName + "' has both @OneToMany and @ManyToOne annotations. " +
                            "A property can only have one association type."
            );
        }
        if (isJoinColumn && isOneToMany) {
            throw new RuntimeException(
                    "Illegal attempt to map a @JoinColumn on a @OneToMany association: '" + propertyName + "'. " +
                            "Use the 'mappedBy' attribute instead."
            );
        }
        if (isJoinColumn && !isManyToOne) {
            throw new RuntimeException(
                    "@JoinColumn was found on a non-entity property: " + propertyName
            );
        }
    }

    private static String resolveTableName(Class<?> element) {
        Table tableAnnotation = element.getAnnotation(Table.class);
        if (tableAnnotation != null && tableAnnotation.name() != null) {
            return tableAnnotation.name();
        } else {
            String simpleClassName = element.getSimpleName();
            return Character.toLowerCase(simpleClassName.charAt(0)) + simpleClassName.substring(1);
        }
    }
}
