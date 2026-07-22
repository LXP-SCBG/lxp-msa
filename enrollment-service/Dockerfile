FROM gradle:8.14-jdk17 AS build

WORKDIR /workspace

COPY build.gradle settings.gradle ./

RUN gradle dependencies --no-daemon

COPY src ./src

RUN gradle bootJar -x test --no-daemon


FROM eclipse-temurin:17-jre AS extract

WORKDIR /workspace

COPY --from=build /workspace/build/libs/*.jar application.jar

RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted


FROM eclipse-temurin:17-jre

WORKDIR /application

COPY --from=extract /workspace/extracted/dependencies/ ./
COPY --from=extract /workspace/extracted/spring-boot-loader/ ./
COPY --from=extract /workspace/extracted/snapshot-dependencies/ ./
COPY --from=extract /workspace/extracted/application/ ./

ENTRYPOINT ["java", "-jar", "application.jar"]