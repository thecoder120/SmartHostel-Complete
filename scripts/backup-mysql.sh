#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
backup_dir="${BACKUP_DIR:-$project_dir/backups}"
stamp="$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p "$backup_dir"

docker compose -f "$project_dir/docker-compose.yml" exec -T mysql sh -c \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysqldump --single-transaction --routines --triggers -uroot smarthostel' \
  | gzip -c > "$backup_dir/smarthostel-$stamp.sql.gz"

echo "Created $backup_dir/smarthostel-$stamp.sql.gz"
