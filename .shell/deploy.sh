#!/bin/bash

################################################################################
# VaikaParts Core Backend - Production Deployment Script
#
# Description: Automated deployment script for VaikaParts backend application.
#              Manages Docker container lifecycle, image versioning, and logging.
#
# Usage: ./deploy.sh <full_image_reference>
# Example: ./deploy.sh ghcr.io/devik-apps/vaikaparts-core-backend-20260205143022:v1.0.15
#
# Requirements:
#   - Docker installed and running
#   - GitHub Container Registry authentication configured
#   - Environment file present at /home/dummyUsername/dummyDirectory/core-domain/.env
#   - Proper permissions to execute script
#
# Author: Devik'Apps DevOps Team
# Version: 1.0.0
################################################################################

set -euo pipefail

# Script configuration
readonly SCRIPT_DIR="/home/dummyUsername/dummyDirectory/core-domain"
readonly LOG_DIR="${SCRIPT_DIR}/logs"
readonly ENV_FILE="${SCRIPT_DIR}/.env"
readonly CONTAINER_NAME="vaikaparts-core-backend"
readonly IMAGE_BASE_NAME="vaikaparts-core-backend"
readonly MAX_IMAGE_VERSIONS=2
readonly CONTAINER_PORT=8080
readonly HEALTH_CHECK_ENDPOINT="http://localhost:${CONTAINER_PORT}/actuator/health"
readonly HEALTH_CHECK_MAX_RETRIES=30
readonly HEALTH_CHECK_INTERVAL=2

# Generate timestamp for logging
TIMESTAMP=$(date '+%Y-%m-%d-%H-%M-%S')
readonly TIMESTAMP
readonly DETAILED_LOG_FILE="${LOG_DIR}/deployment-log-${TIMESTAMP}.txt"
readonly SUMMARY_LOG_FILE="${LOG_DIR}/deployment-summary-${TIMESTAMP}.md"

# Color codes for terminal output
if [ -t 1 ]; then
	RED='\033[0;31m'
	GREEN='\033[0;32m'
	YELLOW='\033[1;33m'
	BLUE='\033[0;34m'
	CYAN='\033[0;36m'
	NC='\033[0m'
else
	RED=''
	GREEN=''
	YELLOW=''
	BLUE=''
	CYAN=''
	NC=''
fi

# Logging functions
log_info() {
	local message="$1"
	echo -e "${BLUE}[INFO]${NC} ${message}"
	echo "[INFO] $(date '+%Y-%m-%d %H:%M:%S') - ${message}" >>"${DETAILED_LOG_FILE}"
}

log_success() {
	local message="$1"
	echo -e "${GREEN}[SUCCESS]${NC} ${message}"
	echo "[SUCCESS] $(date '+%Y-%m-%d %H:%M:%S') - ${message}" >>"${DETAILED_LOG_FILE}"
}

log_warn() {
	local message="$1"
	echo -e "${YELLOW}[WARN]${NC} ${message}"
	echo "[WARN] $(date '+%Y-%m-%d %H:%M:%S') - ${message}" >>"${DETAILED_LOG_FILE}"
}

log_error() {
	local message="$1"
	echo -e "${RED}[ERROR]${NC} ${message}" >&2
	echo "[ERROR] $(date '+%Y-%m-%d %H:%M:%S') - ${message}" >>"${DETAILED_LOG_FILE}"
}

log_step() {
	local message="$1"
	echo -e "${CYAN}[STEP]${NC} ${message}"
	echo "[STEP] $(date '+%Y-%m-%d %H:%M:%S') - ${message}" >>"${DETAILED_LOG_FILE}"
}

# Initialize logging directory
initialize_logging() {
	if [ ! -d "${LOG_DIR}" ]; then
		mkdir -p "${LOG_DIR}"
		log_info "Created logging directory: ${LOG_DIR}"
	fi

	log_info "Detailed log file: ${DETAILED_LOG_FILE}"
	log_info "Summary log file: ${SUMMARY_LOG_FILE}"
}

