package com.example.xmltoallure.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Модель данных для тест-кейса.
 */
@Data
@Builder
public class TestCase {

    /**
     * Имя тест-кейса.
     */
    private String name;
    /**
     * Полное имя тест-кейса.
     */
    private String fullName;
    /**
     * Описание тест-кейса.
     */
    private String description;
    /**
     * Список шагов тест-кейса.
     */
    private List<TestStep> steps;
    /**
     * Список меток.
     */
    private List<Labels> labels;
    /**
     * Статус тест-кейса.
     */
    private String status;

}
