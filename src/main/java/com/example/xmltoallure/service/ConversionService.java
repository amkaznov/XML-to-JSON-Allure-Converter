package com.example.xmltoallure.service;

import com.example.xmltoallure.model.Labels;
import com.example.xmltoallure.model.Parameter;
import com.example.xmltoallure.model.TestCase;
import com.example.xmltoallure.model.TestStep;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.text.StringEscapeUtils;

@Service
public class ConversionService {

    public TestCase convert(String xmlContent, String fileName, String epic, String feature, String story) throws Exception {
        Document doc = loadXMLFromString(xmlContent);
        doc.getDocumentElement().normalize();

        Element testCaseElement = (Element) doc.getElementsByTagName("test-case").item(0);
        String testCaseName = testCaseElement.getAttribute("id");

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

        List<TestStep> steps = new ArrayList<>();
        NodeList childNodes = testCaseElement.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                if ("mockData".equals(element.getTagName())) {
                    steps.add(parseMockDataStep(element));
                } else if ("request".equals(element.getTagName())) {
                    i = parseRequestStep(steps, childNodes, i);
                } else if ("q".equals(element.getTagName())) {
                    i = parseQStep(steps, childNodes, i);
                }
            }
        }

        return TestCase.builder()
                .name(testCaseName)
                .fullName(testCaseName)
                .description("")
                .status("passed")
                .labels(labels)
                .steps(steps)
                .build();
    }

    private Document loadXMLFromString(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        return builder.parse(is);
    }

    private TestStep parseMockDataStep(Element mockDataElement) {
        List<TestStep> mockSubSteps = new ArrayList<>();
        NodeList mockNodes = mockDataElement.getChildNodes();
        
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

        TestStep mockStep = TestStep.builder()
                .name(methodName)
                .status("passed")
                .parameters(parameters)
                .build();
        mockSubSteps.add(mockStep);

        return TestStep.builder()
                .name("Создать моки")
                .status("passed")
                .steps(mockSubSteps)
                .build();
    }

    private int parseRequestStep(List<TestStep> mainSteps, NodeList nodes, int currentIndex) {
        Element requestElement = (Element) nodes.item(currentIndex);
        String requestBody = requestElement.getTextContent().trim();

        TestStep bodySubStep = TestStep.builder()
                .name(requestBody)
                .status("passed")
                .build();

        TestStep requestBodyStep = TestStep.builder()
                .name("Тело запроса:")
                .status("passed")
                .steps(List.of(bodySubStep))
                .build();

        List<TestStep> expectedResultSteps = new ArrayList<>();
        int nextIndex = collectExpectedResults(nodes, currentIndex + 1, expectedResultSteps);

        TestStep expectedResultStep = TestStep.builder()
                .name("Expected Result")
                .steps(expectedResultSteps)
                .build();

        List<TestStep> subSteps = new ArrayList<>();
        subSteps.add(requestBodyStep);
        if (!expectedResultSteps.isEmpty()) {
            subSteps.add(expectedResultStep);
        }

        TestStep mainRequestStep = TestStep.builder()
                .name("Отправить запрос:")
                .status("passed")
                .steps(subSteps)
                .parameters(List.of(Parameter.builder().name("Body").value(requestBody).build()))
                .build();

        mainSteps.add(mainRequestStep);
        return nextIndex - 1;
    }

    private int parseQStep(List<TestStep> mainSteps, NodeList nodes, int currentIndex) {
        Element qElement = (Element) nodes.item(currentIndex);
        String qBody = qElement.getTextContent().trim();

        List<TestStep> expectedResultSteps = new ArrayList<>();
        int nextIndex = collectExpectedResults(nodes, currentIndex + 1, expectedResultSteps);
        
        TestStep expectedResultStep = null;
        if (!expectedResultSteps.isEmpty()) {
            expectedResultStep = TestStep.builder()
                .name("Expected Result")
                .steps(expectedResultSteps)
                .build();
        }

        TestStep mainQStep = TestStep.builder()
                .name("Отправить текст в бота:\n" + qBody)
                .status("passed")
                .steps(expectedResultStep != null ? List.of(expectedResultStep) : new ArrayList<>())
                .build();

        mainSteps.add(mainQStep);
        return nextIndex - 1;
    }

    private int collectExpectedResults(NodeList nodes, int startIndex, List<TestStep> expectedResultSteps) {
        for (int i = startIndex; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                if ("a".equals(element.getTagName())) {
                    String state = element.getAttribute("state");
                    String text = element.getTextContent().trim();
                    TestStep aStep = TestStep.builder()
                            .name("state = '" + state + "' " + text)
                            .build();
                    expectedResultSteps.add(aStep);
                } else if ("responseData".equals(element.getTagName())) {
                    // Logic for responseData as per comments
                } else {
                    // Found a different tag, stop collecting
                    return i;
                }
            }
        }
        return nodes.getLength();
    }

    private String replaceParameters(String urlTemplate, Element paramsElement) {
        if (paramsElement == null) return urlTemplate;
        NodeList paramNodes = paramsElement.getChildNodes();
        for (int i = 0; i < paramNodes.getLength(); i++) {
            Node node = paramNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element param = (Element) node;
                String key = param.getTagName();
                String value = param.getTextContent().trim();
                urlTemplate = urlTemplate.replace("${" + key + "}", value);
            }
        }
        return urlTemplate;
    }

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
