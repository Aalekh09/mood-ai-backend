# Use official Java 17 image
FROM eclipse-temurin:17-jdk

# Set working directory
WORKDIR /app

# Copy jar file
COPY target/mood-ai-backend-1.0.0.jar app.jar

# Expose port (Render injects PORT automatically)
EXPOSE 8080

# Run Spring Boot
ENTRYPOINT ["java","-jar","/app/app.jar"]
