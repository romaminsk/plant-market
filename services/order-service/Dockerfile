FROM eclipse-temurin:25-jre

WORKDIR /app
COPY services/order-service/build/libs/order-service-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-Xmx256m", "-XX:MaxMetaspaceSize=128m", "-jar", "/app/app.jar"]
