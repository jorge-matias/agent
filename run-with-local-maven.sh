#!/bin/bash
mvn clean package
docker compose --profile default up --build

