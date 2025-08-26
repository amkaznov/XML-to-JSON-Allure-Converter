package com.example.xmltoallure.controller;

import com.example.xmltoallure.model.TestCase;
import com.example.xmltoallure.service.ConversionService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/convert")
@Tag(name = "XML to Allure JSON Converter")
public class ConversionController {

    private final ConversionService conversionService;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    @Autowired
    public ConversionController(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Operation(
            summary = "Конвертирует XML файл в Allure JSON формат",
            description = "Принимает XML файл и параметры для Allure меток, возвращает готовый JSON для скачивания"
    )
    @PostMapping(value = "/xml-to-allure", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> convertXmlToAllure(
            @Parameter(description = "XML файл для конвертации") @RequestPart("file") MultipartFile file,
            @Parameter(description = "Epic для Allure отчета") @RequestParam(required = false) String epic,
            @Parameter(description = "Feature для Allure отчета") @RequestParam(required = false) String feature,
            @Parameter(description = "Story для Allure отчета (если не указано, используется имя файла)") @RequestParam(required = false) String story) {
        try {
            String xmlContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            String originalFileName = file.getOriginalFilename();
            TestCase testCase = conversionService.convert(xmlContent, originalFileName, epic, feature, story);

            String json = gson.toJson(testCase);
            byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

            String outputFileName = "allure-result.json";
            if (originalFileName != null && !originalFileName.isEmpty()) {
                outputFileName = originalFileName.replaceFirst("[.][^.]+$", "") + ".json";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", outputFileName);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(jsonBytes);
        } catch (Exception e) {
            e.printStackTrace();
            // Return a JSON error response for better client-side handling
            String errorJson = "{\"error\":\"Error during conversion: " + e.getMessage() + "\"}";
            return ResponseEntity.status(500).contentType(MediaType.APPLICATION_JSON).body(errorJson.getBytes(StandardCharsets.UTF_8));
        }
    }
}
