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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Контроллер для обработки запросов на конвертацию XML в Allure JSON.
 */
@RestController
@RequestMapping("/api/v1/convert")
@Tag(name = "XML to Allure ZIP Converter")
public class ConversionController {

    private final ConversionService conversionService;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /**
     * Конструктор для внедрения зависимости ConversionService.
     * @param conversionService Сервис для конвертации.
     */
    @Autowired
    public ConversionController(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    /**
     * Конвертирует XML файлы или ZIP-архивы в один ZIP-архив с Allure JSON результатами.
     * @param files Список файлов (XML и/или ZIP) для конвертации.
     * @param epic Epic для Allure отчета.
     * @param feature Feature для Allure отчета.
     * @param story Story для Allure отчета.
     * @return ResponseEntity с ZIP-архивом или сообщением об ошибке.
     */
    @Operation(
            summary = "Конвертирует XML файлы или ZIP-архивы в один ZIP-архив с Allure JSON результатами",
            description = "Принимает один или несколько XML файлов и/или ZIP-архивов с XML файлами и возвращает один общий ZIP-архив для импорта в Allure TestOps"
    )
    @PostMapping(value = "/xml-to-allure-zip", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = "application/zip")
    public ResponseEntity<byte[]> convertXmlToAllureZip(
            @Parameter(description = "Один или несколько XML файлов и/или ZIP-архивов для конвертации") @RequestPart("files") List<MultipartFile> files,
            @Parameter(description = "Epic для Allure отчета") @RequestParam(required = false) String epic,
            @Parameter(description = "Feature для Allure отчета") @RequestParam(required = false) String feature,
            @Parameter(description = "Story для Allure отчета (если не указано, используется имя файла)") @RequestParam(required = false) String story) {
        
        List<TestCase> allTestCases = new ArrayList<>();

        try {
            for (MultipartFile file : files) {
                String originalFileName = file.getOriginalFilename();
                if (originalFileName != null && originalFileName.toLowerCase().endsWith(".zip")) {
                    processZipFile(file, allTestCases, epic, feature, story);
                } else if (originalFileName != null && originalFileName.toLowerCase().endsWith(".xml")) {
                    processXmlFile(file, allTestCases, epic, feature, story);
                }
            }

            byte[] zipBytes = createZipArchive(allTestCases);
            String outputFileName = "allure-results.zip";

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

    private void processXmlFile(MultipartFile file, List<TestCase> allTestCases, String epic, String feature, String story) throws Exception {
        String xmlContent = new String(file.getBytes(), StandardCharsets.UTF_8);
        allTestCases.addAll(conversionService.convert(xmlContent, file.getOriginalFilename(), epic, feature, story));
    }

    private void processZipFile(MultipartFile file, List<TestCase> allTestCases, String epic, String feature, String story) throws IOException {
        try (InputStream is = file.getInputStream(); ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                if (!zipEntry.isDirectory() && zipEntry.getName().toLowerCase().endsWith(".xml")) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        baos.write(buffer, 0, len);
                    }
                    String xmlContent = baos.toString(StandardCharsets.UTF_8.name());
                    try {
                        String fileNameOnly = new File(zipEntry.getName()).getName();
                        allTestCases.addAll(conversionService.convert(xmlContent, fileNameOnly, epic, feature, story));
                    } catch (Exception e) {
                        System.err.println("Failed to convert file in zip: " + zipEntry.getName() + " - " + e.getMessage());
                    }
                }
                zis.closeEntry();
            }
        }
    }

    /**
     * Создает ZIP-архив из списка тест-кейсов.
     * @param testCases Список тест-кейсов.
     * @return Массив байтов с ZIP-архивом.
     * @throws IOException Если произошла ошибка ввода-вывода.
     */
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