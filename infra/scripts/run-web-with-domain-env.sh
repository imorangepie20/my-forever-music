#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
WEB_DIR="${REPO_ROOT}/apps/web"
ENV_FILE="${WEB_DIR}/.env.local"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Missing env file: ${ENV_FILE}" >&2
  echo "Create it from ${WEB_DIR}/.env.example first." >&2
  exit 1
fi

set -a
source "${ENV_FILE}"
set +a

cd "${WEB_DIR}"
npm run dev -- --host 0.0.0.0
