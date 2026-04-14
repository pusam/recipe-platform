# Recipe Platform

YouTube 요리 영상 URL을 입력하면 AI(Gemini)가 자막/제목/설명을 분석해 레시피를 추출하고 저장하는 사이드 프로젝트.

## 스택
- Backend: Spring Boot 3.3 + JPA + Flyway + H2(기본) / MariaDB(선택)
- Frontend: Vue 3 (Options API) + Vite
- AI: Google Gemini 2.0 Flash

## 실행 방법

### 1. 환경변수
```bash
export GEMINI_API_KEY=your_gemini_api_key
# 운영 시 (개발은 디폴트 사용 가능)
export JWT_SECRET=at-least-32-bytes-long-random-secret-string
```

### 2. 백엔드 (포트 8082)
```bash
./gradlew :backend:bootRun -PskipFrontend
```
H2 콘솔: http://localhost:8082/h2-console
- JDBC URL: `jdbc:h2:file:./data/recipe;MODE=MySQL`
- User: `sa` / Password: (없음)

### 3. 프론트엔드
```bash
cd frontend
npm install
npm run dev
```
http://localhost:5173 (Vite가 `/api`를 백엔드 8082로 프록시)

### 4. 통합 빌드 (단일 JAR)
```bash
./gradlew :backend:bootJar
java -jar backend/build/libs/backend-0.0.1-SNAPSHOT.jar
```

## API
- `POST /api/auth/signup` `{ email, username, password }` → JWT 발급
- `POST /api/auth/login` `{ email, password }` → JWT 발급
- `GET /api/users/me` (인증) → 내 정보
- `GET /api/users/me/favorites` (인증) → 내 즐겨찾기 목록
- `GET/POST/DELETE /api/favorites/{recipeId}` (인증) → 즐겨찾기 상태/추가/삭제
- `POST /api/recipes` `{ youtubeUrl }` (인증) → 레시피 추출 + 저장
- `GET /api/recipes` → 목록 (공개)
- `GET /api/recipes/{id}` → 상세 (공개)
- `DELETE /api/recipes/{id}` (인증) → 삭제

## 한계
- 자막이 없는 영상(요리 ASMR 등)은 제목/설명만으로 추출 → 정확도 낮음
- YouTube의 자막 추출 방식은 비공식 → 깨질 수 있음. 깨지면 yt-dlp 등으로 교체.
