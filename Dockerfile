# ---------- Stage 1: Build frontend ----------
FROM node:20-alpine AS frontend-build
WORKDIR /app
COPY frontend/package*.json ./frontend/
RUN cd frontend && npm ci
COPY frontend ./frontend
RUN cd frontend && npm run build

# ---------- Stage 2: Build backend JAR ----------
FROM gradle:8.10-jdk17 AS backend-build
WORKDIR /app
COPY backend ./backend
COPY settings.gradle build.gradle ./
COPY --from=frontend-build /app/frontend/dist ./frontend/dist
RUN gradle :backend:bootJar -PskipFrontend --no-daemon

# ---------- Stage 3: Runtime ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=backend-build /app/backend/build/libs/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
