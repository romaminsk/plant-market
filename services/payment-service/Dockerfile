FROM eclipse-temurin:25-jre

WORKDIR /app
COPY services/payment-service/build/libs/payment-service-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-Xmx256m", "-XX:MaxMetaspaceSize=128m", "-jar", "/app/app.jar"]
