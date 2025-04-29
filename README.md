# TacoBank - Core Server (비즈니스 서버)  
타코뱅크는 영수증 OCR 인식을 통해 품목별 정산 기능을 제공하는 오픈뱅킹 서비스 프로젝트입니다.<br>
타코뱅크는 다음과 같이 두 개의 주요 서버로 구성됩니다 :
1. **인증 서버 (`auth_server`)**: 사용자 인증 및 토큰 기반 보안을 담당
2. **코어 서버 (`core_server`)**: 송금, 문자인증, 멤버 관리, 영수증 더치페이 기능과 같은 비즈니스 로직을 담당
<br>

## **📌 개요**  
`core_server`는 타코뱅크의 핵심 비즈니스 로직을 담당하는 서버입니다.  
송금, 문자인증, 멤버 관리, 영수증 더치페이 등 다양한 기능을 제공합니다.

---

## **✨ 주요 기능**  
- **더치페이 (정산)**: 정산 내역 조회, 정산 요청 (1/N 방식, 영수증 기반 더치페이)  
- **영수증 OCR 인식**: OCR API를 활용한 영수증 데이터 인식 및 처리
- **송금 기능**:
  - 수취인 조회 및 송금 요청  
  - **Idempotency Key**를 사용해 중복 송금 요청 방지  
  - **Redis를 활용한 실시간 송금 세션 관리**
- **문자 인증**: 문자 인증 요청 및 검증  
- **멤버 기능**: 회원 정보 조회 및 수정, 탈퇴, 비밀번호 및 출금 핀번호 수정, 이메일로 친구 검색 등
- **계좌 관리**: 계좌 조회, 메인 계좌 설정, 즐겨찾기 관리, 최근 이체 내역 조회 등
- **친구 및 그룹 관리**: 친구/그룹 신청, 수락, 거절, 삭제, 추방  
- **알림 시스템**: 알림 조회 및 읽음 처리, 독촉 알림 전송  

---

## **🛠️ 기술 스택**  
- **Java 17**  
- **Spring Boot 3.3**  
- **Spring Security 6.3**  
- **JPA (Java Persistence API)**  
- **Redis 7.2**  
- **Gradle 8.10**
- Naver Cloud Platform - **OCR 영수증 API**
- Naver Cloud Platform - **SMS API**

---

## **🔐 보안 정책**  
- **JWT 토큰 검증**: 모든 요청에서 토큰 유효성 검증  
- **AES-256 암호화**: 민감한 금융 데이터 암호화
- **Bcrypt 해싱**으로 비밀번호 안전 저장
- **Idempotency Key**: 중복 송금 방지  
- **Redis 세션 관리**: 사용자 상태, 송금 세션, 문자 본인인증 및 토큰 블랙리스트 관리  

---

## **📂 프로젝트 구조**  
```plaintext
src/
├── main
│   ├── java
│   │   └── com
│   │       └── almagest_dev
│   │           └── tacobank_core_server
│   │               ├── TacobankCoreServerApplication.java
│   │               ├── application  # 비즈니스 로직 서비스 계층
│   │               │   ├── logging
│   │               │   └── service
│   │               ├── common       # 공통 모듈 및 유틸리티 등
│   │               │   ├── constants  # 공통 상수 (Redis Key Prefix)
│   │               │   ├── dto
│   │               │   ├── exception  # 공통 예외처리
│   │               │   └── utils      # 공통 유틸리티 클래스
│   │               ├── domain    # 도메인 모델 및 리포지토리 (도메인 별로 분리)
│   │               │   ├── account
│   │               │   ├── bankCode
│   │               │   ├── friend
│   │               │   ├── group
│   │               │   ├── member
│   │               │   ├── notification
│   │               │   ├── receipt
│   │               │   ├── settlememt
│   │               │   ├── sms
│   │               │   └── transfer
│   │               ├── infrastructure  # 인프라 및 보안 설정
│   │               │   ├── config      
│   │               │   ├── encryption  # 암복호 유틸리티 클래스
│   │               │   ├── external    # 외부 통신 API
│   │               │   │   ├── naver
│   │               │   │   │   ├── client   # Naver OCR, SMS API 통신
│   │               │   │   │   ├── dto
│   │               │   │   │   │   ├── ocr  # Naver OCR API
│   │               │   │   │   │   └── sms  # Naver SMS API
│   │               │   │   │   └── util     # Naver API 공통 연결
│   │               │   │   ├── s3
│   │               │   │   └── testbed
│   │               │   │       ├── client
│   │               │   │       ├── dto
│   │               │   │       └── util
│   │               │   ├── persistence
│   │               │   ├── security        # Security 설정
│   │               │   │   ├── authentication
│   │               │   │   └── handler
│   │               │   └── sms
│   │               │       ├── dto   
│   │               │       └── util  # SMS 문자인증 공통 모듈
│   │               └── presentation  # API 컨트롤러 및 DTO (도메인 별로 분리)
│   │                   ├── controller
│   │                   └── dto
│   │                       ├── account
│   │                       ├── auth
│   │                       ├── friend
│   │                       ├── group
│   │                       ├── home
│   │                       ├── member
│   │                       ├── notify
│   │                       ├── receipt
│   │                       ├── settlement
│   │                       ├── transantion
│   │                       └── transfer
│   └── resources
│       ├── application.yml         # 메인 환경 설정
│       ├── application-{서버명}.yml  # 서버별 환경 설정
│       └── logback-spring.xml      # 로깅 설정
└── test
```

---

## **💬 문의**  
- **담당자**: [Hyewon Ju](https://github.com/hywnj)  |  [Han Ji Yun](https://github.com/Koreanpaper)
- **이메일**: jhjsjym@naver.com

