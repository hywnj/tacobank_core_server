package com.almagest_dev.tacobank_core_server.application.service;

import com.almagest_dev.tacobank_core_server.common.exception.OcrSendFailedException;
import com.almagest_dev.tacobank_core_server.infrastructure.client.dto.NaverReceiptOcrResponseDto;
import com.almagest_dev.tacobank_core_server.infrastructure.client.naver.NaverOcrApiClient;
import com.almagest_dev.tacobank_core_server.presentation.dto.ReceiptOcrResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptOcrService {

    private final NaverOcrApiClient naverOcrApiClient;

    /**
     * OCR 처리 비즈니스 로직
     * @param file MultipartFile (사용자 업로드 파일)
     * @return ReceiptOcrResponseDto (OCR 결과)
     */
    public ReceiptOcrResponseDto processReceiptOcr(MultipartFile file) {
        log.info("ReceiptOcrService::processReceiptOcr START");

        // 파일 검증
        validateFile(file);

        // 파일 확장자 추출
        String imageName = file.getOriginalFilename();
        String format = extractFileExtension(imageName);

        // OCR API 호출
        try {
            byte[] imageData = file.getBytes();
            log.info("ReceiptOcrService::processReceiptOcr Naver OCR API 호출 START - imageName: {} | format: {}", imageName, format);
            NaverReceiptOcrResponseDto apiResponse = naverOcrApiClient.sendOcrReceipt(imageData, imageName, format);
            log.info("ReceiptOcrService::processReceiptOcr Naver OCR API 호출 END");

            // ReceiptOcrResponse로 변환
            ReceiptOcrResponseDto response = convertToReceiptResponseDto(apiResponse);
            log.info("ReceiptOcrService::processReceiptOcr ReceiptOcrResponse 변환 - {}", response);

            log.info("ReceiptOcrService::processReceiptOcr END");
            return response;

        } catch (IOException e) {
            throw new OcrSendFailedException("ReceiptOcrService::processReceiptOcr - 영수증 이미지 getByte() 실패", e);
        }
    }

    public ReceiptOcrResponseDto convertToReceiptResponseDto(NaverReceiptOcrResponseDto ocrResponse) {
        if (ocrResponse == null || ocrResponse.getImages() == null || ocrResponse.getImages().isEmpty()) {
            throw new IllegalArgumentException("OCR 응답이 비어 있거나 유효하지 않습니다.");
        }
        var image = ocrResponse.getImages().get(0);
        if (image.getReceipt() == null || image.getReceipt().getResult() == null) {
            throw new IllegalArgumentException("OCR 응답에 영수증 결과가 없습니다.");
        }

        var result = image.getReceipt().getResult();

        // Items 매핑
        List<ReceiptOcrResponseDto.Item> items = ocrResponse.getImages().get(0).getReceipt().getResult().getSubResults()
                .stream()
                .flatMap(subResult -> subResult.getItems().stream())
                .map(item -> ReceiptOcrResponseDto.Item.builder()
                        .number(0) // 이후 번호를 설정
                        .name(item.getName().getText())
                        .quantity(Integer.parseInt(item.getCount().getFormatted().getValue()))
                        .price(Integer.parseInt(item.getPrice().getPrice().getFormatted().getValue()))
                        .build()
                )
                .collect(Collectors.toList());

        // 번호 부여
        for (int i = 0; i < items.size(); i++) {
            items.set(i, items.get(i).toBuilder().number(i + 1).build());
        }

        // 총합계 금액 매핑
        int totalAmount = Integer.parseInt(
                ocrResponse.getImages().get(0).getReceipt().getResult().getTotalPrice().getPrice().getFormatted().getValue()
        );

        return ReceiptOcrResponseDto.builder()
                .totalAmount(totalAmount)
                .items(items)
                .build();
    }



    /**
     * 파일 유효성 검증
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드된 파일이 비어 있습니다.");
        }
        if (file.getOriginalFilename() == null || !file.getOriginalFilename().contains(".")) {
            throw new IllegalArgumentException("파일 이름에서 확장자를 추출할 수 없습니다.");
        }
        String extension = extractFileExtension(file.getOriginalFilename());
        List<String> supportedExtensions = List.of("png", "jpg", "jpeg");
        if (!supportedExtensions.contains(extension)) {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다: " + extension);
        }
        long maxSizeInBytes = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > maxSizeInBytes) {
            throw new IllegalArgumentException("업로드된 파일이 5MB를 초과합니다.");
        }
    }


    /**
     * 파일 확장자 추출
     */
    private String extractFileExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}
