package org.tastech.query;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.tastech.query.annotation.JoinData;
import org.tastech.query.annotation.QueryField;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class QuerySpecification<T, P> implements Specification<T> {
    private final P parameter;

    public QuerySpecification(P parameter) {
        this.parameter = parameter;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        Specification<T> specification = buildSpecification();
        if (specification != null) {
            return specification.toPredicate(root, query, criteriaBuilder);
        }
        return criteriaBuilder.conjunction();
    }

    protected Specification<T> buildSpecification() {
        Specification<T> specification = null;
        if (this.parameter != null) {
            Field[] fields = this.parameter.getClass().getDeclaredFields();
            for (Field field : fields) {
                try {
                    if (field.trySetAccessible()) {
                        if (field.get(this.parameter) instanceof Parameter param) {
                            Type valueType = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                            if (param.getValueType() == null && valueType instanceof Class<?> aClass) {
                                param.setValueType(aClass);
                            }

                            var fieldName = field.getName();
                            if(field.getAnnotation(QueryField.class) != null){
                                fieldName = field.getAnnotation(QueryField.class).getName();
                            }

                            specification = param.updateSpecification(specification, fieldName, field.getAnnotation(JoinData.class));
                        }
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e.getMessage());
                }
            }
        }
        return specification;
    }
}
