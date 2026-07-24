FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q clean package

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*     && useradd --system --uid 10001 --create-home legalai
WORKDIR /app
COPY --from=build /workspace/target/legal-ai-platform-0.1.0.jar /app/app.jar
RUN mkdir -p /data/legal-ai/original-documents && chown -R legalai:legalai /data/legal-ai /app
USER legalai
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
