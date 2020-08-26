package lhdt.sender;

import com.fasterxml.jackson.databind.ObjectMapper;
import lhdt.config.PropertiesConfig;
import lhdt.domain.*;
import lhdt.support.LogMessageSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

@Slf4j
public class ResultSender {

    /**
     * Converter 실행 후 로그파일 전송
     * @param converterJob  converterJob
     * @param objectMapper  objectMapper
     * @param propertiesConfig  propertiesConfig  
     * @param restTemplate  restTemplate
     * @param serverTarget    serverTarget
     * @param filePath  filePath
     * @throws IOException  IOException
     * @throws URISyntaxException   URISyntaxException
     */
    public static void sendLog(ConverterJob converterJob, ObjectMapper objectMapper, PropertiesConfig propertiesConfig,
                               RestTemplate restTemplate, ServerTarget serverTarget, String filePath)
            throws IOException, URISyntaxException {
        
        if (invalidFilePath(filePath))
            throw new IOException("지정된 파일을 찾을 수 없습니다. : " + filePath);

        Path sendFilePath = Paths.get(filePath);
        File logFile = sendFilePath.toFile();
        ConverterResultLog resultLog = objectMapper.readValue(logFile, ConverterResultLog.class);
        resultLog.setConverterJob(converterJob);
        log.info(">>>>>>>>>> isSuccess : {}", resultLog.getIsSuccess());
        log.info(">>>>>>>>>> convertedNumber : {} / {}", resultLog.getNumberOfFilesConverted(), resultLog.getNumberOfFilesToBeConverted());
        log.info(">>>>>>>>>> failureLog : {}", resultLog.getFailureLog());

        URI uri = getSendURI(propertiesConfig, serverTarget, "/api/agent/logs");
        ResponseEntity<ConverterResultLog> responseEntity =
                restTemplate.postForEntity(uri, resultLog, ConverterResultLog.class);
        log.info(">>>>>>>>>> status : {}", Objects.requireNonNull(responseEntity.getBody()).getConverterJob().getConverterJobStatus());
        log.info(">>>>>>>>>> errorCode : {}", Objects.requireNonNull(responseEntity.getBody()).getConverterJob().getErrorCode());
    }

    /**
     * Converter 실행 후 위치파일 전송
     * @param converterJob  converterJob
     * @param objectMapper  objectMapper
     * @param propertiesConfig  propertiesConfig  
     * @param restTemplate  restTemplate
     * @param serverTarget    serverTarget
     * @param filePath  filePath
     * @throws IOException  IOException
     * @throws URISyntaxException   URISyntaxException
     */
    public static void sendLocation(ConverterJob converterJob, ObjectMapper objectMapper, PropertiesConfig propertiesConfig,
                                    RestTemplate restTemplate, ServerTarget serverTarget, String filePath)
            throws IOException, URISyntaxException {

        if (invalidFilePath(filePath))
            throw new IOException("지정된 파일을 찾을 수 없습니다. : " + filePath);

        Path sendFilePath = Paths.get(filePath);
        File locationFile = sendFilePath.toFile();
        ConverterLocation location = objectMapper.readValue(locationFile, ConverterLocation.class);
        location.setConverterJob(converterJob);
        log.info("longitude = {}, latitude = {}", location.getLongitude(), location.getLatitude());

        URI uri = getSendURI(propertiesConfig, serverTarget, "/api/agent/locations");
        ResponseEntity<ConverterLocation> responseEntity =
                restTemplate.postForEntity(uri, location, ConverterLocation.class);
        log.info(">>>>>>>>>> status : {}", Objects.requireNonNull(responseEntity.getBody()).getConverterJob().getConverterJobStatus());
        log.info(">>>>>>>>>> errorCode : {}", Objects.requireNonNull(responseEntity.getBody()).getConverterJob().getErrorCode());
    }

