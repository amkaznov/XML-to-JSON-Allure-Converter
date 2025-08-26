package com.example.xmltoallure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

/**
 * Главный класс приложения для конвертации XML в Allure JSON.
 */
@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "XML to Allure Converter API", version = "1.0", description = "API for converting XML test cases to Allure JSON format"))
public class XmlToAllureApplication {

    /**
     * Точка входа в приложение.
     * @param args Аргументы командной строки.
     */
    public static void main(String[] args) {
        SpringApplication.run(XmlToAllureApplication.class, args);
    }

}
