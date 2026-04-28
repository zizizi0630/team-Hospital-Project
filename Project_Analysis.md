# HealthcareTP 프로젝트 분석 보고서

## 1. 프로젝트 개요
- **프로젝트명**: HealthcareTP
- **설명**: Healthcare Team project for Spring Boot
- **기반 프레임워크**: Spring Boot 3.5.9
- **Java 버전**: 21
- **빌드 시스템**: Gradle

## 2. 기술 스택
`build.gradle` 분석을 통해 확인된 주요 라이브러리와 기술입니다.

### Backend Framework
- **Spring Boot Starter Web**: RESTful API 및 웹 애플리케이션 구축
- **Spring Data JPA**: 데이터베이스 ORM 및 접근
- **Spring Security**: 인증 및 인가 처리
- **Validation**: 데이터 유효성 검증

### Frontend (Server-side Rendering)
- **Thymeleaf**: 뷰 템플릿 엔진
- **Thymeleaf Layout Dialect**: 레이아웃 재사용성 향상
- **Thymeleaf Extras Spring Security**: 뷰 단에서의 보안 처리 지원

### Database
- **MariaDB Driver**: MariaDB 데이터베이스 연동
- **H2 Database** (주석 처리됨): 로컬 테스트용으로 추정되나 현재는 MariaDB 사용 중

### AI Integration
- **Spring AI (OpenAI)**: OpenAI 모델(ChatGPT 등) 연동 지원
  - 버전: 1.1.2
  - `spring-ai-starter-model-openai` 사용

### Utilities
- **Lombok**: boilerplate 코드(Getter/Setter 등) 자동 생성

## 3. 소스 코드 구조 분석 (`src/main/java/healthcare`)
프로젝트는 도메인별로 패키지가 구성되어 있는 것으로 보입니다.

- **`User`**: 사용자 계정, 권한, 프로필 관리
- **`Appointment`**: 병원/진료 예약 시스템
- **`Question` / `Answer`**: Q&A 게시판 또는 상담 기능
- **`OpenAI`**: AI 모델과의 통신 로직 및 서비스
- **`Tool` / `Code`**: 유틸리티 클래스 또는 공통 코드 관리
- **설정 파일**:
    - `SecurityConfig.java`, `ApiSecurityConfig.java`: 웹 및 API 보안 설정 분리 추정
    - `HealthcareTpApplication.java`: 메인 클래스 (`@EnableAsync` 어노테이션을 통해 비동기 처리가 활성화됨)

## 4. 주요 설정 분석 (`application.properties`)

### 데이터베이스 설정
- **DBMS**: MariaDB
- **URL**: `jdbc:mariadb://180.210.82.240:3306/healthcare_db`
- **DDL Auto**: `update` (스키마 자동 업데이트 활성화 - 상용 배포 시 주의 필요)
- **JPA**: SQL 쿼리 로깅이 활성화되어 있음 (`show_sql=true`)

### OpenAI 설정
- **API Key**: 설정 파일에 API 키가 포함되어 있습니다.
  - *보안 권고*: `application.properties`에 API Key를 직접 입력하는 것은 보안상 위험합니다. 환경 변수(`${OPENAI_API_KEY}`)를 사용하도록 변경하는 것을 강력히 권장합니다.

### 서버 및 파일 업로드
- **포트**: 8080
- **파일 업로드 제한**: 파일당 10MB / 요청당 15MB
- **캐시 설정**: 정적 리소스에 대한 캐시가 비활성화됨 (`no-cache`, `no-store`) - 개발 단계 설정으로 보임

## 5. 종합 의견
이 프로젝트는 **Spring Boot 3.x**와 **Java 21** 최신 스택을 기반으로, **JPA**와 **Thymeleaf**를 사용하는 전형적인 모놀리식(혹은 하이브리드) 웹 애플리케이션 구조입니다.
특이점으로 **Spring AI**를 도입하여 헬스케어 관련 질문 답변이나 예약 보조 등의 AI 기능을 통합하려는 시도가 보입니다.

**⚠️ 보안 주의사항**:
소스코드 내(`application.properties`)에 **DB 비밀번호**와 **OpenAI API Key**가 평문으로 노출되어 있습니다. 이 파일이 공개 저장소(GitHub 등)에 업로드되지 않도록 주의하거나, 해당 값들을 환경 변수나 별도의 설정 파일로 분리해야 합니다.
