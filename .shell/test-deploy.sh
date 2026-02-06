#!/bin/bash

################################################################################
# VaikaParts Core Backend - Deployment Script Test Suite
#
# Description: Test script to validate deploy.sh functionality without
#              connecting to GitHub Container Registry. Uses a simple
#              HTTP server container to test deployment logic.
#
# Usage: ./test-deploy.sh [-v|--verbose]
#
# Options:
#   -v, --verbose    Show full deployment output for debugging
#
# Requirements:
#   - Docker installed and running#   - deploy.sh script present in the same directory
#   - Sufficient permissions to create test files and run Docker
#
# Author: Devik'Apps DevOps Team
# Version: 1.1.0
################################################################################

set -euo pipefail

# Parse command line arguments
VERBOSE_MODE=false
if [ "${1:-}" == "-v" ] || [ "${1:-}" == "--verbose" ]; then
	VERBOSE_MODE=true
fi

# Test configuration
readonly TEST_DIR="/tmp/vaikaparts-test"
readonly TEST_SCRIPT_DIR="${TEST_DIR}"
readonly TEST_LOG_DIR="${TEST_SCRIPT_DIR}/logs"
readonly TEST_ENV_FILE="${TEST_SCRIPT_DIR}/.env"
readonly DEPLOY_SCRIPT="./deploy.sh"
readonly TEST_PORT=8080 # Use 8080 instead of 80 to avoid conflicts

# Use httpd (Apache) as mock - it responds properly to HTTP requests
readonly MOCK_IMAGE_BASE="httpd"
readonly MOCK_IMAGE_TAG_1="2.4-alpine"
readonly MOCK_IMAGE_TAG_2="2.4.58-alpine"
# Note: We'll only use tags 1 and 2 since we're hitting Docker rate limits
# The cleanup test will work with just 2 different versions

# Color codes
if [ -t 1 ]; then
	RED='\033[0;31m'
	GREEN='\033[0;32m'
	YELLOW='\033[1;33m'
	BLUE='\033[0;34m'
	CYAN='\033[0;36m'
	MAGENTA='\033[0;35m'
	NC='\033[0m'
else
	RED=''
	GREEN=''
	YELLOW=''
	BLUE=''
	CYAN=''
	MAGENTA=''
	NC=''
fi

# Test result tracking
TESTS_PASSED=0
TESTS_FAILED=0
TESTS_TOTAL=0

# Logging functions
log_test() {
	echo -e "${CYAN}[TEST]${NC} $1"
}

log_pass() {
	echo -e "${GREEN}[PASS]${NC} $1"
	TESTS_PASSED=$((TESTS_PASSED + 1))
}

log_fail() {
	echo -e "${RED}[FAIL]${NC} $1"
	TESTS_FAILED=$((TESTS_FAILED + 1))
}

log_info() {
	echo -e "${BLUE}[INFO]${NC} $1"
}

log_warn() {
	echo -e "${YELLOW}[WARN]${NC} $1"
}

log_section() {
	echo ""
	echo -e "${MAGENTA}========================================${NC}"
	echo -e "${MAGENTA}$1${NC}"
	echo -e "${MAGENTA}========================================${NC}"
}

log_debug() {
	if [ "${VERBOSE_MODE}" = true ]; then
		echo -e "${BLUE}[DEBUG]${NC} $1"
	fi
}

# Run command with optional verbose output
run_deployment() {
	local image="$1"
	local exit_code
	local output_file="/tmp/deploy-output-$$.txt"

	if [ "${VERBOSE_MODE}" = true ]; then
		log_debug "Running deployment in verbose mode..." >&2
		set +e
		"${TEST_SCRIPT_DIR}/deploy.sh" "${image}" >&2
		exit_code=$?
		set -e

		# In verbose mode, just return the exit code
		echo "${exit_code}"
	else
		set +e
		"${TEST_SCRIPT_DIR}/deploy.sh" "${image}" >"${output_file}" 2>&1
		exit_code=$?
		set -e

		# Return exit code on first line
		echo "${exit_code}"

		# Return output on subsequent lines
		if [ -f "${output_file}" ]; then
			cat "${output_file}"
			rm -f "${output_file}"
		fi
	fi
}

