#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
BACKUP_DIR="${BACKUP_DIR:-$PROJECT_DIR/backups}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/medexpertmatch_$TIMESTAMP.dump"

PG_HOST="${PG_HOST:-localhost}"
PG_PORT="${PG_PORT:-5433}"
PG_USER="${PG_USER:-medexpertmatch}"
PG_DB="${PG_DB:-medexpertmatch}"
PGPASSWORD="${PGPASSWORD:-medexpertmatch}"

mkdir -p "$BACKUP_DIR"

echo "Backing up $PG_DB to $BACKUP_FILE ..."
PGPASSWORD="$PGPASSWORD" pg_dump -h "$PG_HOST" -p "$PG_PORT" -U "$PG_USER" -d "$PG_DB" \
  -Fc --no-owner --no-acl > "$BACKUP_FILE"

echo "Backup complete: $(du -h "$BACKUP_FILE" | cut -f1)"
echo "Restore: pg_restore -h $PG_HOST -p $PG_PORT -U $PG_USER -d $PG_DB $BACKUP_FILE"
