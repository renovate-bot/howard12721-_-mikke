#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${BACKEND_DIR}/compose.debug.yml"

readonly BACKEND_DIR COMPOSE_FILE

compose() {
  docker compose -f "${COMPOSE_FILE}" "$@"
}

usage() {
  cat <<'EOF'
Usage:
  ./scripts/debug-compose.sh full [--migrate]
  ./scripts/debug-compose.sh sync [--migrate] <service...>

Commands:
  full
    Build and start the whole debug stack.

  sync
    Rebuild and restart only the specified services without touching their
    compose dependencies. This is the fast path for day-to-day development.

Options:
  --migrate
    Run Flyway for the affected services before starting them.

Examples:
  ./scripts/debug-compose.sh full --migrate
  ./scripts/debug-compose.sh sync post-service
  ./scripts/debug-compose.sh sync --migrate api post-service
EOF
}

is_valid_service() {
  case "$1" in
    api|identity-service|friendship-service|post-service|media-service|guess-service|feed-service|notification-service)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

flyway_service_for() {
  case "$1" in
    identity-service) echo "flyway-identity-service" ;;
    friendship-service) echo "flyway-friendship-service" ;;
    post-service) echo "flyway-post-service" ;;
    media-service) echo "flyway-media-service" ;;
    guess-service) echo "flyway-guess-service" ;;
    feed-service) echo "flyway-feed-service" ;;
    notification-service) echo "flyway-notification-service" ;;
    *) return 1 ;;
  esac
}

start_infra() {
  compose up -d mariadb redis
}

run_migrations_for() {
  local service
  local flyway_service

  for service in "$@"; do
    if flyway_service="$(flyway_service_for "${service}" 2>/dev/null)"; then
      compose --profile migration run --rm "${flyway_service}"
    fi
  done
}

run_all_migrations() {
  run_migrations_for \
    identity-service \
    friendship-service \
    post-service \
    media-service \
    guess-service \
    feed-service \
    notification-service
}

main() {
  local command="${1:-}"
  local migrate=false
  local -a services=()
  local service

  if [[ -z "${command}" ]]; then
    usage
    exit 1
  fi
  if [[ "${command}" == "-h" || "${command}" == "--help" ]]; then
    usage
    exit 0
  fi
  shift

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --migrate)
        migrate=true
        ;;
      -h|--help)
        usage
        exit 0
        ;;
      *)
        services+=("$1")
        ;;
    esac
    shift
  done

  if [[ ${#services[@]} -gt 0 ]]; then
    for service in "${services[@]}"; do
      if ! is_valid_service "${service}"; then
        echo "Unknown service: ${service}" >&2
        exit 1
      fi
    done
  fi

  cd "${BACKEND_DIR}"

  case "${command}" in
    full)
      start_infra
      if [[ "${migrate}" == "true" ]]; then
        run_all_migrations
      fi
      compose up -d --build
      ;;
    sync)
      if [[ ${#services[@]} -eq 0 ]]; then
        echo "sync requires at least one service." >&2
        exit 1
      fi
      start_infra
      if [[ "${migrate}" == "true" ]]; then
        run_migrations_for "${services[@]}"
      fi
      compose build "${services[@]}"
      compose up -d --no-deps "${services[@]}"
      ;;
    *)
      usage
      exit 1
      ;;
  esac
}

main "$@"