    /**
     * Converter 실행 후 속성파일 전송
     * @param converterJob  converterJob
     * @param propertiesConfig  propertiesConfig
     * @param restTemplate  restTemplate
     * @param serverTarget    serverTarget
     * @param filePath  filePath
     * @throws IOException  IOException
     * @throws URISyntaxException   URISyntaxException
     */
    public static void sendAttribute(ConverterJob converterJob, PropertiesConfig propertiesConfig,
                                     RestTemplate restTemplate, ServerTarget serverTarget, String filePath)
            throws IOException, URISyntaxException {

        if (invalidFilePath(filePath))
            throw new IOException("지정된 파일을 찾을 수 없습니다. : " + filePath);

        Path sendFilePath = Paths.get(filePath);

        // File attributeFile = sendFilePath.toFile();
        // List<Map<String, Object>> attributes = objectMapper.readValue(attributeFile, new TypeReference<>() {});

        // json 파일을 Object로 변환하여 전송할 예정이였으나, json string으로 DB에 넣기 때문에 string 변경함.
        byte[] jsonData = Files.readAllBytes(sendFilePath);
        String attributes = new String(jsonData, StandardCharsets.UTF_8);
        log.info(">>>>>>>>>> attributesJson : {}", attributes);

        ConverterAttribute attribute = new ConverterAttribute();
        attribute.setConverterJob(converterJob);
        attribute.setAttributes(attributes);
        URI uri = getSendURI(propertiesConfig, serverTarget, "/api/agent/attributes");
        ResponseEntity<ConverterAttribute> responseEntity =
                restTemplate.postForEntity(uri, attribute, ConverterAttribute.class);
        log.info(">>>>>>>>>> status : {}", Objects.requireNonNull(responseEntity.getBody()).getConverterJob().getConverterJobStatus());
        log.info(">>>>>>>>>> errorCode : {}", Objects.requireNonNull(responseEntity.getBody()).getConverterJob().getErrorCode());
    }

    /**
     * Converter 실행 후 변환결과 전송
     * @param converterJob  converterJob
     * @param propertiesConfig  propertiesConfig
     * @param restTemplate  restTemplate
     * @param serverTarget    serverTarget
     */
    public static void sendConverterJobStatus(ConverterJob converterJob, PropertiesConfig propertiesConfig, 
                                              RestTemplate restTemplate, ServerTarget serverTarget) {
        URI uri;
        try {
            uri = getSendURI(propertiesConfig, serverTarget, "/api/converters/status");
            ResponseEntity<ConverterJob> responseEntity = restTemplate.postForEntity(uri, converterJob, ConverterJob.class);
            log.info(">>>>>>>>>> status : {}", Objects.requireNonNull(responseEntity.getBody()).getConverterJobStatus());
            log.info(">>>>>>>>>> errorCode : {}", Objects.requireNonNull(responseEntity.getBody()).getErrorCode());
        } catch (URISyntaxException e) {
            LogMessageSupport.printMessage(e);
        }
    }

    /**
     * 전송할 파일의 유효성 검사
     * @param filePath  filePath
     * @return 파일 유효성 검사
     */
    private static boolean invalidFilePath(String filePath) {
        boolean result = false;
        Path sendFilePath = Paths.get(filePath);
        File sendFile = sendFilePath.toFile();
        if (!sendFile.exists()) {
            result = true;
        }
        return result;
    }

    /**
     * 변환을 수행한 서버(ADMIN, USER)로 전송하기 위해 호출할 URI를 생성
     * @param propertiesConfig  propertiesConfig
     * @param serverTarget    serverTarget
     * @param url   url
     * @return 호출할 URI
     * @throws URISyntaxException   URISyntaxException
     */
    private static URI getSendURI(PropertiesConfig propertiesConfig, ServerTarget serverTarget, String url)
            throws URISyntaxException {
        URI uri;
        if (ServerTarget.USER == serverTarget) {
            uri = new URI(propertiesConfig.getCmsUserRestServer() + url);
        } else {
            uri = new URI(propertiesConfig.getCmsAdminRestServer() + url);
        }
        return uri;
    }
}