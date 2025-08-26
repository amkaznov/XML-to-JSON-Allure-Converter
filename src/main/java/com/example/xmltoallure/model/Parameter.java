package com.example.xmltoallure.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Parameter {
    private String name;
    private String value;
}
