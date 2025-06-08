FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY target/agent-module-1.0-SNAPSHOT.jar agent.jar
ENV SERVER_URL=http://server-module:8080/metrics
CMD ["java", "-cp", "agent.jar", "com.example.agent.AgentMain"]
