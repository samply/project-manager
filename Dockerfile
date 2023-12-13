FROM eclipse-temurin:21-jre

COPY target/project-manager.jar /app/

WORKDIR /app

CMD ["sh", "-c", "java $JAVA_OPTS -jar project-manager.jar"]
