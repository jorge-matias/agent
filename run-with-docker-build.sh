#!/bin/bash
docker compose --profile build up maven
docker compose --profile default up --build

