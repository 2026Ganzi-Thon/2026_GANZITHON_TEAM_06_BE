# ---- build ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle* build.gradle* ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon > /dev/null 2>&1 || true
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# ---- run ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 app
COPY --from=build /app/build/libs/*.jar app.jar
USER app
EXPOSE 8080
# 힙 크기는 서버 .env의 JAVA_OPTS로 바꿉니다 (deploy/.env.example 참고)
ENV JAVA_OPTS="-Xms256m -Xmx512m"
# exec로 셸 대신 java가 PID 1이 됩니다.
# docker stop의 SIGTERM이 java에 바로 전달되어 graceful shutdown(server.shutdown: graceful)이 동작합니다.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
