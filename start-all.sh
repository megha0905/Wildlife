#!/usr/bin/env bash
# Starts PostgreSQL, all 10 backend apps (in the background) and tells you how to start the frontend.
# Usage (from the repo root):  bash start-all.sh
set -e
cd "$(dirname "$0")"

if [ ! -f .env ]; then cp .env.example .env; echo "Created .env from .env.example"; fi
set -a; source .env; set +a
if [ ${#JWT_SECRET} -lt 32 ]; then echo "JWT_SECRET in .env must be at least 32 characters"; exit 1; fi

echo "Starting PostgreSQL..."
docker compose up -d
until docker compose ps | grep -q healthy; do sleep 2; done
echo "PostgreSQL is healthy."

mkdir -p logs
for s in auth-service habitat-service wildlife-service resource-service patrol-service \
         incident-service rescue-service programme-service reporting-service api-gateway; do
  echo "Starting $s (log: logs/$s.log)"
  (cd "$s" && nohup mvn -q spring-boot:run -Dspring-boot.run.jvmArguments="-Xmx300m" > "../logs/$s.log" 2>&1 &)
done

echo
echo "The first start downloads libraries and can take 5-10 minutes."
echo "Check progress with:   bash status.sh"
echo "Then start the frontend in a new terminal:   cd frontend && npm install && npm run dev"
