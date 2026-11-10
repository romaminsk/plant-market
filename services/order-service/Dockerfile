FROM eclipse-temurin:25-jre

WORKDIR /app
COPY infra/docker/secret-entrypoint.sh /usr/local/bin/secret-entrypoint.sh
RUN chmod +x /usr/local/bin/secret-entrypoint.sh
COPY services/order-service/build/libs/order-service-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8083
ENTRYPOINT ["/usr/local/bin/secret-entrypoint.sh", "java", "-Xmx256m", "-XX:MaxMetaspaceSize=128m", "-jar", "/app/app.jar"]