# Write summary log in markdown format
write_summary_log() {
	local status="$1"
	local new_image="$2"
	local old_image="${3:-N/A}"
	local error_message="${4:-}"

	cat >"${SUMMARY_LOG_FILE}" <<EOF
# Deployment Summary

**Date:** $(date '+%Y-%m-%d %H:%M:%S')
**Status:** ${status}
**New Image:** ${new_image}
**Previous Image:** ${old_image}

## Deployment Details

- **Container Name:** ${CONTAINER_NAME}
- **Port:** ${CONTAINER_PORT}
- **Environment File:** ${ENV_FILE}

## Actions Performed

EOF

	if [ "${status}" == "SUCCESS" ]; then
		cat >>"${SUMMARY_LOG_FILE}" <<EOF
1. Pre-deployment validation completed
2. New image pulled successfully
3. Old container stopped and removed
4. Image cleanup executed (maintained last ${MAX_IMAGE_VERSIONS} versions)
5. New container started successfully
6. Health check passed

## Container Status

\`\`\`
$(docker ps --filter "name=${CONTAINER_NAME}" --format "table {{.ID}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}")
\`\`\`

## Rollback Information

Previous image version is available for rollback if needed:
- Image: ${old_image}
- Command: \`docker run -d --name ${CONTAINER_NAME} -p ${CONTAINER_PORT}:${CONTAINER_PORT} --env-file ${ENV_FILE} ${old_image}\`

EOF
	else
		cat >>"${SUMMARY_LOG_FILE}" <<EOF
**Error:** ${error_message}

## Troubleshooting

Review the detailed log file for complete information:
\`${DETAILED_LOG_FILE}\`

## Current System State

\`\`\`
$(docker ps -a --filter "name=${CONTAINER_NAME}" --format "table {{.ID}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || echo "No containers found")
\`\`\`

EOF
	fi

	log_info "Summary log written to: ${SUMMARY_LOG_FILE}"
}

# Validate prerequisites
validate_prerequisites() {
	log_step "Validating deployment prerequisites"

	# Check if Docker is installed and running
	if ! command -v docker &>/dev/null; then
		log_error "Docker is not installed on this system"
		write_summary_log "FAILED" "$1" "" "Docker is not installed"
		exit 1
	fi

	if ! docker info &>/dev/null; then
		log_error "Docker daemon is not running"
		write_summary_log "FAILED" "$1" "" "Docker daemon is not running"
		exit 1
	fi
	log_success "Docker is installed and running"

	# Check if environment file exists
	if [ ! -f "${ENV_FILE}" ]; then
		log_error "Environment file not found at: ${ENV_FILE}"
		write_summary_log "FAILED" "$1" "" "Environment file not found"
		exit 1
	fi
	log_success "Environment file found: ${ENV_FILE}"

	# Validate environment file is not empty
	if [ ! -s "${ENV_FILE}" ]; then
		log_error "Environment file is empty: ${ENV_FILE}"
		write_summary_log "FAILED" "$1" "" "Environment file is empty"
		exit 1
	fi
	log_success "Environment file validation passed"

	log_success "All prerequisites validated successfully"
}

# Parse image reference and extract version
parse_image_reference() {
	local image_ref="$1"

	if [[ ! "${image_ref}" =~ .+:.+ ]]; then
		log_error "Invalid image reference format: ${image_ref}"
		write_summary_log "FAILED" "${image_ref}" "" "Invalid image reference format"
		exit 1
	fi

	# Warn if not using ghcr.io (for production)
	if [[ ! "${image_ref}" =~ ^ghcr\.io/ ]]; then
		log_warn "Image reference does not start with ghcr.io - this may not be a production image"
	fi

	log_success "Image reference format validated: ${image_ref}"
}
# Authenticate to GitHub Container Registry
authenticate_registry() {
	log_step "Authenticating to GitHub Container Registry"

	# Check if already authenticated by attempting to pull manifest
	if docker manifest inspect "$1" &>/dev/null; then
		log_success "Already authenticated to GitHub Container Registry"
		return 0
	fi

	log_warn "Registry authentication may be required. Ensure Docker is logged in to ghcr.io"
	log_info "If authentication fails, run: docker login ghcr.io -u <username> -p <token>"
}

# Pull new Docker image
pull_new_image() {
	local image_ref="$1"

	log_step "Checking if image already exists locally: ${image_ref}"

	if docker image inspect "${image_ref}" >/dev/null 2>&1; then
		log_success "Image already exists locally: ${image_ref}"
		log_info "Skipping pull operation"
		return 0
	fi

	log_step "Pulling new Docker image: ${image_ref}"

	if docker pull "${image_ref}"; then
		log_success "Successfully pulled image: ${image_ref}"
	else
		log_error "Failed to pull image: ${image_ref}"
		write_summary_log "FAILED" "${image_ref}" "" "Failed to pull Docker image"
		exit 1
	fi
}

# Get currently running container image
get_current_image() {
	local current_image=""

	if docker ps --filter "name=${CONTAINER_NAME}" --format "{{.Image}}" | grep -q .; then
		current_image=$(docker ps --filter "name=${CONTAINER_NAME}" --format "{{.Image}}")
		log_info "Currently running image: ${current_image}"
	else
		log_warn "No container currently running with name: ${CONTAINER_NAME}"
		current_image="none"
	fi

	echo "${current_image}"
}

stop_current_container() {
	log_step "Stopping current container"

	if docker ps -a --filter "name=${CONTAINER_NAME}" --format "{{.Names}}" | grep -q "^${CONTAINER_NAME}$"; then
		# Check if it's running and stop it
		if docker ps --filter "name=${CONTAINER_NAME}" --format "{{.Names}}" | grep -q "^${CONTAINER_NAME}$"; then
			log_info "Stopping container: ${CONTAINER_NAME}"
			if docker stop "${CONTAINER_NAME}"; then
				log_success "Container stopped successfully"
			else
				log_error "Failed to stop container: ${CONTAINER_NAME}"
				write_summary_log "FAILED" "$1" "" "Failed to stop current container"
				exit 1
			fi
		else
			log_warn "Container exists but is not running: ${CONTAINER_NAME}"
		fi

		log_info "Removing container: ${CONTAINER_NAME}"
		if docker rm "${CONTAINER_NAME}"; then
			log_success "Container removed successfully"
		else
			log_error "Failed to remove container: ${CONTAINER_NAME}"
			write_summary_log "FAILED" "$1" "" "Failed to remove stopped container"
			exit 1
		fi
	else
		log_warn "No container found with name: ${CONTAINER_NAME}"
	fi
}

# Clean up old Docker images (keep only last 2 versions)
cleanup_old_images() {
	log_step "Cleaning up old Docker images"

	# Get all images with the base name, sorted by creation date (newest first)
	local all_images
	all_images=$(docker images --filter "reference=${IMAGE_BASE_NAME}:*" --format "{{.Repository}}:{{.Tag}}" | sort -r)

	if [ -z "${all_images}" ]; then
		log_warn "No images found matching pattern: ${IMAGE_BASE_NAME}:*"
		return 0
	fi

	local image_count
	image_count=$(echo "${all_images}" | wc -l)
	log_info "Found ${image_count} image(s) with base name: ${IMAGE_BASE_NAME}"

	if [ "${image_count}" -le "${MAX_IMAGE_VERSIONS}" ]; then
		log_info "Image count (${image_count}) is within limit (${MAX_IMAGE_VERSIONS}). No cleanup needed."
		return 0
	fi

	# Keep only the last MAX_IMAGE_VERSIONS images
	local images_to_delete
	images_to_delete=$(echo "${all_images}" | tail -n +$((MAX_IMAGE_VERSIONS + 1)))

	log_info "Deleting $(echo "${images_to_delete}" | wc -l) old image(s)"

	while IFS= read -r image; do
		if [ -n "${image}" ]; then
			log_info "Deleting image: ${image}"
			if docker rmi "${image}" 2>>"${DETAILED_LOG_FILE}"; then
				log_success "Deleted image: ${image}"
			else
				log_warn "Failed to delete image: ${image} (may be in use)"
			fi
		fi
	done <<<"${images_to_delete}"

	log_success "Image cleanup completed"
}

# Start new container
start_new_container() {
	local image_ref="$1"

	log_step "Starting new container with image: ${image_ref}"

	if docker run -d \
		--name "${CONTAINER_NAME}" \
		-p "${CONTAINER_PORT}:${CONTAINER_PORT}" \
		--env-file "${ENV_FILE}" \
		--restart unless-stopped \
		"${image_ref}"; then
		log_success "Container started successfully"
	else
		log_error "Failed to start container with image: ${image_ref}"
		write_summary_log "FAILED" "${image_ref}" "" "Failed to start new container"
		exit 1
	fi
}

# Perform health check
perform_health_check() {
	log_step "Performing health check on new container"

	local retry_count=0
	local health_status="unhealthy"

	log_info "Waiting for application to start (max ${HEALTH_CHECK_MAX_RETRIES} attempts)"

	while [ ${retry_count} -lt ${HEALTH_CHECK_MAX_RETRIES} ]; do
		retry_count=$((retry_count + 1))

		# Check if container is still running
		if ! docker ps --filter "name=${CONTAINER_NAME}" --format "{{.Names}}" | grep -q "^${CONTAINER_NAME}$"; then
			log_error "Container stopped unexpectedly during health check"
			log_info "Container logs:"
			docker logs "${CONTAINER_NAME}" 2>&1 | tail -n 50 >>"${DETAILED_LOG_FILE}"
			write_summary_log "FAILED" "$1" "$2" "Container stopped during health check"
			exit 1
		fi

		# Attempt health check
		if curl -sf "${HEALTH_CHECK_ENDPOINT}" >/dev/null 2>&1; then
			health_status="healthy"
			log_success "Health check passed on attempt ${retry_count}"
			break
		fi

		log_info "Health check attempt ${retry_count}/${HEALTH_CHECK_MAX_RETRIES} failed. Retrying in ${HEALTH_CHECK_INTERVAL}s..."
		sleep ${HEALTH_CHECK_INTERVAL}
	done

	if [ "${health_status}" != "healthy" ]; then
		log_error "Health check failed after ${HEALTH_CHECK_MAX_RETRIES} attempts"
		log_info "Container logs:"
		docker logs "${CONTAINER_NAME}" 2>&1 | tail -n 50 >>"${DETAILED_LOG_FILE}"
		write_summary_log "FAILED" "$1" "$2" "Health check failed after ${HEALTH_CHECK_MAX_RETRIES} attempts"
		exit 1
	fi

	log_success "Application is healthy and responding correctly"
}

# Display deployment summary
display_summary() {
	local new_image="$1"
	local old_image="$2"

	echo ""
	echo "=========================================="
	echo "Deployment Completed Successfully"
	echo "=========================================="
	echo "Timestamp: $(date '+%Y-%m-%d %H:%M:%S')"
	echo "New Image: ${new_image}"
	echo "Previous Image: ${old_image}"
	echo "Container Name: ${CONTAINER_NAME}"
	echo "Container Port: ${CONTAINER_PORT}"
	echo ""
	echo "Container Status:"
	docker ps --filter "name=${CONTAINER_NAME}" --format "table {{.ID}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}"
	echo ""
	echo "Logs:"
	echo "  Detailed: ${DETAILED_LOG_FILE}"
	echo "  Summary: ${SUMMARY_LOG_FILE}"
	echo "=========================================="
}

# Main deployment function
main() {
	local new_image_ref="$1"
	local old_image_ref=""

	echo "=========================================="
	echo "VaikaParts Backend Deployment Script"
	echo "=========================================="
	echo "Starting deployment process at $(date '+%Y-%m-%d %H:%M:%S')"
	echo ""

	# Initialize logging
	initialize_logging

	log_info "Deployment initiated for image: ${new_image_ref}"

	# Validate prerequisites
	validate_prerequisites "${new_image_ref}"

	# Parse and validate image reference
	parse_image_reference "${new_image_ref}"

	# Authenticate to registry
	authenticate_registry "${new_image_ref}"

	# Pull new image
	pull_new_image "${new_image_ref}"

	# Get current image before stopping
	old_image_ref=$(get_current_image)

	# Stop current container
	stop_current_container "${new_image_ref}"

	# Clean up old images
	cleanup_old_images

	# Start new container
	start_new_container "${new_image_ref}"

	# Perform health check
	perform_health_check "${new_image_ref}" "${old_image_ref}"

	# Write summary log
	write_summary_log "SUCCESS" "${new_image_ref}" "${old_image_ref}"

	# Display summary
	display_summary "${new_image_ref}" "${old_image_ref}"

	log_success "Deployment completed successfully"

	exit 0
}

# Script entry point
if [ $# -ne 1 ]; then
	echo "Usage: $0 <full_image_reference>"
	echo "Example: $0 ghcr.io/devik-apps/vaikaparts-core-backend-20260205143022:v1.0.15"
	exit 1
fi

main "$1"
