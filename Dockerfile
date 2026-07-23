FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q clean package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/legal-parser.jar /app/legal-parser.jar
ENTRYPOINT ["java", "-jar", "/app/legal-parser.jar"]
