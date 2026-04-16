# ---------- Stage 1: Build frontend ----------
FROM node:20.18-alpine AS frontend-build
WORKDIR /app
COPY frontend/package*.json ./frontend/
RUN cd frontend && npm ci
COPY frontend ./frontend
RUN cd frontend && npm run build

# ---------- Stage 2: Build backend JAR ----------
FROM gradle:8.10.2-jdk17 AS backend-build
WORKDIR /app
COPY backend ./backend
COPY settings.gradle build.gradle ./
COPY --from=frontend-build /app/frontend/dist ./frontend/dist
RUN gradle :backend:bootJar -PskipFrontend --no-daemon

# ---------- Stage 3: Runtime ----------
FROM eclipse-temurin:17.0.13_11-jre-alpine
WORKDIR /app

# non-root 사용자 생성
RUN addgroup -S app && adduser -S -G app app \
    && apk add --no-cache curl \
    && rm -rf /var/cache/apk/*

COPY --from=backend-build --chown=app:app /app/backend/build/libs/*.jar app.jar

USER app
EXPOSE 8083

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD curl -fsS http://localhost:8083/api/recipes?size=1 >/dev/null || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
