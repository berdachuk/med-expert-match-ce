#!/bin/bash

# Rebuild MedExpertMatch Docker images
# This script rebuilds all Docker images for the MedExpertMatch application

set -e

# Configuration
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="${PROJECT_ROOT}/docker-compose.yml"
COMPOSE_DEV_FILE="${PROJECT_ROOT}/docker-compose.dev.yml"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Rebuilding MedExpertMatch Docker Images ===${NC}"
echo ""

# Check if Docker is installed and running
if ! command -v docker &> /dev/null; then
    echo -e "${RED}Docker is not installed or not in PATH${NC}"
    exit 1
fi

if ! docker info &> /dev/null; then
    echo -e "${RED}Docker daemon is not running${NC}"
    exit 1
fi

echo -e "${YELLOW}Stopping existing containers...${NC}"
# Stop and remove existing containers
docker compose -f "${COMPOSE_FILE}" down --remove-orphans 2>/dev/null || true
docker compose -f "${COMPOSE_DEV_FILE}" down --remove-orphans 2>/dev/null || true

echo -e "${YELLOW}Removing old images...${NC}"
# Remove old images if they exist
docker rmi medexpertmatch-app:latest 2>/dev/null || true
docker rmi medexpertmatch-postgres:latest 2>/dev/null || true
docker rmi medexpertmatch-postgres-dev:latest 2>/dev/null || true

echo -e "${YELLOW}Building images...${NC}"
# Build all images
echo "Building main application stack..."
docker compose -f "${COMPOSE_FILE}" build --no-cache

echo "Building development database..."
docker compose -f "${COMPOSE_DEV_FILE}" build --no-cache

echo -e "${GREEN}Docker images rebuilt successfully!${NC}"
echo ""
echo -e "${YELLOW}To start the main stack:${NC}"
echo "  docker compose -f docker-compose.yml up -d"
echo ""
echo -e "${YELLOW}To start the development database:${NC}"
echo "  docker compose -f docker-compose.dev.yml up -d"
echo ""
echo -e "${GREEN}=== Rebuild Complete ===${NC}"