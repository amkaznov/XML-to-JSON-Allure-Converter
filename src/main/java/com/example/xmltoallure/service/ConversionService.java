package com.example.xmltoallure.service;

import com.example.xmltoallure.model.Labels;
import com.example.xmltoallure.model.Parameter;
import com.example.xmltoallure.model.TestCase;
import com.example.xmltoallure.model.TestStep;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ConversionService {

    /**
 * Сервис для конвертации XML в объекты TestCase.
 */
@Service
public class ConversionService {

    /**
     * Конвертирует содержимое XML в список объектов TestCase.
     * @param xmlContent Содержимое XML файла.
     * @param fileName Имя файла.
     * @param epic Epic для Allure отчета.
     * @param feature Feature для Allure отчета.
     * @param story Story для Allure отчета.
     * @return Список объектов TestCase.
     * @throws Exception Если произошла ошибка при парсинге XML.
     */
    public List<TestCase> convert(String xmlContent, String fileName, String epic, String feature, String story) throws Exception {
        Document doc = loadXMLFromString(xmlContent);
        doc.getDocumentElement().normalize();

        List<TestCase> testCases = new ArrayList<>();
        NodeList testCaseNodes = doc.getElementsByTagName("test-case");

        for (int i = 0; i < testCaseNodes.getLength(); i++) {
            Node testCaseNode = testCaseNodes.item(i);
            if (testCaseNode.getNodeType() == Node.ELEMENT_NODE) {
                Element testCaseElement = (Element) testCaseNode;
                testCases.add(parseTestCase(testCaseElement, fileName, epic, feature, story));
            }
        }
        return testCases;
    }

    /**
     * Парсит элемент test-case и создает объект TestCase.
     * @param testCaseElement Элемент test-case.
     * @param fileName Имя файла.
     * @param epic Epic для Allure отчета.
     * @param feature Feature для Allure отчета.
     * @param story Story для Allure отчета.
     * @return Объект TestCase.
     */
    private TestCase parseTestCase(Element testCaseElement, String fileName, String epic, String feature, String story) {
        String testCaseName = testCaseElement.getAttribute("id");

        // --- Pass 1: Analyze dateTime structure for this specific test case ---
        int dateTimeCount = 0;
        int firstDateTimeIndex = -1;
        int firstRequestIndex = -1;
        NodeList allNodes = testCaseElement.getChildNodes();
        for (int i = 0; i < allNodes.getLength(); i++) {
            Node node = allNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                String tagName = ((Element) node).getTagName();
                if ("dateTime".equals(tagName)) {
                    dateTimeCount++;
                    if (firstDateTimeIndex == -1) {
                        firstDateTimeIndex = i;
                    }
                }
                if (firstRequestIndex == -1 && ("request".equals(tagName) || "q".equals(tagName) || "event".equals(tagName))) {
                    firstRequestIndex = i;
                }
            }
        }

        boolean singleDateTimeAtStart = dateTimeCount == 1 && (firstRequestIndex == -1 || firstDateTimeIndex < firstRequestIndex);
        String description = "";
        if (singleDateTimeAtStart) {
            description = "Установить дату и время\n" + ((Element) allNodes.item(firstDateTimeIndex)).getTextContent().trim();
        }

        // --- Pass 2: Build TestCase ---
        List<Labels> labels = createLabels(fileName, epic, feature, story);
        List<TestStep> regularSteps = new ArrayList<>();
        List<TestStep> mockSubSteps = new ArrayList<>();
        String pendingDateTime = null;
        String pendingRequestData = null;

        for (int i = 0; i < allNodes.getLength(); i++) {
            Node node = allNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                String tagName = element.getTagName();

                if ("dateTime".equals(tagName)) {
                    if (!singleDateTimeAtStart) {
                        pendingDateTime = element.getTextContent().trim();
                    }
                    continue;
                }
                
                if ("requestData".equals(tagName)) {
                    pendingRequestData = element.getTextContent().trim();
                    continue;
                }

                if ("mockData".equals(tagName)) {
                    mockSubSteps.add(parseSingleMockSubStep(element));
                } else if ("request".equals(tagName)) {
                    i = parseRequestStep(regularSteps, allNodes, i, pendingDateTime, null);
                    pendingDateTime = null;
                    pendingRequestData = null;
                } else if ("q".equals(tagName)) {
                    i = parseQStep(regularSteps, allNodes, i, pendingDateTime, pendingRequestData);
                    pendingDateTime = null;
                    pendingRequestData = null;
                } else if ("event".equals(tagName)) {
                    i = parseEventStep(regularSteps, allNodes, i, pendingDateTime, pendingRequestData);
                    pendingDateTime = null;
                    pendingRequestData = null;
                }
            }
        }

        List<TestStep> finalSteps = new ArrayList<>();
        if (!mockSubSteps.isEmpty()) {
            finalSteps.add(TestStep.builder()
                .name("Создать моки")
                .status("passed")
                .steps(mockSubSteps)
                .build());
        }
        finalSteps.addAll(regularSteps);

        return TestCase.builder()
                .name(testCaseName)
                .fullName(testCaseName)
                .description(description)
                .status("passed")
                .labels(labels)
                .steps(finalSteps)
                .build();
    }

    // ... (rest of the methods remain unchanged)
}

    private TestCase parseTestCase(Element testCaseElement, String fileName, String epic, String feature, String story) {
        String testCaseName = testCaseElement.getAttribute("id");

        // --- Pass 1: Analyze dateTime structure for this specific test case ---
        int dateTimeCount = 0;
        int firstDateTimeIndex = -1;
        int firstRequestIndex = -1;
        NodeList allNodes = testCaseElement.getChildNodes();
        for (int i = 0; i < allNodes.getLength(); i++) {
            Node node = allNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                String tagName = ((Element) node).getTagName();
                if ("dateTime".equals(tagName)) {
                    dateTimeCount++;
                    if (firstDateTimeIndex == -1) {
                        firstDateTimeIndex = i;
                    }
                }
                if (firstRequestIndex == -1 && ("request".equals(tagName) || "q".equals(tagName) || "event".equals(tagName))) {
                    firstRequestIndex = i;
                }
            }
        }

        boolean singleDateTimeAtStart = dateTimeCount == 1 && (firstRequestIndex == -1 || firstDateTimeIndex < firstRequestIndex);
        String description = "";
        if (singleDateTimeAtStart) {
            description = "Установить дату и время\n" + ((Element) allNodes.item(firstDateTimeIndex)).getTextContent().trim();
        }

        // --- Pass 2: Build TestCase ---
        List<Labels> labels = createLabels(fileName, epic, feature, story);
        List<TestStep> regularSteps = new ArrayList<>();
        List<TestStep> mockSubSteps = new ArrayList<>();
        String pendingDateTime = null;
        String pendingRequestData = null;

        for (int i = 0; i < allNodes.getLength(); i++) {
            Node node = allNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                String tagName = element.getTagName();

                if ("dateTime".equals(tagName)) {
                    if (!singleDateTimeAtStart) {
                        pendingDateTime = element.getTextContent().trim();
                    }
                    continue;
                }
                
                if ("requestData".equals(tagName)) {
                    pendingRequestData = element.getTextContent().trim();
                    continue;
                }

                if ("mockData".equals(tagName)) {
                    mockSubSteps.add(parseSingleMockSubStep(element));
                } else if ("request".equals(tagName)) {
                    i = parseRequestStep(regularSteps, allNodes, i, pendingDateTime, null);
                    pendingDateTime = null;
                    pendingRequestData = null;
                } else if ("q".equals(tagName)) {
                    i = parseQStep(regularSteps, allNodes, i, pendingDateTime, pendingRequestData);
                    pendingDateTime = null;
                    pendingRequestData = null;
                } else if ("event".equals(tagName)) {
                    i = parseEventStep(regularSteps, allNodes, i, pendingDateTime, pendingRequestData);
                    pendingDateTime = null;
                    pendingRequestData = null;
                }
            }
        }

        List<TestStep> finalSteps = new ArrayList<>();
        if (!mockSubSteps.isEmpty()) {
            finalSteps.add(TestStep.builder()
                .name("Создать моки")
                .status("passed")
                .steps(mockSubSteps)
                .build());
        }
        finalSteps.addAll(regularSteps);

        return TestCase.builder()
                .name(testCaseName)
                .fullName(testCaseName)
                .description(description)
                .status("passed")
                .labels(labels)
                .steps(finalSteps)
                .build();
    }

    /**
     * Создает список меток для Allure отчета.
     * @param fileName Имя файла.
     * @param epic Epic для Allure отчета.
     * @param feature Feature для Allure отчета.
     * @param story Story для Allure отчета.
     * @return Список меток.
     */
    private List<Labels> createLabels(String fileName, String epic, String feature, String story) {
        List<Labels> labels = new ArrayList<>();
        if (epic != null && !epic.isEmpty()) {
            labels.add(Labels.builder().name("epic").value(epic).build());
        }
        if (feature != null && !feature.isEmpty()) {
            labels.add(Labels.builder().name("feature").value(feature).build());
        }
        if (story != null && !story.isEmpty()) {
            labels.add(Labels.builder().name("story").value(story).build());
        } else if (fileName != null && !fileName.isEmpty()) {
            labels.add(Labels.builder().name("story").value(fileName.replaceFirst("[.][^.]+$", "")).build());
        }
        return labels;
    }

    /**
     * Загружает XML из строки.
     * @param xml Строка с XML.
     * @return Объект Document.
     * @throws Exception Если произошла ошибка при парсинге.
     */
    private Document loadXMLFromString(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        return builder.parse(is);
    }

    /**
     * Парсит один подшаг мока.
     * @param mockDataElement Элемент mockData.
     * @return Объект TestStep.
     */
    private TestStep parseSingleMockSubStep(Element mockDataElement) {
        Element queryElement = (Element) mockDataElement.getElementsByTagName("query").item(0);
        Element responseElement = (Element) mockDataElement.getElementsByTagName("response").item(0);
        Element paramsElement = (Element) mockDataElement.getElementsByTagName("parameters").item(0);

        String urlTemplate = queryElement.getTextContent().trim();
        String finalUrl = replaceParameters(urlTemplate, paramsElement);
        String methodName = getMethodNameFromUrl(finalUrl);

        List<Parameter> parameters = new ArrayList<>();
        parameters.add(Parameter.builder().name("Method").value(queryElement.getAttribute("method").toUpperCase()).build());
        parameters.add(Parameter.builder().name("URL").value(finalUrl).build());
        parameters.add(Parameter.builder().name("Status").value(responseElement.getAttribute("status")).build());
        parameters.add(Parameter.builder().name("Body").value(StringEscapeUtils.escapeJson(responseElement.getTextContent().trim())).build());

        return TestStep.builder()
                .name(methodName)
                .status("passed")
                .parameters(parameters)
                .build();
    }

    /**
     * Парсит шаг запроса.
     * @param mainSteps Список основных шагов.
     * @param nodes Список узлов.
     * @param currentIndex Текущий индекс.
     * @param pendingDateTime Ожидаемая дата и время.
     * @param pendingRequestData Ожидаемые данные запроса.
     * @return Следующий индекс.
     */
    private int parseRequestStep(List<TestStep> mainSteps, NodeList nodes, int currentIndex, String pendingDateTime, String pendingRequestData) {
        return parseGenericStep(mainSteps, nodes, currentIndex, pendingDateTime, pendingRequestData, body -> "Отправить запрос:", true);
    }

    /**
     * Парсит шаг "q".
     * @param mainSteps Список основных шагов.
     * @param nodes Список узлов.
     * @param currentIndex Текущий индекс.
     * @param pendingDateTime Ожидаемая дата и время.
     * @param pendingRequestData Ожидаемые данные запроса.
     * @return Следующий индекс.
     */
    private int parseQStep(List<TestStep> mainSteps, NodeList nodes, int currentIndex, String pendingDateTime, String pendingRequestData) {
        return parseGenericStep(mainSteps, nodes, currentIndex, pendingDateTime, pendingRequestData, body -> "Отправить текст в бота:\n" + body, false);
    }

    /**
     * Парсит шаг события.
     * @param mainSteps Список основных шагов.
     * @param nodes Список узлов.
     * @param currentIndex Текущий индекс.
     * @param pendingDateTime Ожидаемая дата и время.
     * @param pendingRequestData Ожидаемые данные запроса.
     * @return Следующий индекс.
     */
    private int parseEventStep(List<TestStep> mainSteps, NodeList nodes, int currentIndex, String pendingDateTime, String pendingRequestData) {
        return parseGenericStep(mainSteps, nodes, currentIndex, pendingDateTime, pendingRequestData, body -> "Вызвать ивент:\n" + body, false);
    }

    /**
     * Парсит общий шаг.
     * @param mainSteps Список основных шагов.
     * @param nodes Список узлов.
     * @param currentIndex Текущий индекс.
     * @param pendingDateTime Ожидаемая дата и время.
     * @param pendingRequestData Ожидаемые данные запроса.
     * @param stepNameFormatter Форматтер имени шага.
     * @param isRequest Является ли шаг запросом.
     * @return Следующий индекс.
     */
    private int parseGenericStep(List<TestStep> mainSteps, NodeList nodes, int currentIndex, String pendingDateTime, String pendingRequestData, Function<String, String> stepNameFormatter, boolean isRequest) {
        Element element = (Element) nodes.item(currentIndex);
        String body = element.getTextContent().trim();

        List<TestStep> subSteps = new ArrayList<>();
        List<Parameter> mainParameters = new ArrayList<>();

        if (pendingDateTime != null) {
            subSteps.add(TestStep.builder().name("Перед шагом установить дату и время\n" + pendingDateTime).status("passed").build());
            mainParameters.add(Parameter.builder().name("DateTime").value(pendingDateTime).build());
        }
        
        if (pendingRequestData != null) {
            subSteps.add(TestStep.builder().name("Установить значение\n" + pendingRequestData).status("passed").build());
            mainParameters.add(Parameter.builder().name("RequestData").value(pendingRequestData).build());
        }

        if (isRequest) {
            mainParameters.add(Parameter.builder().name("Body").value(body).build());
            TestStep bodySubStep = TestStep.builder().name(body).status("passed").build();
            TestStep requestBodyStep = TestStep.builder().name("Тело запроса:").status("passed").steps(List.of(bodySubStep)).build();
            subSteps.add(requestBodyStep);
        }

        List<TestStep> expectedResultSteps = new ArrayList<>();
        int nextIndex = collectExpectedResults(nodes, currentIndex + 1, expectedResultSteps);

        if (!expectedResultSteps.isEmpty()) {
            TestStep expectedResultStep = TestStep.builder().name("Expected Result").steps(expectedResultSteps).build();
            subSteps.add(expectedResultStep);
        }

        TestStep mainStep = TestStep.builder()
                .name(stepNameFormatter.apply(body))
                .status("passed")
                .steps(subSteps)
                .parameters(mainParameters)
                .build();

        mainSteps.add(mainStep);
        return nextIndex - 1;
    }

    /**
     * Собирает ожидаемые результаты.
     * @param nodes Список узлов.
     * @param startIndex Начальный индекс.
     * @param expectedResultSteps Список шагов с ожидаемыми результатами.
     * @return Следующий индекс.
     */
    private int collectExpectedResults(NodeList nodes, int startIndex, List<TestStep> expectedResultSteps) {
        for (int i = startIndex; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                if ("a".equals(element.getTagName())) {
                    String state = element.getAttribute("state");
                    String text = element.getTextContent().trim();
                    TestStep aStep = TestStep.builder().name("state = '" + state + "' " + text).build();
                    expectedResultSteps.add(aStep);
                } else if ("responseData".equals(element.getTagName())) {
                    String field = element.getAttribute("field");
                    String text = element.getTextContent().trim();
                    String stepName;

                    if ("replies".equals(field)) {
                        stepName = "Ожидаемое тело:\n" + text;
                    } else if (text.isEmpty()) {
                        stepName = "Ключ " + field + " не равен NULL/существует в ответе";
                    } else {
                        stepName = "Элемент тела\n" + field + "\nимеет значение\n" + text;
                    }
                    
                    TestStep responseDataStep = TestStep.builder().name(stepName).build();
                    expectedResultSteps.add(responseDataStep);
                } else {
                    return i;
                }
            }
        }
        return nodes.getLength();
    }

    /**
     * Заменяет параметры в URL.
     * @param urlTemplate Шаблон URL.
     * @param paramsElement Элемент с параметрами.
     * @return URL с замененными параметрами.
     */
    private String replaceParameters(String urlTemplate, Element paramsElement) {
        if (paramsElement == null) return urlTemplate;
        String result = urlTemplate;
        NodeList paramNodes = paramsElement.getChildNodes();
        for (int i = 0; i < paramNodes.getLength(); i++) {
            Node node = paramNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element param = (Element) node;
                String key = param.getTagName();
                String value = param.getTextContent().trim();
                result = result.replace("${" + key + "}", value);
            }
        }
        return result;
    }

    /**
     * Извлекает имя метода из URL.
     * @param url URL.
     * @return Имя метода.
     */
    private String getMethodNameFromUrl(String url) {
        try {
            Pattern pattern = Pattern.compile(".*/(.*?)(?:\\?.*)?$");
            Matcher matcher = pattern.matcher(url);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            // ignore
        }
        return "unknown_method";
    }
}