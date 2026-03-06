#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
GENERATED_DIR="${PROJECT_ROOT}/build/generated/openapi-client"
POM_FILE="${GENERATED_DIR}/pom.xml"

if [[ ! -f "${POM_FILE}" ]]; then
	echo "ERROR: Generated pom.xml not found at ${POM_FILE}" >&2
	echo "ERROR: Run generateJavaClient task first." >&2
	exit 1
fi

mvn install \
	--file "${POM_FILE}" \
	--batch-mode \
	--quiet \
	-DskipTests
