#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly SCRIPT_DIR

PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
readonly PROJECT_ROOT

readonly GENERATED_DIR="${PROJECT_ROOT}/build/generated/openapi-client"
readonly POM_FILE="${GENERATED_DIR}/pom.xml"
readonly MAVEN_SETTINGS_FILE="${PROJECT_ROOT}/.maven-settings.xml"
readonly REQUIRED_ENV_VARS=("GITHUB_ACTOR" "GITHUB_TOKEN" "GITHUB_REPOSITORY")

log_info() { echo "[INFO]  $*"; }
log_warn() { echo "[WARN]  $*" >&2; }
log_error() { echo "[ERROR] $*" >&2; }

validate_environment() {
	log_info "Validating environment..."

	for var in "${REQUIRED_ENV_VARS[@]}"; do
		if [[ -z "${!var:-}" ]]; then
			log_error "Required environment variable '${var}' is not set."
			exit 1
		fi
	done

	if [[ ! -f "${POM_FILE}" ]]; then
		log_error "Generated pom.xml not found at '${POM_FILE}'."
		log_error "Run './gradlew generateJavaClient' first."
		exit 1
	fi

	if ! command -v mvn >/dev/null 2>&1; then
		log_error "Maven (mvn) is not installed or not on PATH."
		exit 1
	fi

	log_info "Environment validation passed."
}

validate_token_format() {
	local token="${GITHUB_TOKEN}"

	if [[ ${#token} -lt 20 ]]; then
		log_error "GITHUB_TOKEN appears to be invalid (too short)."
		exit 1
	fi

	if [[ ! "${token}" =~ ^(ghp_|ghs_|github_pat_|gho_) ]]; then
		log_warn "GITHUB_TOKEN does not match expected GitHub token format."
		log_warn "Proceeding, but authentication may fail."
	fi
}

generate_maven_settings() {
	log_info "Generating temporary Maven settings..."

	cat >"${MAVEN_SETTINGS_FILE}" <<EOF
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                              https://maven.apache.org/xsd/settings-1.0.0.xsd">
  <servers>
    <server>
      <id>github</id>
      <username>${GITHUB_ACTOR}</username>
      <password>${GITHUB_TOKEN}</password>
    </server>
  </servers>
</settings>
EOF

	chmod 600 "${MAVEN_SETTINGS_FILE}"
	log_info "Maven settings generated at '${MAVEN_SETTINGS_FILE}'."
}

cleanup_maven_settings() {
	if [[ -f "${MAVEN_SETTINGS_FILE}" ]]; then
		rm -f "${MAVEN_SETTINGS_FILE}"
		log_info "Temporary Maven settings removed."
	fi
}

check_already_published() {
	local repository_url="https://maven.pkg.github.com/${GITHUB_REPOSITORY}"
	local group_path
	group_path=$(grep -m1 "<groupId>" "${POM_FILE}" | sed 's/.*<groupId>\(.*\)<\/groupId>.*/\1/' | tr '.' '/')

	local artifact_id
	artifact_id=$(grep -m1 "<artifactId>" "${POM_FILE}" | sed 's/.*<artifactId>\(.*\)<\/artifactId>.*/\1/')

	local version
	version=$(grep -m1 "<version>" "${POM_FILE}" | sed 's/.*<version>\(.*\)<\/version>.*/\1/')

	local status_code
	status_code=$(curl -s -o /dev/null -w "%{http_code}" \
		-H "Authorization: Bearer ${GITHUB_TOKEN}" \
		-H "Accept: application/vnd.github+json" \
		"${repository_url}/${group_path}/${artifact_id}/${version}/${artifact_id}-${version}.pom")

	if [[ "${status_code}" == "200" ]]; then
		log_info "Artifact ${artifact_id}:${version} already exists in GitHub Packages. Skipping publish."
		return 0
	fi

	return 1
}

publish() {
	local repository_url
	repository_url="https://maven.pkg.github.com/${GITHUB_REPOSITORY}"

	if check_already_published; then
		return 0
	fi

	log_info "Publishing generated client to GitHub Packages..."
	log_info "Repository : ${repository_url}"
	log_info "Actor      : ${GITHUB_ACTOR}"

	local output
	local exit_code=0

	output=$(mvn deploy \
		--file "${POM_FILE}" \
		--settings "${MAVEN_SETTINGS_FILE}" \
		--batch-mode \
		-DskipTests \
		-DaltDeploymentRepository="github::${repository_url}" 2>&1) || exit_code=$?

	if [[ ${exit_code} -ne 0 ]]; then
		if echo "${output}" | grep -q "status code: 409"; then
			log_info "Artifact already exists in GitHub Packages (409 Conflict). Skipping publish."
			return 0
		fi
		log_error "Publish failed with exit code ${exit_code}."
		echo "${output}" >&2
		exit "${exit_code}"
	fi

	log_info "Client published successfully."
}

main() {
	trap cleanup_maven_settings EXIT

	validate_environment
	validate_token_format
	generate_maven_settings
	publish
}

main "$@"
