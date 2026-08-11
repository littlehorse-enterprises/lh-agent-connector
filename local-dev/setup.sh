#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
CLUSTER_NAME="lh-agent-connector"
OLLAMA_MODEL="${OLLAMA_MODEL:-qwen3:4b}"

clean=false
stop_ollama=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --clean)
      clean=true
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

if [[ "${stop_ollama}" == "true" && "${clean}" != "true" ]]; then
  echo "--stop-ollama must be used with --clean." >&2
  exit 1
fi

if ! command -v kind >/dev/null 2>&1; then
  echo "'kind' command not found. Install https://kind.sigs.k8s.io/." >&2
  exit 1
fi

if ! command -v kubectx >/dev/null 2>&1; then
  echo "'kubectx' command not found. Install https://kubectx.org/." >&2
  exit 1
fi

if [[ "${clean}" == "true" ]]; then
  if [[ "${stop_ollama}" == "true" ]] && ! command -v brew >/dev/null 2>&1; then
    echo "'brew' command not found. Cannot stop the Homebrew Ollama service." >&2
    exit 1
  fi

  kind delete cluster --name "${CLUSTER_NAME}"

  if [[ "${stop_ollama}" == "true" ]]; then
    brew services stop ollama
  fi

  exit 0
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "'docker' command not found. Install https://docs.docker.com/engine/install/." >&2
  exit 1
fi

if ! command -v kubectl >/dev/null 2>&1; then
  echo "'kubectl' command not found. Install https://kubernetes.io/docs/tasks/tools/." >&2
  exit 1
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

kind create cluster --name "${CLUSTER_NAME}" --config "${SCRIPT_DIR}/kind.yaml" -q || true

kubectx "kind-${CLUSTER_NAME}"

kubectl apply -f "${SCRIPT_DIR}/namespace.yaml"
kubectl config set-context --current --namespace=littlehorse
kubectl apply -f "${SCRIPT_DIR}/littlehorse.yaml"
kubectl rollout status deployment/littlehorse --namespace littlehorse --timeout=5m

if ! ollama show "${OLLAMA_MODEL}" >/dev/null 2>&1; then
  echo "Downloading local Ollama model ${OLLAMA_MODEL}."
  ollama pull "${OLLAMA_MODEL}"
fi

ollama show "${OLLAMA_MODEL}" >/dev/null

echo "LittleHorse is ready at localhost:2023."
echo "Dashboard: http://localhost:8080"
echo "Local Ollama is ready at http://localhost:11434 with model ${OLLAMA_MODEL}."
echo "OpenAI-compatible base URL: http://localhost:11434/v1"
