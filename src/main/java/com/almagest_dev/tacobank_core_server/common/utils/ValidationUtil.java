package com.almagest_dev.tacobank_core_server.common.utils;

import org.springframework.stereotype.Component;

@Component
public class ValidationUtil {
    private static final String ALLOWED_SPECIAL_CHARACTERS = "!@_";

    /**
     * 비밀번호 규칙 검사
     */
    public static boolean validatePassword(String password, int minLen, String birthDate, String tel) {
        if (!isValidLength(password, minLen)) return false; // 길이 체크
        if (!containsAllowedCharacters(password)) return false; // 허용되는 문자가 아닌 문자가 있는 경우
        if (containsSensitiveInfo(password, birthDate, tel)) return false; // 개인식별 번호가 포함된 경우
        if (!containsRequiredTypes(password)) return false; // 영문자, 숫자, 특수문자가 모두 1개 이상 포함되어있지 않은 경우
        if (hasRepeatedCharacters(password)) return false; // 반복되는 문자가 있는 경우
        if (hasSequentialNumbers(password)) return false; // 연속된 숫자가 있는 경우

        return true;
    }

    /**
     * 출금 비밀번호 규칙 검사
     */
    public static boolean validateTransferPin(String pin) {
        if (pin == null || pin.isEmpty()) return false; // Null 또는 빈 값 체크
        if (!pin.matches("\\d+")) return false; // 숫자가 아닌 문자가 포함된 경우
        if (removeNonDigits(pin).length() == 0) return false; // 숫자가 아닌 문자로 온 경우를 거르기 위함
        if (pin.length() != 6) return false; // 6자리가 아닌 경우
        if (hasRepeatedCharacters(pin)) return false; // 반복되는 숫자가 있는 경우
        if (hasSequentialNumbers(pin)) return false; // 연속된 숫자가 있는 경우

        return true;
    }

    /**
     * 길이 유효성 검사 (minLen 이상)
     */
    public static boolean isValidLength(String str, int minLen) {
        return str != null && str.length() >= minLen;
    }

    /**
     * 영문자, 숫자, 허용 특수문자 이외 문자 포함시 False
     */
    public static boolean containsAllowedCharacters(String str) {
        return str.matches("^[a-zA-Z0-9" + ALLOWED_SPECIAL_CHARACTERS + "]+$");
    }

    /**
     * 개인정보 포함시 True
     */
    private static boolean containsSensitiveInfo(String str, String birth, String tel) {
        if (birth == null || birth.trim().isEmpty() ||
                tel == null || tel.trim().isEmpty()) {
            return true; // null 또는 공백 문자열일 경우 true 반환
        }

        String sanitizedBirthDate = removeNonDigits(birth);
        String sanitizedTel = removeNonDigits(tel);
        if (sanitizedBirthDate.isEmpty() || sanitizedTel.isEmpty()) {
            return true; // 숫자가 전혀 없는 경우 true 반환
        }

        return str.contains(sanitizedBirthDate) || str.contains(sanitizedTel);
    }

    /**
     * 영문자, 숫자, 특수문자가 모두 1개 이상 포함되어있지 않으면 False
     */
    public static boolean containsRequiredTypes(String str) {
        return str.matches(".*[a-zA-Z].*") && // 영문자
                str.matches(".*[0-9].*") &&    // 숫자
                str.matches(".*[" + ALLOWED_SPECIAL_CHARACTERS + "].*"); // 특수문자
    }

    /**
     * 3개 이상의 동일한 문자가 있다면 True
     */
    public static boolean hasRepeatedCharacters(String str) {
        return str.matches(".*(.)\\1{2,}.*");
    }

    /**
     * 연속 숫자 여부 확인
     *  - 연속 숫자가 3개 이상인 경우, True
     *      ex) 111, 123
     */
    public static boolean hasSequentialNumbers(String str) {
        for (int i = 0; i < str.length() - 2; i++) {
            char first = str.charAt(i);
            char second = str.charAt(i + 1);
            char third = str.charAt(i + 2);

            if (Character.isDigit(first) && Character.isDigit(second) && Character.isDigit(third)) {
                int diff1 = second - first;
                int diff2 = third - second;

                // 증가, 감소하는 연속 숫자 여부 확인
                if (diff1 == diff2 && Math.abs(diff1) == 1) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 숫자를 제외한 문자 제거
     */
    private static String removeNonDigits(String input) {
        return input == null ? "" : input.replaceAll("[^0-9]", "");
    }
}
