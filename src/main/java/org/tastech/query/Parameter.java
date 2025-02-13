package org.tastech.query;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import lombok.*;
import org.springframework.data.jpa.domain.Specification;
import org.tastech.query.annotation.JoinData;
import org.tastech.query.operator.CoordinateOperator;
import org.tastech.query.operator.QueryOperator;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Parameter<T> {
    private QueryOperator queryOperator = QueryOperator.AND;

    private T value;

    private CoordinateOperator coordinateOperator = CoordinateOperator.EQUAL;

    private Class<?> valueType;

    private boolean ignoreCase = false;

    public Parameter(T value) {
        this.value = value;
    }

    public Parameter(CoordinateOperator coordinateOperator) {
        this.coordinateOperator = coordinateOperator;
    }

    @SuppressWarnings("all")
    private Specification<T> createSpecification(String field, JoinData joinData) {
        return (root, query, criteriaBuilder) -> {
            From<?, ?> expression = root;
            String tableField = field;
            if (joinData != null) {
                String tableName = joinData.getTableName();
                if (tableName.isEmpty()) {
                    tableName = Character.toLowerCase(joinData.entityClass().getSimpleName().charAt(0))
                            + joinData.entityClass().getSimpleName().substring(1);
                }

                expression = root.join(tableName);
                tableField = joinData.getJoinField();
            }
            Object compareValue = transformValue();
            return switch (coordinateOperator) {
                case EQUAL -> {
                    Expression<String> ex = isIgnoreCase() ? criteriaBuilder.lower(expression.get(tableField))
                            : expression.get(tableField);
                    yield criteriaBuilder.equal(ex, compareValue);
                }
                case NOT_EQUAL -> {
                    Expression<String> ex = isIgnoreCase() ? criteriaBuilder.lower(expression.get(tableField))
                            : expression.get(tableField);
                    yield criteriaBuilder.notEqual(ex, compareValue);
                }
                case GREATER_THAN -> criteriaBuilder.greaterThan(expression.get(tableField), (Comparable) compareValue);
                case GREATER_THAN_OR_EQUAL ->
                    criteriaBuilder.greaterThanOrEqualTo(expression.get(tableField), (Comparable) compareValue);
                case LESS_THAN -> criteriaBuilder.lessThan(expression.get(tableField), (Comparable) compareValue);
                case LESS_THAN_OR_EQUAL ->
                    criteriaBuilder.lessThanOrEqualTo(expression.get(tableField), (Comparable) compareValue);
                case LIKE -> {
                    Expression ex = isIgnoreCase() ? criteriaBuilder.lower(expression.get(tableField))
                            : expression.get(tableField);
                    yield criteriaBuilder.like(ex, "%" + compareValue + "%");
                }
                case IN -> {
                    Expression ex = isIgnoreCase() ? criteriaBuilder.lower(expression.get(tableField))
                            : expression.get(tableField);
                    yield criteriaBuilder.in(ex).value(compareValue);
                }
                case NOT_IN -> {
                    Expression ex = isIgnoreCase() ? criteriaBuilder.lower(expression.get(tableField))
                            : expression.get(tableField);
                    yield criteriaBuilder.in(ex).value(compareValue).not();
                }
                case BETWEEN -> {
                    if (compareValue instanceof List<?> data) {
                        Object fromValue = data.get(0);
                        Object toValue = data.get(1);

                        yield criteriaBuilder.between(expression.get(tableField), (Comparable) fromValue,
                                (Comparable) toValue);
                    }
                    throw new RuntimeException("Value is invalid");
                }
                case NULL -> criteriaBuilder.isNull(expression.get(tableField));
                case NOT_NULL -> criteriaBuilder.isNotNull(expression.get(tableField));
            };
        };

    }

    public Specification<T> updateSpecification(Specification<T> specification, String field, JoinData joinData) {
        var isEmptyValue = value == null || value.toString().isEmpty() || value.toString().equals("[]");
        var isNullQuery = coordinateOperator == CoordinateOperator.NULL
                || coordinateOperator == CoordinateOperator.NOT_NULL;

        if (isEmptyValue && !isNullQuery) {
            return specification;
        }

        if (specification == null) {
            specification = createSpecification(field, joinData);
        } else {
            switch (queryOperator) {
                case OR -> specification = specification.or(createSpecification(field, joinData));
                case AND -> specification = specification.and(createSpecification(field, joinData));
            }
        }
        return specification;
    }

    private Object transformValue() {
        if (coordinateOperator == CoordinateOperator.NULL || coordinateOperator == CoordinateOperator.NOT_NULL) {
            return null;
        }

        if (value.getClass().isArray()) {
            List<?> a = Arrays.asList((Object[]) value);
            return a.stream().map(this::converter).toList();
        } else {
            return converter(value);
        }
    }

    private Object converter(Object data) {
        try {
            if (valueType == Date.class && !(data instanceof Date)) {
                return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").parse(data.toString());
            }
            if (valueType == UUID.class && !(data instanceof UUID)) {
                return UUID.fromString(data.toString());
            }
            if (isIgnoreCase() && data instanceof String s) {
                return s.toLowerCase();
            }

            return data;
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

}
