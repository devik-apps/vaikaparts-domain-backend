#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly SCRIPT_DIR

PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
readonly PROJECT_ROOT

readonly GENERATED_DIR="${PROJECT_ROOT}/build/generated/openapi-ts-client"
readonly PACKAGE_JSON_FILE="${GENERATED_DIR}/package.json"
readonly VERSION_OUTPUT_FILE="${PROJECT_ROOT}/build/published-ts-client-version.txt"
readonly REQUIRED_ENV_VARS=("GITHUB_ACTOR" "GITHUB_TOKEN" "GITHUB_REPOSITORY")

readonly INITIAL_VERSION="1.0.0"
readonly CLIENT_TAG_PREFIX="client-ts-v"
readonly NPM_REGISTRY="https://npm.pkg.github.com"

readonly NPMRC_FILE="${PROJECT_ROOT}/build/.npmrc-ts-publish"

log_info() { echo "[INFO]  $*" >&2; }
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

	if [[ ! -f "${PACKAGE_JSON_FILE}" ]]; then
		log_error "Generated package.json not found at '${PACKAGE_JSON_FILE}'."
		log_error "Run './gradlew generateTypeScriptClient' first."
		exit 1
	fi

	if ! command -v node >/dev/null 2>&1; then
		log_error "Node.js is not installed or not on PATH."
		exit 1
	fi

	if ! command -v npm >/dev/null 2>&1; then
		log_error "npm is not installed or not on PATH."
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

resolve_latest_version_from_git_tags() {
	local latest_version=""
	local tag version

	while IFS= read -r tag; do
		version="${tag#"${CLIENT_TAG_PREFIX}"}"

		if [[ ! "${version}" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
			continue
		fi

		if [[ -z "${latest_version}" ]]; then
			latest_version="${version}"
			continue
		fi

		local curr_major curr_minor curr_patch
		local new_major new_minor new_patch

		IFS='.' read -r curr_major curr_minor curr_patch <<<"${latest_version}"
		IFS='.' read -r new_major new_minor new_patch <<<"${version}"

		if ((new_major > curr_major)) ||
			((new_major == curr_major && new_minor > curr_minor)) ||
			((new_major == curr_major && new_minor == curr_minor && new_patch > curr_patch)); then
			latest_version="${version}"
		fi
	done < <(git -C "${PROJECT_ROOT}" tag --list "${CLIENT_TAG_PREFIX}*" 2>/dev/null)

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
	log_info "Resolving next version from git tags..."

	local latest_version
	latest_version=$(resolve_latest_version_from_git_tags)

	local next_version
	if [[ -z "${latest_version}" ]]; then
		next_version="${INITIAL_VERSION}"
		log_info "No existing TypeScript client tag found. Using initial version: ${next_version}"
	else
		next_version=$(increment_patch_version "${latest_version}")
		log_info "Latest tagged version : ${latest_version}"
		log_info "Next version          : ${next_version}"
	fi

	echo "${next_version}"
}

resolve_github_org() {
	local org="${GITHUB_REPOSITORY%%/*}"

	if [[ ! "${org}" =~ ^[a-zA-Z0-9]([a-zA-Z0-9-]{0,37}[a-zA-Z0-9])?$ ]]; then
		log_error "Derived GitHub org '${org}' from GITHUB_REPOSITORY does not match expected format."
		exit 1
	fi

	echo "${org}"
}

patch_package_json() {
	local version="${1}"
	local scope="${2}"

	log_info "Patching package.json (version: '${version}', scope: '@${scope}')..."

	PATCH_VERSION="${version}" \
		PATCH_SCOPE="${scope}" \
		PATCH_REGISTRY="${NPM_REGISTRY}/" \
		PATCH_FILE="${PACKAGE_JSON_FILE}" \
		node <<'EOF'
const fs   = require('fs');
const path = require('path');

const filePath = process.env.PATCH_FILE;
const version  = process.env.PATCH_VERSION;
const scope    = process.env.PATCH_SCOPE;
const registry = process.env.PATCH_REGISTRY;

if (!/^\d+\.\d+\.\d+$/.test(version)) {
    process.stderr.write('[ERROR] Invalid version format: ' + version + '\n');
    process.exit(1);
}
if (!/^[a-zA-Z0-9]([a-zA-Z0-9-]{0,37}[a-zA-Z0-9])?$/.test(scope)) {
    process.stderr.write('[ERROR] Invalid scope format: ' + scope + '\n');
    process.exit(1);
}

let raw;
try {
    raw = fs.readFileSync(filePath, 'utf8');
} catch (err) {
    process.stderr.write('[ERROR] Cannot read package.json: ' + err.message + '\n');
    process.exit(1);
}

let pkg;
try {
    pkg = JSON.parse(raw);
} catch (err) {
    process.stderr.write('[ERROR] package.json is not valid JSON: ' + err.message + '\n');
    process.exit(1);
}

const baseName = pkg.name.replace(/^@[^/]+\//, '');
pkg.name          = '@' + scope + '/' + baseName;
pkg.version       = version;
pkg.publishConfig = { registry: registry };
delete pkg.repository;

fs.writeFileSync(filePath, JSON.stringify(pkg, null, 2) + '\n', { mode: 0o644 });
process.stdout.write('[INFO]  package.json patched successfully.\n');
EOF
}

configure_npm_auth() {
	log_info "Writing temporary npm credentials file..."

	local build_dir
	build_dir="$(dirname "${NPMRC_FILE}")"
	mkdir -p "${build_dir}"

	(umask 177 && touch "${NPMRC_FILE}")

	{
		printf 'registry=%s/\n' "${NPM_REGISTRY}"
		printf '//npm.pkg.github.com/:_authToken=%s\n' "${GITHUB_TOKEN}"
	} >>"${NPMRC_FILE}"

	log_info "npm credentials file written to '${NPMRC_FILE}'."
}

cleanup_npm_auth() {
	if [[ -f "${NPMRC_FILE}" ]]; then
		dd if=/dev/zero of="${NPMRC_FILE}" bs=1 count="$(wc -c <"${NPMRC_FILE}")" conv=notrunc 2>/dev/null || true
		rm -f "${NPMRC_FILE}"
		log_info "npm credentials file securely removed."
	fi
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

	log_info "Publishing TypeScript client to GitHub Packages (npm)..."
	log_info "Registry : ${NPM_REGISTRY}"
	log_info "Actor    : ${GITHUB_ACTOR}"
	log_info "Version  : ${version}"

	local output
	local exit_code=0

	output=$(
		cd "${GENERATED_DIR}"
		npm publish --access restricted --userconfig "${NPMRC_FILE}" 2>&1
	) || exit_code=$?

	if [[ ${exit_code} -ne 0 ]]; then
		if echo "${output}" | grep -q "409\|Cannot publish over\|already exists"; then
			log_error "Package version '${version}' already exists (conflict)."
			log_error "Verify that tag '${CLIENT_TAG_PREFIX}${version}' does not already exist."
			exit 1
		fi
		log_error "Publish failed with exit code ${exit_code}."
		echo "${output}" >&2
		exit "${exit_code}"
	fi

	log_info "TypeScript client version '${version}' published successfully."
	write_version_output "${version}"
}

main() {
	trap cleanup_npm_auth EXIT

	validate_environment
	validate_token_format

	local next_version
	next_version=$(resolve_next_version)

	local github_org
	github_org=$(resolve_github_org)

	configure_npm_auth
	patch_package_json "${next_version}" "${github_org}"
	publish "${next_version}"
}

main "$@"
