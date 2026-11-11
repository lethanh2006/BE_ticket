# ---- build: biên dịch và đóng gói jar ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src src
# Test cần Docker (Testcontainers) nên chạy ở CI, không chạy khi build image
RUN mvn -B -q -DskipTests package

# ---- chạy: chỉ JRE, user không phải root ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /workspace/target/dat-ve-*.jar app.jar
USER app
# Gói miễn phí ~512 MB RAM: heap 60%, GC đơn luồng, chỉ JIT C1 để khởi động nhanh và tốn ít bộ nhớ
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
