FROM gradle:8.5-jdk17 AS build
COPY --chown=gradle:gradle . ./
WORKDIR /home/gradle/src
RUN chmod +x gradlew && ./gradlew build -x test

FROM eclipse-temurin:17-jre-alpine
EXPOSE 8080
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
