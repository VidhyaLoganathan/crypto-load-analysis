FROM maven:3.9.4-eclipse-temurin-17 AS build

WORKDIR /app

# Copy everything and build
COPY pom.xml .
COPY src ./src
RUN mvn clean install -DskipTests

# (Optional) Inspect build output
# RUN ls -la /app
# RUN ls -la /app/target

################################################################################
# 2) Run stage
################################################################################
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /

# Create a non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the built JAR
COPY --from=build /app/target/*.jar backend.jar

# Expose the application port
EXPOSE 8080

# Switch to non-root
USER appuser

# Run the JAR
ENTRYPOINT ["java","-jar","/backend.jar"]
