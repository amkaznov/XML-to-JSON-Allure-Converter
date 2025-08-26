package com.example.xmltoallure.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TestCase {

    private String name;
    private String fullName;
    private String description;
    private List<TestStep> steps;
    private List<Labels> labels;
    private String status;

}
