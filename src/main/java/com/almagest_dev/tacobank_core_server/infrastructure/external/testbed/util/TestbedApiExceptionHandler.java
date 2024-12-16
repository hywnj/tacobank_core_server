package com.almagest_dev.tacobank_core_server.infrastructure.external.testbed.util;

import com.almagest_dev.tacobank_core_server.common.exception.TestbedApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestbedApiExceptionHandler {
    public static ParsedError parseException(TestbedApiException ex) {
        try {
            String responseBody = ex.getResponseBody();

            // Json 형식 여부 검증
            if (responseBody == null || !responseBody.trim().startsWith("{")) {
                log.error("TestbedApiExceptionHandler::parseException - Invalid JSON Response: {}", responseBody);
                throw new Exception("Json 형식이 아닌 응답");
            }

            // Json 파싱
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(ex.getResponseBody());

            String apiTranId = rootNode.has("apiTranId") ? rootNode.get("apiTranId").asText("") : "";
            String rspMessage = rootNode.has("rspMessage") ? rootNode.get("rspMessage").asText("") : "요청 처리 중 오류 발생";
            String rspCode = rootNode.has("rspCode") ? rootNode.get("rspCode").asText("") : "ERR001";

            log.error("TestbedApiExceptionHandler::parseException - Error Response Parsed: rspMessage: {}, rspCode: {}", rspMessage, rspCode);
            return new ParsedError(apiTranId, rspCode, rspMessage);

        } catch (Exception parseException) {
            log.error("TestbedApiExceptionHandler::parseException - JSON Parsing Error: {}", parseException.getMessage());
            return new ParsedError("", "ERR999", "서버 에러가 발생했습니다. 잠시 후 다시 시도해주세요.");
        }
    }


    @Data
    @AllArgsConstructor
    public static class ParsedError {
        private String apiTranId;
        private String rspCode;
        private String rspMessage;
    }
}
