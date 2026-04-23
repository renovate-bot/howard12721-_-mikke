#!/bin/sh
set -eu

service_name="${MIGRATION_SERVICE:-}"

if [ -z "$service_name" ]; then
  echo "MIGRATION_SERVICE is required" >&2
  exit 1
fi

database_name="${MARIADB_DATABASE:-$(printf '%s' "$service_name" | tr '-' '_')}"
migration_dir="/flyway/migrations/$service_name"
connect_retries="${FLYWAY_CONNECT_RETRIES:-60}"

if [ ! -d "$migration_dir" ]; then
  echo "Migration directory not found: $migration_dir" >&2
  exit 1
fi

exec flyway \
  -url="jdbc:mariadb://${MARIADB_HOST:-mariadb}:${MARIADB_PORT:-3306}/${database_name}" \
  -user="${MARIADB_USER}" \
  -password="${MARIADB_PASSWORD}" \
  -connectRetries="${connect_retries}" \
  -locations="filesystem:${migration_dir}" \
  migrate
