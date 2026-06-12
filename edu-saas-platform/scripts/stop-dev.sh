#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PID_DIR="$ROOT_DIR/.pids"

STOP_DEPS=0

usage() {
  cat <<'EOF'
Usage: scripts/stop-dev.sh [options]

Options:
  --deps       Also stop Docker containers mysql-edu and redis-edu.
  -h, --help   Show this help.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --deps) STOP_DEPS=1 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage; exit 1 ;;
  esac
  shift
done

info() {
  printf '\033[1;34m[dev]\033[0m %s\n' "$*"
}

stop_pid_file() {
  local file="$1"
  local name="$2"
  if [[ ! -f "$file" ]]; then
    info "$name PID file not found: $file"
    return
  fi

  local pid
  pid="$(cat "$file")"
  if [[ -z "$pid" ]]; then
    rm -f "$file"
    return
  fi

  if kill -0 "$pid" 2>/dev/null; then
    info "Stopping $name PID $pid"
    kill "$pid" 2>/dev/null || true
    for _ in {1..20}; do
      if ! kill -0 "$pid" 2>/dev/null; then
        break
      fi
      sleep 0.5
    done
    if kill -0 "$pid" 2>/dev/null; then
      info "Force stopping $name PID $pid"
      kill -9 "$pid" 2>/dev/null || true
    fi
  else
    info "$name PID $pid is not running"
  fi
  rm -f "$file"
}

stop_container() {
  local name="$1"
  if ! command -v docker >/dev/null 2>&1; then
    info "Docker is not installed; skip $name"
    return
  fi
  if docker ps --format '{{.Names}}' | grep -qx "$name"; then
    info "Stopping container $name"
    docker stop "$name" >/dev/null
  else
    info "Container $name is not running"
  fi
}

mkdir -p "$PID_DIR"

stop_pid_file "$PID_DIR/edu-api.pid" "edu-api"
stop_pid_file "$PID_DIR/frontend.pid" "frontend"

if [[ "$STOP_DEPS" -eq 1 ]]; then
  stop_container mysql-edu
  stop_container redis-edu
fi

info "Stopped development processes."
