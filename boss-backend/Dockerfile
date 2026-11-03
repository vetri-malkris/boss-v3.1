# --- Build stage ---
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B package -DskipTests

# --- Runtime stage ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S boss && adduser -S boss -G boss
COPY --from=build /workspace/target/boss-backend-*.jar app.jar
RUN chown boss:boss app.jar
USER boss

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
