#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly SCRIPT_DIR

PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
readonly PROJECT_ROOT

readonly GENERATED_DIR="${PROJECT_ROOT}/build/generated/openapi-client"
readonly POM_FILE="${GENERATED_DIR}/pom.xml"
readonly MAVEN_SETTINGS_FILE="${PROJECT_ROOT}/.maven-settings.xml"
readonly VERSION_OUTPUT_FILE="${PROJECT_ROOT}/build/published-client-version.txt"
readonly REQUIRED_ENV_VARS=("GITHUB_ACTOR" "GITHUB_TOKEN" "GITHUB_REPOSITORY")

readonly GROUP_ID="com.devikapps"
readonly ARTIFACT_ID="vaikaparts-gen"
readonly INITIAL_VERSION="1.0.0"

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

	if ! command -v curl >/dev/null 2>&1; then
		log_error "curl is not installed or not on PATH."
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
		log_warn "GITHUB_TOKEN does not match a known GitHub token prefix."
		log_warn "Proceeding, but authentication may fail."
	fi
}

resolve_latest_published_version() {
	local api_url="https://api.github.com/user/packages/maven/${GROUP_ID}.${ARTIFACT_ID}/versions"
	local response
	local http_status

	response=$(curl -s -w "\n%{http_code}" \
		-H "Authorization: Bearer ${GITHUB_TOKEN}" \
		-H "Accept: application/vnd.github+json" \
		-H "X-GitHub-Api-Version: 2022-11-28" \
		"${api_url}")

	http_status=$(echo "${response}" | tail -n1)
	local body
	body=$(echo "${response}" | head -n -1)

	if [[ "${http_status}" == "404" ]]; then
		echo ""
		return 0
	fi

	if [[ "${http_status}" != "200" ]]; then
		log_error "Failed to query GitHub Packages API. HTTP status: ${http_status}"
		log_error "Response body: ${body}"
		exit 1
	fi

	local latest_version
	latest_version=$(echo "${body}" |
		grep -o '"name":"[0-9][0-9]*\.[0-9][0-9]*\.[0-9][0-9]*"' |
		head -n1 |
		grep -o '[0-9][0-9]*\.[0-9][0-9]*\.[0-9][0-9]*')

	echo "${latest_version}"
}

increment_patch_version() {
	local version="${1}"
	local major minor patch

	IFS='.' read -r major minor patch <<<"${version}"

	if [[ ! "${major}" =~ ^[0-9]+$ ]] ||
		[[ ! "${minor}" =~ ^[0-9]+$ ]] ||
		[[ ! "${patch}" =~ ^[0-9]+$ ]]; then
		log_error "Cannot parse version components from '${version}'."
		exit 1
	fi

	echo "${major}.${minor}.$((patch + 1))"
}

resolve_next_version() {
	log_info "Resolving next version from GitHub Packages..."

	local latest_version
	latest_version=$(resolve_latest_published_version)

	local next_version
	if [[ -z "${latest_version}" ]]; then
		next_version="${INITIAL_VERSION}"
		log_info "No existing published version found. Using initial version: ${next_version}"
	else
		next_version=$(increment_patch_version "${latest_version}")
		log_info "Latest published version : ${latest_version}"
		log_info "Next version             : ${next_version}"
	fi

	echo "${next_version}"
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
	log_info "Maven settings written to '${MAVEN_SETTINGS_FILE}'."
}

cleanup_maven_settings() {
	if [[ -f "${MAVEN_SETTINGS_FILE}" ]]; then
		rm -f "${MAVEN_SETTINGS_FILE}"
		log_info "Temporary Maven settings removed."
	fi
}

set_pom_version() {
	local version="${1}"

	log_info "Setting pom.xml version to '${version}'..."

	mvn versions:set \
		-DnewVersion="${version}" \
		--file "${POM_FILE}" \
		--batch-mode \
		--quiet

	mvn versions:commit \
		--file "${POM_FILE}" \
		--batch-mode \
		--quiet

	log_info "pom.xml version set and committed."
}

write_version_output() {
	local version="${1}"
	local output_dir
	output_dir="$(dirname "${VERSION_OUTPUT_FILE}")"

	mkdir -p "${output_dir}"
	echo "${version}" >"${VERSION_OUTPUT_FILE}"
	log_info "Published version written to '${VERSION_OUTPUT_FILE}'."
}

publish() {
	local version="${1}"
	local repository_url="https://maven.pkg.github.com/${GITHUB_REPOSITORY}"

	log_info "Publishing generated client to GitHub Packages..."
	log_info "Repository : ${repository_url}"
	log_info "Actor      : ${GITHUB_ACTOR}"
	log_info "Version    : ${version}"

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
			log_info "Artifact version '${version}' already exists (409 Conflict). Skipping publish."
			write_version_output "${version}"
			return 0
		fi
		log_error "Publish failed with exit code ${exit_code}."
		echo "${output}" >&2
		exit "${exit_code}"
	fi

	log_info "Client version '${version}' published successfully."
	write_version_output "${version}"
}

main() {
	trap cleanup_maven_settings EXIT

	validate_environment
	validate_token_format

	local next_version
	next_version=$(resolve_next_version)

	generate_maven_settings
	set_pom_version "${next_version}"
	publish "${next_version}"
}

main "$@"
