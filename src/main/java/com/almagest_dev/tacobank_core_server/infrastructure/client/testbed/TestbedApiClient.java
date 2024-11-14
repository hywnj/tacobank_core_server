package com.almagest_dev.tacobank_core_server.infrastructure.client.testbed;

import com.almagest_dev.tacobank_core_server.common.exception.TestbedApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class TestbedApiClient {
    private final String TESTBED_API_URL = "https://almagest.io/fintech/api";
    private final TestbedApiUtil testbedApiUtil;

    /**
     * Testbed API 요청
     */
    public <T, R> R  requestApi(T requestBody, String path, Class<R> responseType) {
        // Set Request Entity (+Header)
        HttpEntity<?> requestEntity = new HttpEntity<>(requestBody, testbedApiUtil.createHeaders());

        RestTemplate restTemplate = new RestTemplate();
        try {
            R response = restTemplate.postForObject(
                    TESTBED_API_URL + path,
                    requestEntity,
                    responseType
            );

            return response;

        } catch (HttpServerErrorException ex) {
            throw new TestbedApiException(ex.getMessage(), ex);
        }
    }

}
