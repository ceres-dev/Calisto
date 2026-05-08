FROM eclipse-temurin:21-jre

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 3040

ENTRYPOINT ["java", "-Xms512M", "-Xmx4G", "-jar", "app.jar", "--server.port=3040"]