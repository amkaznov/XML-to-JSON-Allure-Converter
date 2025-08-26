package com.example.xmltoallure.model;

import lombok.Builder;
import lombok.Data;

/**
 * Модель данных для метки.
 */
@Data
@Builder
public class Labels {
    /**
     * Имя метки.
     */
    private String name;
    /**
     * Значение метки.
     */
    private String value;
}
