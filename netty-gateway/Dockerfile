FROM maven:3.9.8-eclipse-temurin-21 AS builder
WORKDIR /workspace
COPY pom.xml .
COPY backend/pom.xml backend/pom.xml
COPY netty-gateway/pom.xml netty-gateway/pom.xml
COPY netty-gateway/src netty-gateway/src
RUN mvn -pl netty-gateway -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /workspace/netty-gateway/target/netty-gateway-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 9000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
