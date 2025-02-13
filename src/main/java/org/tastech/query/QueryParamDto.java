package org.tastech.query;

import lombok.*;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public class QueryParamDto {
    SearchParameter term;

    Parameter<Integer> deletedAt;
}
