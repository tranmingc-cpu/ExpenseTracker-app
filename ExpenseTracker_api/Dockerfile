FROM gradle:8.5-jdk17 AS build
WORKDIR /home/gradle/src

COPY --chown=gradle:gradle . .

RUN chmod +x gradlew && ./gradlew build -x test

FROM eclipse-temurin:17-jre-alpine
EXPOSE 8080
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