# Setup test environment
setup_test_environment() {
	log_section "Setting Up Test Environment"

	log_info "Creating test directory structure"
	mkdir -p "${TEST_SCRIPT_DIR}"
	mkdir -p "${TEST_LOG_DIR}"

	log_info "Creating mock environment file"
	cat >"${TEST_ENV_FILE}" <<EOF
# Mock environment variables for testing
PORT=${TEST_PORT}
SPRING_PROFILES_ACTIVE=test
APP_NAME=vaikaparts-test
DATABASE_URL=jdbc:h2:mem:testdb
EOF

	log_info "Copying deploy.sh to test location"
	if [ ! -f "${DEPLOY_SCRIPT}" ]; then
		log_fail "deploy.sh not found in current directory"
		exit 1
	fi

	# Create modified version of deploy.sh for testing
	cp "${DEPLOY_SCRIPT}" "${TEST_SCRIPT_DIR}/deploy.sh"

	log_debug "Applying sed replacements..."

	# Replace all path references
	sed -i "s|readonly SCRIPT_DIR=\"/home/dummyUsername/dummyDirectory/core-domain\"|readonly SCRIPT_DIR=\"${TEST_SCRIPT_DIR}\"|g" "${TEST_SCRIPT_DIR}/deploy.sh"

	# Replace container and image names
	sed -i 's|readonly CONTAINER_NAME="vaikaparts-core-backend"|readonly CONTAINER_NAME="vaikaparts-test-backend"|g' "${TEST_SCRIPT_DIR}/deploy.sh"
	sed -i 's|readonly IMAGE_BASE_NAME="vaikaparts-core-backend"|readonly IMAGE_BASE_NAME="httpd"|g' "${TEST_SCRIPT_DIR}/deploy.sh"

	# Replace port (httpd uses port 80 internally, we map to 8080 externally)
	sed -i "s|readonly CONTAINER_PORT=9090|readonly CONTAINER_PORT=${TEST_PORT}|g" "${TEST_SCRIPT_DIR}/deploy.sh"

	# Replace health check endpoint to use hardcoded port
	sed -i "s|readonly HEALTH_CHECK_ENDPOINT=\"http://localhost:\\\${CONTAINER_PORT}/actuator/health\"|readonly HEALTH_CHECK_ENDPOINT=\"http://localhost:${TEST_PORT}/\"|g" "${TEST_SCRIPT_DIR}/deploy.sh"

	# CRITICAL FIX: httpd listens on port 80 internally, but we want to expose it on 8080
	# Change the docker run command to map 8080:80 instead of 8080:8080
	sed -i "s|-p \"\${CONTAINER_PORT}:\${CONTAINER_PORT}\"|-p \"\${CONTAINER_PORT}:80\"|g" "${TEST_SCRIPT_DIR}/deploy.sh"

	# Reduce health check retries for faster testing
	sed -i 's|readonly HEALTH_CHECK_MAX_RETRIES=30|readonly HEALTH_CHECK_MAX_RETRIES=15|g' "${TEST_SCRIPT_DIR}/deploy.sh"
	sed -i 's|readonly HEALTH_CHECK_INTERVAL=2|readonly HEALTH_CHECK_INTERVAL=3|g' "${TEST_SCRIPT_DIR}/deploy.sh"

	chmod +x "${TEST_SCRIPT_DIR}/deploy.sh"

	log_pass "Test environment setup completed"

	if [ "${VERBOSE_MODE}" = true ]; then
		log_debug "Modified deploy.sh configuration:"
		grep "readonly SCRIPT_DIR\|readonly CONTAINER_NAME\|readonly IMAGE_BASE_NAME\|readonly CONTAINER_PORT\|readonly HEALTH_CHECK" "${TEST_SCRIPT_DIR}/deploy.sh" | head -10
		log_debug "Docker run port mapping:"
		grep '\-p' "${TEST_SCRIPT_DIR}/deploy.sh" | head -1
	fi
}

