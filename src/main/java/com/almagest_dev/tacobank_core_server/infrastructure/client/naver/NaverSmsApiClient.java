package com.almagest_dev.tacobank_core_server.infrastructure.client.naver;

import com.almagest_dev.tacobank_core_server.common.exception.SmsSendFailedException;
import com.almagest_dev.tacobank_core_server.infrastructure.client.dto.Message;
import com.almagest_dev.tacobank_core_server.infrastructure.client.dto.NaverSmsRequestDto;
import com.almagest_dev.tacobank_core_server.infrastructure.client.dto.NaverSmsResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NaverSmsApiClient {
    @Value("${naver.sms.from}")
    private String NAVER_SMS_FROM_NUM;
    @Value("${naver.sms.service.id}")
    private String NAVER_SMS_SERVICE_ID;
    private static final String NAVER_SMS_API_URL ="https://sens.apigw.ntruss.com/sms/v2";
    private final NaverApiUtil naverApiUtil;
    private final RestTemplate restTemplate;

    /**
     * Naver SMS API 요청
     *  - 메시지 리스트로 받아서 요청
     */
    public NaverSmsResponseDto sendSms(List<Message> messages) {
        log.info("NaverSmsApiClient::sendSms START");
        log.info("NaverSmsApiClient::sendSms from - {}", NAVER_SMS_FROM_NUM);
        Long time = System.currentTimeMillis();
        // Set Request Body
        NaverSmsRequestDto requestBody = new NaverSmsRequestDto(
                "SMS"
                , "COMM"
                , "82"
                , NAVER_SMS_FROM_NUM
                , "기본 메시지 내용"
                , messages
        );
        // Set Request Entity (+Header)
        HttpHeaders headers = naverApiUtil.createSmsHeaders(time);
        HttpEntity<NaverSmsRequestDto> requestEntity = new HttpEntity<>(requestBody, headers);
        log.info("NaverSmsApiClient::sendSms Request Header - " + headers + " | Body - " + requestBody);
        // Naver SMS API 요청
        try {
            log.info("NaverSmsApiClient::sendSms CALL Naver API");
            NaverSmsResponseDto response = restTemplate.postForObject(
                    NAVER_SMS_API_URL + "/services/" + NAVER_SMS_SERVICE_ID + "/messages",
                    requestEntity,
                    NaverSmsResponseDto.class
            );
            log.info("NaverSmsApiClient::sendSms Response: {}", response);

            // 응답 결과가 실패인 경우
            if (response.getStatusName().equals("fail") || !response.getStatusCode().equals("202")) {
                throw new SmsSendFailedException(
                        "TERMINATED",
                        "NaverSmsApiClient::sendSms 요청 실패 - " +
                        "[" + response.getRequestId() + "] "
                        + response.getStatusCode() + " | " + response.getStatusName()
                        + " (" + response.getRequestTime() + ")",
                        HttpStatus.BAD_REQUEST);
            }

            log.info("NaverSmsApiClient::sendSms END");
            return response;

        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("NaverSmsApiClient::sendSms Http Exception: {}, Response Body: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new SmsSendFailedException("TERMINATED", ex.getMessage(), HttpStatus.BAD_REQUEST,ex);
        }
    }

}
