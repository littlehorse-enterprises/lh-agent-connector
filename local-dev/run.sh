#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
PROJECT_DIR=$(cd "${SCRIPT_DIR}/.." && pwd)

native=false
build=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --build)
      build=true
      ;;
    --native)
      native=true
      ;;
    *)
      echo "Unknown argument: $1" >&2
      exit 1
      ;;
  esac
  shift
done

cd "${PROJECT_DIR}"

if [[ "${build}" == "true" ]]; then
  ./local-dev/build.sh
fi

if [[ "${native}" == "true" ]]; then
  ./build/quarkus-run '-Dquarkus.log.category."io.littlehorse.agent".level=DEBUG'
else
  exec java \
    '-Dquarkus.log.category."io.littlehorse.agent".level=DEBUG' \
    -jar ./build/quarkus-app/quarkus-run.jar
fi
