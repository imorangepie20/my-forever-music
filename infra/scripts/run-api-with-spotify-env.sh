#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
API_DIR="${REPO_ROOT}/services/api"
ENV_FILE="${API_DIR}/.env.local"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Missing env file: ${ENV_FILE}" >&2
  echo "Create it from ${API_DIR}/.env.example first." >&2
  exit 1
fi

set -a
source "${ENV_FILE}"
set +a

export PATH="/opt/homebrew/opt/openjdk@21/bin:/opt/homebrew/bin:${PATH}"
export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"

cd "${API_DIR}"
./gradlew bootRun
