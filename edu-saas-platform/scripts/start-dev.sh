#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
API_DIR="$BACKEND_DIR/edu-api"
FRONTEND_DIR="$ROOT_DIR/frontend"
LOG_DIR="$ROOT_DIR/logs/dev"
PID_DIR="$ROOT_DIR/.pids"

WITH_DEPS=0
SKIP_INSTALL=0
BACKEND_ONLY=0
FRONTEND_ONLY=0
KILL_EXISTING=0

usage() {
  cat <<'EOF'
Usage: scripts/start-dev.sh [options]

Options:
  --with-deps       Start MySQL 8 and Redis 7 with Docker containers first.
  --skip-install    Skip Maven/npm dependency install steps.
  --backend-only    Start only the Spring Boot API.
  --frontend-only   Start only the Vite frontend.
  --kill-existing   Stop processes recorded in .pids and free 8080/5173 first.
  -h, --help        Show this help.

Examples:
  scripts/start-dev.sh
  scripts/start-dev.sh --with-deps
  scripts/start-dev.sh --skip-install --kill-existing
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --with-deps) WITH_DEPS=1 ;;
    --skip-install) SKIP_INSTALL=1 ;;
    --backend-only) BACKEND_ONLY=1 ;;
    --frontend-only) FRONTEND_ONLY=1 ;;
    --kill-existing) KILL_EXISTING=1 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage; exit 1 ;;
  esac
  shift
done

if [[ "$BACKEND_ONLY" -eq 1 && "$FRONTEND_ONLY" -eq 1 ]]; then
  echo "Cannot use --backend-only and --frontend-only together." >&2
  exit 1
fi

mkdir -p "$LOG_DIR" "$PID_DIR"

info() {
  printf '\033[1;34m[dev]\033[0m %s\n' "$*"
}

warn() {
  printf '\033[1;33m[dev]\033[0m %s\n' "$*"
}

fail() {
  printf '\033[1;31m[dev]\033[0m %s\n' "$*" >&2
  exit 1
}

need_cmd() {
  if command -v "$1" >/dev/null 2>&1; then
    return
  fi
  case "$1" in
    mvn)
      fail "Missing command: mvn. Install Maven first: brew install maven"
      ;;
    java)
      fail "Missing command: java. Install JDK 21 first: brew install openjdk@21"
      ;;
    docker)
      fail "Missing command: docker. Install Docker Desktop or run without --with-deps after starting MySQL/Redis yourself."
      ;;
    *)
      fail "Missing command: $1"
      ;;
  esac
}

port_pid() {
  lsof -tiTCP:"$1" -sTCP:LISTEN 2>/dev/null | head -n 1 || true
}

stop_pid_file() {
  local file="$1"
  if [[ -f "$file" ]]; then
    local pid
    pid="$(cat "$file")"
    if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
      info "Stopping PID $pid from $file"
      kill "$pid" 2>/dev/null || true
      sleep 1
    fi
    rm -f "$file"
  fi
}

kill_port() {
  local port="$1"
  local pid
  pid="$(port_pid "$port")"
  if [[ -n "$pid" ]]; then
    info "Stopping process $pid on port $port"
    kill "$pid" 2>/dev/null || true
    sleep 1
  fi
}

wait_for_http() {
  local url="$1"
  local name="$2"
  local attempts="${3:-60}"
  local i
  for ((i = 1; i <= attempts; i++)); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      info "$name is ready: $url"
      return 0
    fi
    sleep 2
  done
  warn "$name did not become ready in time: $url"
  return 1
}

wait_for_port() {
  local port="$1"
  local name="$2"
  local attempts="${3:-30}"
  local i
  for ((i = 1; i <= attempts; i++)); do
    if [[ -n "$(port_pid "$port")" ]]; then
      info "$name is listening on port $port"
      return 0
    fi
    sleep 1
  done
  warn "$name did not listen on port $port in time"
  return 1
}

start_docker_container() {
  local name="$1"
  shift
  if docker ps --format '{{.Names}}' | grep -qx "$name"; then
    info "$name is already running"
    return
  fi
  if docker ps -a --format '{{.Names}}' | grep -qx "$name"; then
    info "Starting existing container $name"
    docker start "$name" >/dev/null
    return
  fi
  info "Creating container $name"
  docker run -d --name "$name" "$@" >/dev/null
}

preflight() {
  need_cmd lsof
  need_cmd curl
  need_cmd nc
  need_cmd screen
  if [[ "$FRONTEND_ONLY" -ne 1 ]]; then
    need_cmd java
    need_cmd mvn
    if command -v /usr/libexec/java_home >/dev/null 2>&1; then
      JAVA21_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
      if [[ -n "${JAVA21_HOME:-}" ]]; then
        export JAVA_HOME="$JAVA21_HOME"
        export PATH="$JAVA_HOME/bin:$PATH"
        info "Using Java 21: $JAVA_HOME"
      fi
    fi
  fi
  if [[ "$BACKEND_ONLY" -ne 1 ]]; then
    need_cmd node
    need_cmd npm
  fi
  if [[ "$WITH_DEPS" -eq 1 ]]; then
    need_cmd docker
  fi
}

start_deps() {
  info "Starting MySQL/Redis with Docker"
  start_docker_container mysql-edu \
    -p 3306:3306 \
    -e MYSQL_ROOT_PASSWORD=root \
    -e MYSQL_DATABASE=edu_saas \
    mysql:8.0
  start_docker_container redis-edu \
    -p 6379:6379 \
    redis:7
  info "Waiting for MySQL and Redis"
  for _ in {1..45}; do
    if docker exec mysql-edu mysqladmin ping -uroot -proot --silent >/dev/null 2>&1; then
      break
    fi
    sleep 2
  done
  docker exec mysql-edu mysqladmin ping -uroot -proot --silent >/dev/null 2>&1 || fail "MySQL container is not ready"
  docker exec redis-edu redis-cli ping >/dev/null 2>&1 || fail "Redis container is not ready"
}

