FROM eclipse-temurin:25-jre

WORKDIR /app
COPY services/api-gateway/build/libs/api-gateway-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx256m", "-XX:MaxMetaspaceSize=128m", "-jar", "/app/app.jar"]
