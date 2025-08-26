package com.example.xmltoallure.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Модель данных для шага тест-кейса.
 */
@Data
@Builder
public class TestStep {

    /**
     * Имя шага.
     */
    private String name;
    /**
     * Статус шага.
     */
    private String status;
    /**
     * Список вложенных шагов.
     */
    private List<TestStep> steps;
    /**
     * Список параметров.
     */
    private List<Parameter> parameters;

}
