package com.example.xmltoallure.model;

import lombok.Builder;
import lombok.Data;

/**
 * Модель данных для параметра.
 */
@Data
@Builder
public class Parameter {
    /**
     * Имя параметра.
     */
    private String name;
    /**
     * Значение параметра.
     */
    private String value;
}
