#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly SCRIPT_DIR

PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
readonly PROJECT_ROOT

readonly GENERATED_DIR="${PROJECT_ROOT}/build/generated/openapi-client"
readonly POM_FILE="${GENERATED_DIR}/pom.xml"
readonly LOCAL_VERSION="0.0.0-LOCAL-SNAPSHOT"

log_info() { echo "[INFO]  $*"; }
log_error() { echo "[ERROR] $*" >&2; }

validate_prerequisites() {
	log_info "Validating prerequisites..."

	if [[ ! -f "${POM_FILE}" ]]; then
		log_error "Generated pom.xml not found at '${POM_FILE}'."
		log_error "Run './gradlew generateJavaClient' first."
		exit 1
	fi

	if ! command -v mvn >/dev/null 2>&1; then
		log_error "Maven (mvn) is not installed or not on PATH."
		exit 1
	fi

	log_info "Prerequisites validated."
}

set_pom_version() {
	log_info "Setting pom.xml version to '${LOCAL_VERSION}'..."

	mvn versions:set \
		-DnewVersion="${LOCAL_VERSION}" \
		--file "${POM_FILE}" \
		--batch-mode \
		--quiet

	mvn versions:commit \
		--file "${POM_FILE}" \
		--batch-mode \
		--quiet

	log_info "pom.xml version set and committed."
}

install_to_local_repository() {
	log_info "Installing generated client to Maven local repository..."

	mvn install \
		--file "${POM_FILE}" \
		--batch-mode \
		--quiet \
		-DskipTests

	log_info "Client installed successfully as version '${LOCAL_VERSION}'."
	log_info "Ensure your build.gradle dependency resolves to: com.devikapps:vaikaparts-gen:${LOCAL_VERSION}"
}

main() {
	validate_prerequisites
	set_pom_version
	install_to_local_repository
}

main "$@"
