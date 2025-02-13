package org.tastech.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchParameter {
    private List<String> fields;

    private String value;

    private boolean ignoreCase = true;

    public SearchParameter(List<String> fields, String value) {
        this.fields = fields;
        this.value = value;
    }
}
