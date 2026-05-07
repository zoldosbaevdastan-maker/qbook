FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B
COPY backend/src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S qbook && adduser -S qbook -G qbook
COPY --from=build /app/target/qbook-backend-1.0.0.jar app.jar
RUN chown qbook:qbook app.jar
USER qbook
EXPOSE 8080
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-XX:+UseG1GC", "-XX:+UseContainerSupport", "-jar", "app.jar"]
