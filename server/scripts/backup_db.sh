#!/usr/bin/env bash
set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-/var/backups/study-postgres}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"
CONTAINER_NAME="${CONTAINER_NAME:-study_postgres}"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-5433}"
DB_USER="${DB_USER:-study}"
DB_NAME="${DB_NAME:-study_learning}"

mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"

TIMESTAMP="$(date '+%Y%m%d_%H%M%S')"
TMP_FILE="$BACKUP_DIR/.study_learning_${TIMESTAMP}.sql.gz.tmp"
FINAL_FILE="$BACKUP_DIR/study_learning_${TIMESTAMP}.sql.gz"

docker exec "$CONTAINER_NAME" pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" --no-owner --no-privileges \
  | gzip -9 > "$TMP_FILE"

gzip -t "$TMP_FILE"
FILE_SIZE="$(wc -c < "$TMP_FILE" | tr -d ' ')"
if [ "$FILE_SIZE" -lt 200 ]; then
  echo "[ERROR] Backup file too small (${FILE_SIZE} bytes), aborting." >&2
  rm -f "$TMP_FILE"
  exit 1
fi

mv "$TMP_FILE" "$FINAL_FILE"
chmod 600 "$FINAL_FILE"

find "$BACKUP_DIR" -maxdepth 1 -type f -name 'study_learning_*.sql.gz' -mtime +"$RETENTION_DAYS" -delete

echo "[OK] Backup saved: $FINAL_FILE (${FILE_SIZE} bytes)"
