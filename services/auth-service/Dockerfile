FROM eclipse-temurin:25-jre

WORKDIR /app
COPY services/auth-service/build/libs/auth-service-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-Xmx256m", "-XX:MaxMetaspaceSize=128m", "-jar", "/app/app.jar"]