check_external_deps() {
  if [[ "$FRONTEND_ONLY" -eq 1 ]]; then
    return
  fi
  if ! nc -z 127.0.0.1 3306 >/dev/null 2>&1; then
    fail "MySQL is not reachable on 127.0.0.1:3306. Start it first or run scripts/start-dev.sh --with-deps"
  fi
  if ! nc -z 127.0.0.1 6379 >/dev/null 2>&1; then
    fail "Redis is not reachable on 127.0.0.1:6379. Start it first or run scripts/start-dev.sh --with-deps"
  fi
}

start_backend() {
  if [[ "$FRONTEND_ONLY" -eq 1 ]]; then
    return
  fi
  local api_pid="$PID_DIR/edu-api.pid"
  if [[ -n "$(port_pid 8080)" ]]; then
    fail "Port 8080 is already in use. Run scripts/stop-dev.sh or scripts/start-dev.sh --kill-existing"
  fi

  if [[ "$SKIP_INSTALL" -ne 1 ]]; then
    info "Building backend modules: mvn clean install -DskipTests"
    (cd "$BACKEND_DIR" && mvn clean install -DskipTests)
  fi

  info "Starting backend API. Log: $LOG_DIR/backend.log"
  screen -S edu-api -X quit 2>/dev/null || true
  if [[ "$WITH_DEPS" -eq 1 ]]; then
    screen -dmS edu-api bash -lc "cd '$API_DIR' && export JAVA_HOME='$JAVA_HOME' && export PATH=\"\$JAVA_HOME/bin:\$PATH\" && export SPRING_PROFILES_ACTIVE=\"\${SPRING_PROFILES_ACTIVE:-private}\" && export DB_URL=\"\${DB_URL:-jdbc:mysql://127.0.0.1:3306/edu_saas?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai}\" && export DB_USERNAME=\"\${DB_USERNAME:-root}\" && export DB_PASSWORD=\"\${DB_PASSWORD:-root}\" && export REDIS_HOST=\"\${REDIS_HOST:-127.0.0.1}\" && export REDIS_PORT=\"\${REDIS_PORT:-6379}\" && export JWT_SECRET=\"\${JWT_SECRET:-dev-local-jwt-secret-change-me-dev-local-jwt-secret}\" && export PAYMENT_CALLBACK_SECRET=\"\${PAYMENT_CALLBACK_SECRET:-dev-local-payment-secret}\" && mvn spring-boot:run > '$LOG_DIR/backend.log' 2>&1"
  else
    screen -dmS edu-api bash -lc "cd '$API_DIR' && export JAVA_HOME='$JAVA_HOME' && export PATH=\"\$JAVA_HOME/bin:\$PATH\" && mvn spring-boot:run > '$LOG_DIR/backend.log' 2>&1"
  fi
  echo "screen:edu-api" > "$api_pid"
  wait_for_http "http://127.0.0.1:8080/actuator/health" "Backend API" 90 || {
    warn "Backend log tail:"
    tail -n 80 "$LOG_DIR/backend.log" || true
  }
}

start_frontend() {
  if [[ "$BACKEND_ONLY" -eq 1 ]]; then
    return
  fi
  local frontend_pid="$PID_DIR/frontend.pid"
  if [[ -n "$(port_pid 5173)" ]]; then
    fail "Port 5173 is already in use. Run scripts/stop-dev.sh or scripts/start-dev.sh --kill-existing"
  fi

  if [[ "$SKIP_INSTALL" -ne 1 ]]; then
    if [[ ! -d "$FRONTEND_DIR/node_modules" ]]; then
      info "Installing frontend dependencies: npm install"
      (cd "$FRONTEND_DIR" && npm install)
    else
      info "Frontend node_modules exists; skipping npm install. Use npm install manually after dependency changes."
      # npm install
    fi
  fi

  info "Starting frontend. Log: $LOG_DIR/frontend.log"
  screen -S edu-frontend -X quit 2>/dev/null || true
  screen -dmS edu-frontend bash -lc "cd '$FRONTEND_DIR' && npm run dev -- --host 127.0.0.1 > '$LOG_DIR/frontend.log' 2>&1"
  echo "screen:edu-frontend" > "$frontend_pid"
  wait_for_port 5173 "Frontend" 30 || {
    warn "Frontend log tail:"
    tail -n 80 "$LOG_DIR/frontend.log" || true
  }
}

main() {
  preflight
  if [[ "$KILL_EXISTING" -eq 1 ]]; then
    stop_pid_file "$PID_DIR/edu-api.pid"
    stop_pid_file "$PID_DIR/frontend.pid"
    kill_port 8080
    kill_port 5173
  fi
  if [[ "$WITH_DEPS" -eq 1 ]]; then
    start_deps
  else
    check_external_deps
  fi
  start_backend
  start_frontend

  info "Done."
  if [[ "$FRONTEND_ONLY" -ne 1 ]]; then
    info "Backend:  http://127.0.0.1:8080"
    info "Swagger:  http://127.0.0.1:8080/swagger-ui.html"
  fi
  if [[ "$BACKEND_ONLY" -ne 1 ]]; then
    info "Frontend: http://127.0.0.1:5173"
  fi
  info "Logs:     $LOG_DIR"
  info "Stop:     scripts/stop-dev.sh"
}

main
