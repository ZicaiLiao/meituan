FROM maven:3.9.8-eclipse-temurin-21 AS builder
WORKDIR /workspace
COPY pom.xml .
COPY backend/pom.xml backend/pom.xml
COPY netty-gateway/pom.xml netty-gateway/pom.xml
COPY backend/src backend/src
RUN mvn -pl backend -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /workspace/backend/target/backend-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

