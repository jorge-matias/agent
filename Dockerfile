# Build stage with Maven
FROM maven:3.9-eclipse-temurin-17

WORKDIR /build
COPY pom.xml .
COPY agent-module/pom.xml agent-module/
COPY server-module/pom.xml server-module/

# Download dependencies first (cache layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY agent-module/src agent-module/src
COPY server-module/src server-module/src

# Build both modules
RUN mvn clean package
