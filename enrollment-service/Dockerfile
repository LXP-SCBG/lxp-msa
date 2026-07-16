FROM eclipse-temurin:17-jdk AS build

WORKDIR /workspace

COPY . .

RUN ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /workspace/build/libs/*.jar app.jar

EXPOSE 9082

ENTRYPOINT ["java", "-jar", "/app/app.jar"]