package org.tastech.query;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.tastech.query.annotation.JoinDataImpl;
import org.tastech.query.operator.CoordinateOperator;
import org.tastech.query.operator.QueryOperator;

import java.util.List;

public class SearchAndQuerySpecification<T, P> extends QuerySpecification<T, P> {
    private final SearchParameter searchParameter;

    public SearchAndQuerySpecification(SearchParameter searchParameter, P parameter) {
        super(parameter);
        this.searchParameter = searchParameter;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        Specification<T> specification = buildSearchSpecification();
        if (specification != null) {
            return specification.toPredicate(root, query, criteriaBuilder);
        }
        return criteriaBuilder.conjunction();
    }

    protected Specification<T> buildSearchSpecification() {
        Specification<T> specification = buildSpecification();
        if (this.searchParameter != null && searchParameter.getValue() != null) {
            Specification<T> searchSpecification = null;
            List<String> fields = this.searchParameter.getFields();
            Parameter param = new Parameter();
            param.setValue(searchParameter.getValue());
            param.setCoordinateOperator(CoordinateOperator.LIKE);
            param.setQueryOperator(QueryOperator.OR);
            param.setIgnoreCase(searchParameter.isIgnoreCase());
            for (String field : fields) {
                JoinDataImpl joinData = null;
                if (field.contains(".")) {
                    try {
                        joinData = new JoinDataImpl();
                        joinData.setJoinField(field.substring(field.indexOf(".") + 1));
                        joinData.setTableName(field.substring(0, field.indexOf(".")));
                    } catch (Exception e) {
                        throw new RuntimeException(e.getMessage());
                    }
                }

                searchSpecification = param.updateSpecification(searchSpecification, field, joinData);
            }
            if (specification == null) {
                specification = searchSpecification;
            } else {
                if (searchSpecification != null) {
                    specification = specification.and(searchSpecification);
                }
            }
        }
        return specification;
    }
}
