#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
CLUSTER_NAME="lh-agent-connector"
OLLAMA_MODEL="${OLLAMA_MODEL:-qwen3:4b}"

clean=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --clean)
      clean=true
      ;;
    *)
      echo "Unknown argument: $1" >&2
      exit 1
      ;;
  esac
  shift
done

if ! command -v kind >/dev/null 2>&1; then
  echo "'kind' command not found. Install https://kind.sigs.k8s.io/." >&2
  exit 1
fi

if ! command -v kubectx >/dev/null 2>&1; then
  echo "'kubectx' command not found. Install https://kubectx.org/." >&2
  exit 1
fi

if [[ "${clean}" == "true" ]]; then
  kind delete cluster --name "${CLUSTER_NAME}"
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

if ! docker info >/dev/null 2>&1; then
  echo "Docker is not running." >&2
  exit 1
fi

kind create cluster --name "${CLUSTER_NAME}" --config "${SCRIPT_DIR}/kind.yaml" -q || true

kubectx "kind-${CLUSTER_NAME}"

kubectl apply -f "${SCRIPT_DIR}/namespace.yaml"
kubectl config set-context --current --namespace=littlehorse
kubectl apply -f "${SCRIPT_DIR}/littlehorse.yaml"
kubectl apply -f "${SCRIPT_DIR}/ollama.yaml"
kubectl rollout status deployment/littlehorse --namespace littlehorse --timeout=5m
kubectl rollout status deployment/ollama --namespace littlehorse --timeout=5m

if ! kubectl exec deployment/ollama --namespace littlehorse -- \
  ollama show "${OLLAMA_MODEL}" >/dev/null 2>&1; then
  echo "Downloading Ollama model ${OLLAMA_MODEL}."
  kubectl exec deployment/ollama --namespace littlehorse -- \
    ollama pull "${OLLAMA_MODEL}"
fi

kubectl exec deployment/ollama --namespace littlehorse -- \
  ollama show "${OLLAMA_MODEL}" >/dev/null

echo "LittleHorse is ready at localhost:2023."
echo "Dashboard: http://localhost:8080"
echo "Ollama is ready at http://localhost:11434 with model ${OLLAMA_MODEL}."
echo "OpenAI-compatible base URL: http://localhost:11434/v1"
