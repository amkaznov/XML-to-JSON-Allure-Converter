package com.example.xmltoallure.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TestStep {

    private String name;
    private String status;
    private List<TestStep> steps;
    private List<Parameter> parameters;

}