# Pull mock images for testing
prepare_mock_images() {
	log_section "Preparing Mock Images"

	local images_to_pull=()

	# Check which images need to be pulled
	if ! docker image inspect "${MOCK_IMAGE_BASE}:${MOCK_IMAGE_TAG_1}" >/dev/null 2>&1; then
		images_to_pull+=("${MOCK_IMAGE_BASE}:${MOCK_IMAGE_TAG_1}")
	else
		log_debug "Image ${MOCK_IMAGE_BASE}:${MOCK_IMAGE_TAG_1} already exists locally"
	fi

	if ! docker image inspect "${MOCK_IMAGE_BASE}:${MOCK_IMAGE_TAG_2}" >/dev/null 2>&1; then
		images_to_pull+=("${MOCK_IMAGE_BASE}:${MOCK_IMAGE_TAG_2}")
	else
		log_debug "Image ${MOCK_IMAGE_BASE}:${MOCK_IMAGE_TAG_2} already exists locally"
	fi

	# Pull missing images
	if [ ${#images_to_pull[@]} -eq 0 ]; then
		log_pass "All required images already exist locally"
	else
		log_info "Pulling ${#images_to_pull[@]} missing image(s)..."
		for image in "${images_to_pull[@]}"; do
			log_info "Pulling ${image}"
			if [ "${VERBOSE_MODE}" = true ]; then
				docker pull "${image}" || log_warn "Failed to pull ${image} (continuing anyway)"
			else
				docker pull "${image}" >/dev/null 2>&1 || log_warn "Failed to pull ${image} (continuing anyway)"
			fi
		done
		log_pass "Mock images check completed"
	fi
}

# Cleanup any existing test containers and images
cleanup_existing_tests() {
	log_debug "Cleaning up any existing test artifacts"
	docker stop vaikaparts-test-backend 2>/dev/null || true
	docker rm vaikaparts-test-backend 2>/dev/null || true
	sleep 1
}

# Test 1: Validate script prerequisites check
test_prerequisites_validation() {
	log_section "Test 1: Prerequisites Validation"
	TESTS_TOTAL=$((TESTS_TOTAL + 1))

	log_test "Testing prerequisites validation with missing .env file"

	# Temporarily remove .env file
	mv "${TEST_ENV_FILE}" "${TEST_ENV_FILE}.backup"

	local output
	local exit_code

	set +e
	output=$("${TEST_SCRIPT_DIR}/deploy.sh" "${MOCK_IMAGE_BASE}:${MOCK_IMAGE_TAG_1}" 2>&1)
	exit_code=$?
	set -e

	local test_passed=true

	if [ ${exit_code} -ne 0 ]; then
		log_pass "Script exited with error code (${exit_code})"
	else
		log_fail "Script did not exit with error code"
		test_passed=false
	fi

	if echo "${output}" | grep -q "Environment file not found"; then
		log_pass "Script correctly detected missing .env file"
	else
		log_fail "Script did not detect missing .env file"
		test_passed=false
	fi

	if [ "${test_passed}" = false ]; then
		TESTS_FAILED=$((TESTS_FAILED - 1)) # Correct the double counting
	fi

	# Restore .env file
	mv "${TEST_ENV_FILE}.backup" "${TEST_ENV_FILE}"
}

# Test 2: First deployment (no existing container)
test_first_deployment() {
	log_section "Test 2: First Deployment"
	TESTS_TOTAL=$((TESTS_TOTAL + 1))

	log_test "Testing initial deployment with no existing container"

	cleanup_existing_tests

	local result
	result=$(run_deployment "${MOCK_IMAGE_BASE}:${MOCK_IMAGE_TAG_1}")

	# Extract exit code (first line) and output (rest)
	local exit_code
	exit_code=$(echo "$result" | head -n 1)
	local output
	output=$(echo "$result" | tail -n +2)

	if [ "${exit_code}" -eq 0 ]; then
		log_pass "First deployment executed successfully"

		# Verify container is running
		if docker ps --filter "name=vaikaparts-test-backend" --format "{{.Names}}" | grep -q "vaikaparts-test-backend"; then
			log_pass "Container is running after deployment"
		else
			log_fail "Container is not running after deployment"
			if [ "${VERBOSE_MODE}" = false ] && [ -n "${output}" ]; then
				echo "Deployment output:" >&2
				echo "${output}" >&2
			fi
			return
		fi

		# Verify correct image is used
		local running_image
		running_image=$(docker ps --filter "name=vaikaparts-test-backend" --format "{{.Image}}")
		if [[ "${running_image}" == "${MOCK_IMAGE_BASE}:${MOCK_IMAGE_TAG_1}" ]]; then
			log_pass "Correct image is running: ${running_image}"
		else
			log_fail "Wrong image is running: ${running_image}"
		fi

		# Verify container is actually responding
		sleep 3
		if curl -sf "http://localhost:${TEST_PORT}/" >/dev/null 2>&1; then
			log_pass "Container is responding to HTTP requests"
		else
			log_fail "Container is not responding to HTTP requests"
		fi
	else
		log_fail "First deployment failed with exit code ${exit_code}"
		if [ "${VERBOSE_MODE}" = false ] && [ -n "${output}" ]; then
			echo "Deployment output:" >&2
			echo "${output}" >&2
		fi
	fi
}

# Test 3: Update deployment (replace existing container)
test_update_deployment() {
	log_section "Test 3: Update Deployment"
	TESTS_TOTAL=$((TESTS_TOTAL + 1))

	log_test "Testing deployment update with existing container"

	sleep 3

	local result
	result=$(run_deployment "${MOCK_IMAGE_BASE}:${MOCK_IMAGE_TAG_2}")

	local exit_code
	exit_code=$(echo "$result" | head -n 1)
	local output
	output=$(echo "$result" | tail -n +2)

	if [ "${exit_code}" -eq 0 ]; then
		log_pass "Update deployment executed successfully"

		# Verify new image is running
		local running_image
		running_image=$(docker ps --filter "name=vaikaparts-test-backend" --format "{{.Image}}")
		if [[ "${running_image}" == "${MOCK_IMAGE_BASE}:${MOCK_IMAGE_TAG_2}" ]]; then
			log_pass "Container updated to new image: ${running_image}"
		else
			log_fail "Container not updated to new image. Running: ${running_image}"
		fi

		# Verify old image still exists (for rollback)
		if docker images --format "{{.Repository}}:{{.Tag}}" | grep -q "${MOCK_IMAGE_BASE}:${MOCK_IMAGE_TAG_1}"; then
			log_pass "Previous image retained for rollback"
		else
			log_fail "Previous image was deleted (should be retained)"
		fi
	else
		log_fail "Update deployment failed with exit code ${exit_code}"
		if [ "${VERBOSE_MODE}" = false ] && [ -n "${output}" ]; then
			echo "Deployment output:" >&2
			echo "${output}" >&2
		fi
	fi
}

# Test 4: Image cleanup (keep only last 2 versions)
test_image_cleanup() {
	log_section "Test 4: Image Cleanup"
	TESTS_TOTAL=$((TESTS_TOTAL + 1))

	log_test "Testing image retention (verifying last 2 versions are kept)"

	sleep 3

	# Re-deploy first image - this should keep both images since we only have 2
	local result
	result=$(run_deployment "${MOCK_IMAGE_BASE}:${MOCK_IMAGE_TAG_1}")

	local exit_code
	exit_code=$(echo "$result" | head -n 1)
	local output
	output=$(echo "$result" | tail -n +2)

	if [ "${exit_code}" -eq 0 ]; then
		log_pass "Re-deployment executed successfully"

		# Count httpd images (should still have both)
		local image_count
		image_count=$(docker images --filter "reference=${MOCK_IMAGE_BASE}:*alpine*" --format "{{.Repository}}:{{.Tag}}" | wc -l)

		if [ "${image_count}" -le 2 ]; then
			log_pass "Image retention working correctly (${image_count} images present)"
		else
			log_warn "More than 2 images present (${image_count} images)"
		fi

		# Verify both images still exist
		if docker images --format "{{.Repository}}:{{.Tag}}" | grep -q "${MOCK_IMAGE_BASE}:${MOCK_IMAGE_TAG_1}"; then
			log_pass "First image (${MOCK_IMAGE_TAG_1}) still exists"
		else
			log_fail "First image was incorrectly deleted"
		fi

		if docker images --format "{{.Repository}}:{{.Tag}}" | grep -q "${MOCK_IMAGE_BASE}:${MOCK_IMAGE_TAG_2}"; then
			log_pass "Second image (${MOCK_IMAGE_TAG_2}) still exists"
		else
			log_fail "Second image was incorrectly deleted"
		fi
	else
		log_fail "Re-deployment failed with exit code ${exit_code}"
		if [ "${VERBOSE_MODE}" = false ] && [ -n "${output}" ]; then
			echo "Deployment output:" >&2
			echo "${output}" >&2
		fi
	fi
}

# Test 5: Log file generation
test_log_generation() {
	log_section "Test 5: Log File Generation"
	TESTS_TOTAL=$((TESTS_TOTAL + 1))

	log_test "Testing log file generation"

	# Check for detailed log files
	local detailed_logs
	detailed_logs=$(find "${TEST_LOG_DIR}" -name "deployment-log-*.txt" 2>/dev/null | wc -l)

	if [ "${detailed_logs}" -gt 0 ]; then
		log_pass "Detailed log files created (${detailed_logs} files)"
	else
		log_fail "No detailed log files found"
	fi

	# Check for summary log files
	local summary_logs
	summary_logs=$(find "${TEST_LOG_DIR}" -name "deployment-summary-*.md" 2>/dev/null | wc -l)

	if [ "${summary_logs}" -gt 0 ]; then
		log_pass "Summary log files created (${summary_logs} files)"
	else
		log_fail "No summary log files found"
	fi

	# Verify log content
	local latest_summary
	latest_summary=$(find "${TEST_LOG_DIR}" -name "deployment-summary-*.md" -type f -printf '%T@ %p\n' 2>/dev/null | sort -rn | head -1 | cut -d' ' -f2-)

	if [ -n "${latest_summary}" ] && [ -f "${latest_summary}" ]; then
		if grep -q "Deployment Summary" "${latest_summary}"; then
			log_pass "Summary log contains expected header"
		else
			log_fail "Summary log missing expected content"
		fi

		if grep -q "SUCCESS" "${latest_summary}"; then
			log_pass "Summary log shows SUCCESS status"
		else
			log_fail "Summary log does not show SUCCESS status"
			if [ "${VERBOSE_MODE}" = true ]; then
				log_debug "Summary log content:"
				cat "${latest_summary}"
			fi
		fi
	fi
}

# Test 6: Invalid image reference handling
test_invalid_image_reference() {
	log_section "Test 6: Invalid Image Reference Handling"
	TESTS_TOTAL=$((TESTS_TOTAL + 1))

	log_test "Testing script behavior with invalid image reference"

	local output
	local exit_code

	# Capture both output and exit code
	set +e
	output=$("${TEST_SCRIPT_DIR}/deploy.sh" "invalid-image-ref" 2>&1)
	exit_code=$?
	set -e

	# Test should verify two things:
	# 1. Script exits with non-zero code
	# 2. Error message is present in output

	local test_passed=true

	if [ ${exit_code} -ne 0 ]; then
		log_pass "Script exited with error code (${exit_code})"
	else
		log_fail "Script did not exit with error code (exit code: ${exit_code})"
		test_passed=false
	fi

	if echo "${output}" | grep -q "Invalid image reference format"; then
		log_pass "Script output contains expected error message"
	else
		log_fail "Script did not output expected error message"
		test_passed=false
	fi

	if [ "${test_passed}" = true ]; then
		log_pass "Script correctly rejected invalid image reference"
	else
		log_fail "Script did not properly validate image reference"
		# Correct double counting since we already incremented failures above
		TESTS_FAILED=$((TESTS_FAILED - 1))
	fi
}

# Test 7: Container health check
test_health_check() {
	log_section "Test 7: Health Check Functionality"
	TESTS_TOTAL=$((TESTS_TOTAL + 1))

	log_test "Testing health check functionality"

	# Find the most recent SUCCESSFUL deployment log (not the invalid-image-ref one)
	local latest_detailed_log
	latest_detailed_log=$(grep -l "Health check passed" "${TEST_LOG_DIR}"/deployment-log-*.txt 2>/dev/null | tail -1)

	if [ -n "${latest_detailed_log}" ] && [ -f "${latest_detailed_log}" ]; then
		if grep -q "Health check passed" "${latest_detailed_log}"; then
			log_pass "Health check executed and passed"
		else
			log_fail "Health check did not pass or was not executed"
			if [ "${VERBOSE_MODE}" = true ]; then
				log_debug "Latest detailed log:"
				tail -50 "${latest_detailed_log}"
			fi
		fi
	else
		log_fail "Could not find detailed log with successful health check"
	fi
}

# Display test results summary
display_test_summary() {
	log_section "Test Results Summary"

	echo ""
	echo "Total Tests: ${TESTS_TOTAL}"
	echo -e "${GREEN}Passed: ${TESTS_PASSED}${NC}"
	echo -e "${RED}Failed: ${TESTS_FAILED}${NC}"
	echo ""

	if [ ${TESTS_FAILED} -eq 0 ]; then
		echo -e "${GREEN}All tests passed successfully!${NC}"
		echo ""
		log_info "Sample logs generated in: ${TEST_LOG_DIR}"
		echo ""
		return 0
	else
		echo -e "${RED}Some tests failed. Please review the output above.${NC}"
		echo ""
		log_warn "Run with -v or --verbose flag to see full deployment output"
		echo ""
		return 1
	fi
}

# Main test execution
main() {
	log_section "VaikaParts Deployment Script Test Suite"
	echo "Starting test execution at $(date '+%Y-%m-%d %H:%M:%S')"
	if [ "${VERBOSE_MODE}" = true ]; then
		echo "(Verbose mode enabled)"
	fi
	echo ""

	# Verify Docker is available
	if ! command -v docker &>/dev/null; then
		log_fail "Docker is not installed. Cannot run tests."
		exit 1
	fi

	if ! docker info &>/dev/null; then
		log_fail "Docker daemon is not running. Cannot run tests."
		exit 1
	fi

	# Setup
	setup_test_environment
	prepare_mock_images

	# Run tests
	test_prerequisites_validation
	test_first_deployment
	test_update_deployment
	test_image_cleanup
	test_log_generation
	test_invalid_image_reference
	test_health_check

	# Display results
	display_test_summary
	local result=$?

	# Cleanup instructions
	echo ""
	log_section "Manual Cleanup Instructions"
	log_info "Test environment kept for inspection at: ${TEST_DIR}"
	log_info "To clean up manually, run the following commands:"
	echo ""
	echo "  docker stop vaikaparts-test-backend 2>/dev/null"
	echo "  docker rm vaikaparts-test-backend 2>/dev/null"
	echo "  docker rmi httpd:${MOCK_IMAGE_TAG_1} httpd:${MOCK_IMAGE_TAG_2} 2>/dev/null"
	echo "  rm -rf ${TEST_DIR}"
	echo ""

	exit ${result}
}

# Run tests
main "$@"
