#!/usr/bin/env bash
set -euo pipefail

BACKUP_ENV_FILE="${BACKUP_ENV_FILE:-/opt/study-server/.backup.env}"
if [ -f "$BACKUP_ENV_FILE" ]; then
  set -a
  # shellcheck disable=SC1090
  source "$BACKUP_ENV_FILE"
  set +a
fi

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
READABLE_TIME="$(date '+%Y-%m-%d %H:%M:%S %Z')"
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

SHA256_SUM="$(sha256sum "$FINAL_FILE" 2>/dev/null | awk '{print $1}' || shasum -a 256 "$FINAL_FILE" | awk '{print $1}')"
echo "[OK] Backup saved: $FINAL_FILE (${FILE_SIZE} bytes, sha256=${SHA256_SUM})"

if [ -n "${TG_BOT_TOKEN:-}" ] && [ -n "${TG_CHAT_ID:-}" ]; then
  CAPTION=$(printf "基础学习 PostgreSQL 每日备份\n时间：%s\n文件：%s\n大小：%s bytes\nSHA256：%s" \
    "$READABLE_TIME" "$(basename "$FINAL_FILE")" "$FILE_SIZE" "$SHA256_SUM")
  TG_RESP="$(curl -sS --max-time 60 \
    -F "chat_id=${TG_CHAT_ID}" \
    -F "document=@${FINAL_FILE}" \
    -F "caption=${CAPTION}" \
    "https://api.telegram.org/bot${TG_BOT_TOKEN}/sendDocument" 2>&1)" || {
      echo "[ERROR] Telegram upload curl failed (local backup preserved at $FINAL_FILE)" >&2
      exit 1
    }
  if echo "$TG_RESP" | grep -q '"ok":true'; then
    echo "[OK] Telegram backup pushed to chat ${TG_CHAT_ID}"
  else
    echo "[ERROR] Telegram API rejected upload: ${TG_RESP}" >&2
    exit 1
  fi
else
  echo "[INFO] TG_BOT_TOKEN or TG_CHAT_ID not configured; skipped Telegram upload."
fi
