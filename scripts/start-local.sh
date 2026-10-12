#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_DIR"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is not installed. Install Docker Desktop, open it, then run this script again." >&2
  exit 1
fi
if ! docker compose version >/dev/null 2>&1; then
  echo "Docker Compose is unavailable. Update Docker Desktop and try again." >&2
  exit 1
fi
if ! docker info >/dev/null 2>&1; then
  echo "Docker is installed but not running. Open Docker Desktop and wait until it is ready." >&2
  exit 1
fi

created_env=false
if [[ ! -f .env ]]; then
  if [[ ! -f .env.example ]]; then
    echo "Missing .env.example; this project copy is incomplete." >&2
    exit 1
  fi
  if ! command -v openssl >/dev/null 2>&1; then
    echo "OpenSSL is required to generate secure local secrets." >&2
    exit 1
  fi
  umask 077
  db_password="$(openssl rand -hex 24)"
  root_password="$(openssl rand -hex 24)"
  invite_code="$(openssl rand -hex 16)"
  awk -v db="$db_password" -v root="$root_password" -v invite="$invite_code" '
    /^DB_PASSWORD=/ { print "DB_PASSWORD=" db; next }
    /^MYSQL_ROOT_PASSWORD=/ { print "MYSQL_ROOT_PASSWORD=" root; next }
    /^STAFF_INVITE_CODE=/ { print "STAFF_INVITE_CODE=" invite; next }
    { print }
  ' .env.example > .env.tmp
  mv .env.tmp .env
  chmod 600 .env
  created_env=true
fi

docker compose up --build -d
web_port="$(awk -F= '$1 == "WEB_PORT" { print $2; exit }' .env)"
web_port="${web_port:-8080}"
host_port="${web_port##*:}"
printf '\nSmart Hostel is starting. Open http://localhost:%s after the containers become healthy.\n' "$host_port"
if [[ "$created_env" == true ]]; then
  printf 'First staff invite code: %s\n' "$invite_code"
  echo "Keep this code private. The local credentials are stored in .env."
fi
