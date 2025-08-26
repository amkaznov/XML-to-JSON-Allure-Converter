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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/v1/convert")
@Tag(name = "XML to Allure ZIP Converter")
public class ConversionController {

    private final ConversionService conversionService;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    @Autowired
    public ConversionController(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Operation(
            summary = "Конвертирует XML файл в ZIP-архив с Allure JSON результатами",
            description = "Принимает XML файл с одним или несколькими тест-кейсами и возвращает ZIP-архив для импорта в Allure TestOps"
    )
    @PostMapping(value = "/xml-to-allure-zip", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = "application/zip")
    public ResponseEntity<byte[]> convertXmlToAllureZip(
            @Parameter(description = "XML файл для конвертации") @RequestPart("file") MultipartFile file,
            @Parameter(description = "Epic для Allure отчета") @RequestParam(required = false) String epic,
            @Parameter(description = "Feature для Allure отчета") @RequestParam(required = false) String feature,
            @Parameter(description = "Story для Allure отчета (если не указано, используется имя файла)") @RequestParam(required = false) String story) {
        try {
            String xmlContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            String originalFileName = file.getOriginalFilename();
            List<TestCase> testCases = conversionService.convert(xmlContent, originalFileName, epic, feature, story);

            byte[] zipBytes = createZipArchive(testCases);

            String outputFileName = "allure-results.zip";
            if (originalFileName != null && !originalFileName.isEmpty()) {
                outputFileName = originalFileName.replaceFirst("[.][^.]+$", "") + "-result.zip";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/zip"));
            headers.setContentDispositionFormData("attachment", outputFileName);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(zipBytes);

        } catch (Exception e) {
            e.printStackTrace();
            String errorJson = "{\"error\":\"Error during conversion: " + e.getMessage() + "\"}";
            return ResponseEntity.status(500).contentType(MediaType.APPLICATION_JSON).body(errorJson.getBytes(StandardCharsets.UTF_8));
        }
    }

    private byte[] createZipArchive(List<TestCase> testCases) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            Map<String, Integer> fileNameCounts = new HashMap<>();
            for (TestCase testCase : testCases) {
                String baseName = testCase.getName().replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
                
                int count = fileNameCounts.getOrDefault(baseName, 0);
                String finalName = (count == 0) ? baseName : baseName + "-" + count;
                fileNameCounts.put(baseName, count + 1);

                String entryName = finalName + "-result.json";
                
                ZipEntry entry = new ZipEntry(entryName);
                zos.putNextEntry(entry);
                
                String json = gson.toJson(testCase);
                zos.write(json.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }
}