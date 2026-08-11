#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
PROJECT_DIR=$(cd "${SCRIPT_DIR}/.." && pwd)
REPOSITORY="littlehorse/lh-agent-connector"
TAG="latest"

if [[ $# -gt 0 ]]; then
  echo "Usage: $0" >&2
  exit 1
fi

cd "${PROJECT_DIR}"

./gradlew -x check build
./gradlew -x check build \
  -Dquarkus.native.enabled=true \
  -Dquarkus.package.runner-suffix=-run \
  -Dquarkus.package.output-name=quarkus \
  -Dquarkus.package.jar.enabled=false \
  -Dquarkus.native.container-build=true \
  -Dquarkus.native.builder-image=quay.io/quarkus/ubi9-quarkus-mandrel-builder-image:jdk-25

docker build --tag "${REPOSITORY}:${TAG}" .
