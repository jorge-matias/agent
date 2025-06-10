#!/usr/bin/env bash

# Color codes for better output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Help function
print_help() {
    echo -e "${GREEN}Metrics System Build & Run Script${NC}"
    echo
    echo "Usage: ./run.sh [build-type] [run-type]"
    echo
    echo "Build Types:"
    echo "  --build-local-jvm       Build using local Maven installation"
    echo "  --build-local-native    Build native macOS binary using local GraalVM"
    echo "  --build-docker          Build using Maven in Docker"
    echo
    echo "Run Types:"
    echo "  --run-local-native      Run native macOS agent with Docker server"
    echo "  --run-local-jvm         Run JVM agent with Docker server"
    echo "  --run-docker-jvm        Run JVM agent and server in Docker"
    echo "  --run-docker-graal      Run native agent and server in Docker"
}

# Error handling
error() {
    echo -e "${RED}Error: $1${NC}" >&2
    exit 1
}

# Check if Docker is running
check_docker() {
    if ! docker info >/dev/null 2>&1; then
        error "Docker is not running. Please start Docker and try again."
    fi
}

# Build functions
build_local_jvm() {
    echo -e "${GREEN}Building with local Maven...${NC}"
    mvn clean package || error "Maven build failed"
}

build_local_native() {
    echo -e "${GREEN}Building native macOS binary with local GraalVM...${NC}"
    if ! command -v native-image >/dev/null; then
        error "GraalVM native-image not found. Please install GraalVM and native-image"
    fi

    cd agent-module || error "Could not find agent-module directory"
    mvn clean package || error "Maven build failed"
    native-image -jar target/agent-module-1.0-SNAPSHOT-jar-with-dependencies.jar || error "Native image build failed"
    mv agent-module ../agent-mac || error "Failed to move native binary"
    cd ..
}

build_docker() {
    echo -e "${GREEN}Building with Docker Maven...${NC}"
    check_docker
    docker compose --profile build up maven || error "Docker Maven build failed"
}

# Run functions
start_server() {
    echo -e "${GREEN}Starting server in Docker...${NC}"
    check_docker
    docker compose up -d server-module || error "Failed to start server"
    echo -e "${YELLOW}Waiting for server to start...${NC}"
    sleep 5
}

run_local_native() {
    echo -e "${GREEN}Running native macOS agent...${NC}"
    if [ ! -f "./agent-mac" ]; then
        error "Native binary not found. Build it first with --build-local-native"
    fi

    start_server
    export SERVER_URL=http://localhost:8080/metrics
    ./agent-mac
}

run_local_jvm() {
    echo -e "${GREEN}Running JVM agent...${NC}"
    if [ ! -f "./agent-module/target/agent-module-1.0-SNAPSHOT-jar-with-dependencies.jar" ]; then
        error "Agent JAR not found. Build it first with --build-local-jvm"
    fi

    start_server
    export SERVER_URL=http://localhost:8080/metrics
    java -Xms64m -Xmx128m -jar ./agent-module/target/agent-module-1.0-SNAPSHOT-jar-with-dependencies.jar
}

run_docker_jvm() {
    echo -e "${GREEN}Running server and JVM agent in Docker...${NC}"
    check_docker
    docker compose --profile jvm up --build server-module agent-module-jvm
}

run_docker_graal() {
    echo -e "${GREEN}Running server and native agent in Docker...${NC}"
    check_docker
    docker compose up --build server-module agent-module
}

# Main script
if [ $# -eq 0 ]; then
    print_help
    exit 0
fi

BUILD_CMD=""
RUN_CMD=""

# Parse arguments
while [ $# -gt 0 ]; do
    case "$1" in
        --build-local-jvm)
            BUILD_CMD="build_local_jvm"
            ;;
        --build-local-native)
            BUILD_CMD="build_local_native"
            ;;
        --build-docker)
            BUILD_CMD="build_docker"
            ;;
        --run-local-native)
            RUN_CMD="run_local_native"
            ;;
        --run-local-jvm)
            RUN_CMD="run_local_jvm"
            ;;
        --run-docker-jvm)
            RUN_CMD="run_docker_jvm"
            ;;
        --run-docker-graal)
            RUN_CMD="run_docker_graal"
            ;;
        --help|-h)
            print_help
            exit 0
            ;;
        *)
            error "Unknown option: $1"
            ;;
    esac
    shift
done

# Execute commands
if [ -n "$BUILD_CMD" ]; then
    $BUILD_CMD
fi

if [ -n "$RUN_CMD" ]; then
    $RUN_CMD
fi
