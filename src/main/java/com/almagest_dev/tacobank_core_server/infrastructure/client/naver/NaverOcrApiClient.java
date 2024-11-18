package com.almagest_dev.tacobank_core_server.infrastructure.client.naver;

import com.almagest_dev.tacobank_core_server.common.exception.OcrSendFailedException;
import com.almagest_dev.tacobank_core_server.infrastructure.client.dto.NaverReceiptOcrResponseDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class NaverOcrApiClient {
    @Value("${naver.ocr.api.url}")
    private String NAVER_OCR_API_URL;

    private final NaverApiUtil naverApiUtil;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules() // Java 8 날짜, 시간 지원
            .setSerializationInclusion(JsonInclude.Include.NON_NULL) // null 필드 제외
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // 알 수 없는 필드 무시


    public NaverReceiptOcrResponseDto sendOcrReceipt(byte[] imageData, String imageName, String format) {
        log.info("NaverOcrApiClient::sendOcrReceipt START");
        // 1. HttpHeaders 생성
        HttpHeaders headers = naverApiUtil.createOcrHeaders();
        log.info("NaverOcrApiClient::sendOcrReceipt Headers - {}", headers);

        // 2. message 메타데이터 생성
        Map<String, Object> message = Map.of(
                "version", "V2",
                "requestId", UUID.randomUUID().toString(),
                "timestamp", System.currentTimeMillis(),
                "images", List.of(
                        Map.of(
                                "format", format,
                                "name", imageName
                        )
                )
        );
        log.info("NaverOcrApiClient::sendOcrReceipt message - {}", message);

        // 3. MultipartBodyBuilder를 사용해 요청 바디 생성
        MultipartBodyBuilder builder = new MultipartBodyBuilder();

        // message 추가
        String jsonMessage = createJsonMetadata(message);
        builder.part("message", jsonMessage)
                .header("Content-Disposition", "form-data; name=message")
                .header("Content-Type", "application/json");

        // file 추가
        Resource imageResource = new ByteArrayResource(imageData) {
            @Override
            public String getFilename() {
                return imageName; // 전달받은 파일명 사용
            }
        };

        String validatedFormat = validateImageFormat(format);
        builder.part("file", imageResource)
                .header("Content-Disposition", "form-data; name=file; filename=" + imageName)
                .header("Content-Type", "image/" + validatedFormat);

        // 4. HttpEntity 생성
        HttpEntity<?> requestEntity = new HttpEntity<>(builder.build(), headers);
        log.info("NaverOcrApiClient::sendOcrReceipt requestEntity - {}", requestEntity);

        // 5. API 요청
        try {
            log.info("NaverOcrApiClient::sendOcrReceipt CALL Naver OCR API");
            NaverReceiptOcrResponseDto response = restTemplate.postForObject(
                    NAVER_OCR_API_URL,
                    requestEntity,
                    NaverReceiptOcrResponseDto.class
            );
            log.info("NaverOcrApiClient::sendOcrReceipt Naver OCR API response - {}", response);

            // 응답 결과가 실패인 경우
            if (response == null || response.getImages() == null || response.getImages().isEmpty()) {
                throw new OcrSendFailedException("응답이 null이거나 유효하지 않습니다.");
            }

            if (!"SUCCESS".equals(response.getImages().get(0).getInferResult())) {
                throw new OcrSendFailedException(
                        String.format("NaverOcrApiClient::sendOcrReceipt 요청 실패 - [requestId=%s, uid=%s, inferResult=%s, message=%s]",
                                response.getRequestId(),
                                response.getImages().get(0).getUid(),
                                response.getImages().get(0).getInferResult(),
                                response.getImages().get(0).getMessage())
                );
            }

            log.info("NaverOcrApiClient::sendOcrReceipt END");
            return response;

        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw new OcrSendFailedException(ex.getMessage(), ex);

        }  catch (Exception ex) {
            throw new OcrSendFailedException("알 수 없는 오류 발생: " + ex.getMessage(), ex);
        }

    }


    // 메타데이터를 JSON으로 변환
    private String createJsonMetadata(Map<String, Object> metadata) {
        try {
            // Map 데이터를 JSON 문자열로 변환
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 직렬화 중 오류 발생", e);
        }
    }

    // 파일 형식 지정
    private String validateImageFormat(String format) {
        List<String> supportedFormats = Arrays.asList("png", "jpg", "jpeg");
        if (!supportedFormats.contains(format.toLowerCase())) {
            throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다: " + format);
        }
        return format.toLowerCase();
    }

}
