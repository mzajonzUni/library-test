FROM maven:3.8.5-openjdk-17 AS maven_build
COPY ./pom.xml pom.xml
COPY ./src src/
RUN mvn clean package

FROM openjdk:17
COPY --from=maven_build target/*.jar app.jar
COPY start.sh .
ENTRYPOINT ["/bin/bash","start.sh"]