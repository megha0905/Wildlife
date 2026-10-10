#!/usr/bin/env bash
# Stops all backend apps and PostgreSQL. Usage: bash stop-all.sh
pkill -f 'spring-boot:run' || true
pkill -f 'com.wildlife' || true
docker compose stop
echo "Stopped."
