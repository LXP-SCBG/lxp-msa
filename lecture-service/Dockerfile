FROM eclipse-temurin:17-jdk AS build

WORKDIR /workspace

COPY . .

RUN ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /workspace/build/libs/*.jar app.jar

# 실제 PORT번호는 9081
EXPOSE 9080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]