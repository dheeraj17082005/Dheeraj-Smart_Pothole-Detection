#!/usr/bin/env bash
set -e

echo "=================================================="
echo "           PotholeX - Quick Start"
echo "=================================================="

# 1. Check Prerequisites
command -v docker >/dev/null 2>&1 || { echo "Error: docker is required but not installed." >&2; exit 1; }
command -v docker-compose >/dev/null 2>&1 || command -v docker >/dev/null 2>&1 || { echo "Error: docker-compose is required." >&2; exit 1; }

# 2. Environment File Setup
if [ ! -f .env ]; then
  if [ -f .env.example ]; then
    echo "[INFO] Copying .env.example to .env..."
    cp .env.example .env
  else
    echo "[WARN] No .env.example found, creating default .env..."
    cat << 'ENVEOF' > .env
PORT=80
BACKEND_PORT=8080
AI_SERVICE_PORT=8000
POSTGRES_PORT=5432
MINIO_PORT=9000
MINIO_CONSOLE_PORT=9001
JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
ENVEOF
  fi
fi

# 3. Clean Container Conflicts
echo "[INFO] Ensuring clean container environment..."
docker rm -f pothole_postgres pothole_minio pothole_backend pothole_ai_service pothole_frontend >/dev/null 2>&1 || true

# 4. Build and Start Application Services
echo "[INFO] Launching PotholeX services via Docker Compose..."
docker-compose up --build -d

# 5. Service Readiness Check
echo "[INFO] Waiting for service health checks to pass..."
for i in {1..30}; do
  if curl -s http://localhost:8080/actuator/health | grep -q "UP" 2>/dev/null; then
    echo "[SUCCESS] Backend service is healthy!"
    break
  fi
  sleep 2
done

echo "=================================================="
echo "          PotholeX is Ready to Use!"
echo "=================================================="
echo " Frontend Web App:  http://localhost"
echo " Backend REST API:  http://localhost:8080"
echo " AI FastAPI Specs: http://localhost:8000/docs"
echo " MinIO Storage:     http://localhost:9001"
echo "=================================================="
