#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
PROJECT_DIR=$(cd "${SCRIPT_DIR}/.." && pwd)
COMPOSE_FILE="${SCRIPT_DIR}/compose.yaml"
COMPOSE_PROJECT_NAME="lh-agent-connector"
GRADLE_PROPERTIES="${PROJECT_DIR}/gradle.properties"
OLLAMA_MODEL="${OLLAMA_MODEL:-qwen3:4b}"

clean=false
configure_lhctl=false
stop_ollama=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --clean)
      clean=true
      ;;
    --lhctl)
      configure_lhctl=true
      ;;
    --stop-ollama)
      stop_ollama=true
      ;;
    *)
      echo "Unknown argument: $1" >&2
      exit 1
      ;;
  esac
  shift
done

if [[ "${configure_lhctl}" == "true" && ("${clean}" == "true" || "${stop_ollama}" == "true") ]]; then
  echo "--lhctl cannot be combined with --clean or --stop-ollama." >&2
  exit 1
fi

if [[ "${stop_ollama}" == "true" && "${clean}" != "true" ]]; then
  echo "--stop-ollama must be used with --clean." >&2
  exit 1
fi

if [[ "${configure_lhctl}" == "true" ]]; then
  lhctl_config_dir="${HOME}/.config"
  lhctl_config_file="${lhctl_config_dir}/littlehorse.config"

  mkdir -p "${lhctl_config_dir}"
  if [[ -f "${lhctl_config_file}" ]]; then
    cp "${lhctl_config_file}" "${lhctl_config_file}.backup"
    echo "Backed up existing littlehorse.config to ${lhctl_config_file}.backup."
  fi

  cat >"${lhctl_config_file}" <<EOF
LHC_API_HOST=localhost
LHC_API_PORT=2023
EOF

  echo "Configured lhctl with ${lhctl_config_file}."
  exit 0
fi

if [[ ! -f "${GRADLE_PROPERTIES}" ]]; then
  echo "Gradle properties file not found: ${GRADLE_PROPERTIES}" >&2
  exit 1
fi

LITTLEHORSE_VERSION=$(sed -n 's/^version=//p' "${GRADLE_PROPERTIES}")
if [[ -z "${LITTLEHORSE_VERSION}" || "${LITTLEHORSE_VERSION}" == *$'\n'* ]]; then
  echo "Expected exactly one non-empty version property in ${GRADLE_PROPERTIES}." >&2
  exit 1
fi

run_compose() {
  LH_VERSION="${LITTLEHORSE_VERSION}" docker compose \
    --project-name "${COMPOSE_PROJECT_NAME}" \
    --file "${COMPOSE_FILE}" \
    "$@"
}

if ! command -v docker >/dev/null 2>&1; then
  echo "'docker' command not found. Install https://docs.docker.com/engine/install/." >&2
  exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
  echo "The Docker Compose plugin is not available. Install https://docs.docker.com/compose/install/." >&2
  exit 1
fi

if [[ "${clean}" == "true" ]]; then
  if [[ "${stop_ollama}" == "true" ]] && ! command -v brew >/dev/null 2>&1; then
    echo "'brew' command not found. Cannot stop the Homebrew Ollama service." >&2
    exit 1
  fi

  run_compose down --volumes --remove-orphans

  if [[ "${stop_ollama}" == "true" ]]; then
    brew services stop ollama
  fi

  exit 0
fi

if ! command -v ollama >/dev/null 2>&1; then
  echo "'ollama' command not found. Install it with: brew install ollama" >&2
  exit 1
fi

if ! command -v curl >/dev/null 2>&1; then
  echo "'curl' command not found. Install it before running local setup." >&2
  exit 1
fi

if ! docker info >/dev/null 2>&1; then
  echo "Docker is not running." >&2
  exit 1
fi

if ! curl --fail --silent --max-time 5 http://localhost:11434/api/tags >/dev/null; then
  if ! command -v brew >/dev/null 2>&1; then
    echo "'brew' command not found. Cannot start the installed Ollama service." >&2
    exit 1
  fi

  echo "Starting Ollama with Homebrew."
  brew services start ollama

  for _ in {1..30}; do
    if curl --fail --silent --max-time 2 http://localhost:11434/api/tags >/dev/null; then
      break
    fi
    sleep 1
  done

  if ! curl --fail --silent --max-time 5 http://localhost:11434/api/tags >/dev/null; then
    echo "Ollama did not become ready at http://localhost:11434." >&2
    exit 1
  fi
fi

run_compose up --detach --wait --wait-timeout 300

if ! ollama show "${OLLAMA_MODEL}" >/dev/null 2>&1; then
  echo "Downloading local Ollama model ${OLLAMA_MODEL}."
  ollama pull "${OLLAMA_MODEL}"
fi

ollama show "${OLLAMA_MODEL}" >/dev/null

echo "LittleHorse is ready at localhost:2023."
echo "Dashboard: http://localhost:8080"
echo "Local Ollama is ready at http://localhost:11434 with model ${OLLAMA_MODEL}."
echo "OpenAI-compatible base URL: http://localhost:11434/v1"
