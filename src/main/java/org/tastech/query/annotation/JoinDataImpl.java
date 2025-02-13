package org.tastech.query.annotation;

import lombok.Data;

import java.lang.annotation.Annotation;

@Data
public class JoinDataImpl implements JoinData {
    private Class<?> entityClass;

    private String joinField;

    private String tableName;

    @Override
    public Class<?> entityClass() {
        return this.entityClass;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return null;
    }
}
