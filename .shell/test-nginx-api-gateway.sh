#!/bin/bash

################################################################################
# VaikaParts Nginx API Gateway - Test Suite
#
# Description: Comprehensive test script to validate nginx API gateway
#              functionality including routing, SSL/TLS, rate limiting,
#              security headers, backend connectivity, and ModSecurity WAF.
#
# Usage: ./test-nginx-api-gateway.sh [-v|--verbose] [-h|--host HOST]
#
# Options:
#   -v, --verbose    Show detailed test output and curl responses
#   -h, --host HOST  Test against specific host (default: localhost)
#
# Requirements:
#   - nginx installed and running
#   - curl installed
#   - Appropriate permissions to read nginx config files
#
# Author: Devik'Apps DevOps Team
# Version: 1.0.1
################################################################################

set -euo pipefail

# Parse command line arguments
VERBOSE_MODE=false
TEST_HOST="localhost"

while [[ $# -gt 0 ]]; do
	case $1 in
	-v | --verbose)
		VERBOSE_MODE=true
		shift
		;;
	-h | --host)
		TEST_HOST="$2"
		shift 2
		;;
	*)
		echo "Unknown option: $1"
		echo "Usage: $0 [-v|--verbose] [-h|--host HOST]"
		exit 1
		;;
	esac
done

readonly NGINX_ERROR_LOG="/var/log/nginx/error.log"
readonly HTTP_PORT=80
readonly HTTPS_PORT=443
readonly BACKEND_PORT=8080
readonly TEST_TIMEOUT=5

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

TESTS_PASSED=0
TESTS_FAILED=0
TESTS_TOTAL=0
TESTS_SKIPPED=0

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

log_skip() {
	echo -e "${YELLOW}[SKIP]${NC} $1"
	TESTS_SKIPPED=$((TESTS_SKIPPED + 1))
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

execute_curl() {
	local url="$1"
	local args="${2:-}"
	local output_file
	local exit_code

	output_file="/tmp/curl-output-$$-$(date +%s%N).txt"

	if [ "${VERBOSE_MODE}" = true ]; then
		# Print debug to stderr BEFORE executing
		echo -e "${BLUE}[DEBUG]${NC} Executing: curl --max-time ${TEST_TIMEOUT} ${args} ${url}" >&2
		set +e
		# shellcheck disable=SC2086
		curl -v --max-time "${TEST_TIMEOUT}" ${args} "${url}" >"${output_file}" 2>&1
		exit_code=$?
		set -e
		cat "${output_file}" >&2
	else
		set +e
		# shellcheck disable=SC2086
		curl -s --max-time "${TEST_TIMEOUT}" ${args} "${url}" >"${output_file}" 2>&1
		exit_code=$?
		set -e
	fi

	echo "${exit_code}|${output_file}"
}

# Test 1: Nginx service status
test_nginx_service_status() {
	log_section "Test 1: Nginx Service Status"
	TESTS_TOTAL=$((TESTS_TOTAL + 1))

	log_test "Checking if nginx service is active and running"

	if systemctl is-active --quiet nginx; then
		log_pass "Nginx service is active and running"

		# Check process count
		local worker_count
		worker_count=$(pgrep -c nginx || echo "0")
		log_info "Nginx processes running: ${worker_count}"

	else
		log_fail "Nginx service is not running"
		return
	fi
}

# Test 2: Nginx configuration validation
test_nginx_configuration() {
	log_section "Test 2: Nginx Configuration Validation"
	TESTS_TOTAL=$((TESTS_TOTAL + 1))

	log_test "Validating nginx configuration syntax"

	local output
	local exit_code

	set +e
	output=$(sudo nginx -t 2>&1)
	exit_code=$?
	set -e

	if [ ${exit_code} -eq 0 ]; then
		log_pass "Nginx configuration syntax is valid"

		if [ "${VERBOSE_MODE}" = true ]; then
			log_debug "Configuration test output:"
			echo "${output}"
		fi
	else
		log_fail "Nginx configuration has syntax errors"
		echo "${output}"
	fi
}

# Test 3: HTTP to HTTPS redirect
test_http_redirect() {
	log_section "Test 3: HTTP to HTTPS Redirect"
	TESTS_TOTAL=$((TESTS_TOTAL + 1))

	log_test "Testing HTTP to HTTPS redirect"

	local result
	result=$(execute_curl "http://${TEST_HOST}/" "-i")
	local exit_code="${result%%|*}"
	local output_file="${result##*|}"

	if [ "${exit_code}" -eq 0 ]; then
		local output
		output=$(cat "${output_file}")

		if echo "${output}" | grep -q "301 Moved Permanently"; then
			log_pass "HTTP correctly redirects with 301 status"

			if echo "${output}" | grep -i "Location:" | grep -q "https://"; then
				log_pass "Redirect location points to HTTPS"
			else
				log_fail "Redirect location does not point to HTTPS"
			fi
		else
			log_fail "HTTP does not redirect properly"
			if [ "${VERBOSE_MODE}" = true ]; then
				echo "${output}"
			fi
		fi
	else
		log_fail "Failed to connect to HTTP endpoint (exit code: ${exit_code})"
	fi

	rm -f "${output_file}"
}

# Test 4: HTTPS connectivity
test_https_connectivity() {
	log_section "Test 4: HTTPS Connectivity"
	TESTS_TOTAL=$((TESTS_TOTAL + 1))

	log_test "Testing HTTPS endpoint connectivity"

	local result
	result=$(execute_curl "https://${TEST_HOST}/" "-k -i")
	local exit_code="${result%%|*}"
	local output_file="${result##*|}"

	if [ "${exit_code}" -eq 0 ]; then
		log_pass "HTTPS endpoint is accessible"

		local output
		output=$(cat "${output_file}")

		# Check for successful response or redirect
		if echo "${output}" | grep -qE "HTTP/[0-9.]+ (200|301|302|303)"; then
			log_pass "HTTPS endpoint returns valid HTTP status"
		else
			log_warn "HTTPS endpoint returned unexpected status"
		fi
	else
		log_fail "Failed to connect to HTTPS endpoint (exit code: ${exit_code})"
	fi

	rm -f "${output_file}"
}

# Test 5: SSL/TLS certificate
test_ssl_certificate() {
	log_section "Test 5: SSL/TLS Certificate"
	TESTS_TOTAL=$((TESTS_TOTAL + 1))

	log_test "Checking SSL/TLS certificate details"

	local output
	set +e
	output=$(echo | openssl s_client -connect "${TEST_HOST}:${HTTPS_PORT}" -servername "${TEST_HOST}" 2>/dev/null | openssl x509 -noout -subject -dates 2>/dev/null)
	local exit_code=$?
	set -e

	if [ ${exit_code} -eq 0 ] && [ -n "${output}" ]; then
		log_pass "SSL certificate is present and readable"

		if [ "${VERBOSE_MODE}" = true ]; then
			log_debug "Certificate details:"
			echo "${output}"
		fi

		# Check if self-signed
		if echo "${output}" | grep -qi "selfsigned\|localhost"; then
			log_warn "Using self-signed certificate (consider using Let's Encrypt for production)"
		fi

		# Check expiration
		local not_after
		not_after=$(echo "${output}" | grep "notAfter" | cut -d= -f2)
		log_info "Certificate expires: ${not_after}"

	else
		log_fail "Could not retrieve SSL certificate information"
	fi
}

# Test 6: Backend connectivity (health check)
test_backend_connectivity() {
	log_section "Test 6: Backend Service Connectivity"
	TESTS_TOTAL=$((TESTS_TOTAL + 1))

	log_test "Testing backend service health check (direct connection)"

	# Test /ping endpoint directly on backend
	local result
	result=$(execute_curl "http://localhost:${BACKEND_PORT}/ping" "")
	local exit_code="${result%%|*}"
	local output_file="${result##*|}"

	if [ "${exit_code}" -eq 0 ]; then
		local output
		output=$(cat "${output_file}")

		if [ "${output}" = "pong" ]; then
			log_pass "Backend health check endpoint responding correctly on port ${BACKEND_PORT}"
		else
			log_warn "Backend responded but with unexpected content: ${output}"
		fi
	else
		log_fail "Backend health check failed on port ${BACKEND_PORT} (exit code: ${exit_code})"
		log_info "Note: Backend service may not be running on localhost:${BACKEND_PORT}"
	fi

	rm -f "${output_file}"

	# Also test through nginx proxy
	log_test "Testing backend service through nginx proxy"
	result=$(execute_curl "https://${TEST_HOST}/ping" "-k")
	exit_code="${result%%|*}"
	output_file="${result##*|}"

	if [ "${exit_code}" -eq 0 ]; then
		local output
		output=$(cat "${output_file}")

		if [ "${output}" = "pong" ]; then
			log_pass "Nginx proxy to backend is working correctly"
		else
			log_info "Nginx proxy accessible but /ping not routed (this is OK if /ping is not exposed through gateway)"
		fi
	else
		log_info "Nginx proxy does not expose /ping endpoint (this is OK for security)"
	fi

	rm -f "${output_file}"
}

# Test 7: Rate limiting
test_rate_limiting() {
	log_section "Test 7: Rate Limiting"
	TESTS_TOTAL=$((TESTS_TOTAL + 1))

	log_test "Testing rate limiting functionality on nginx gateway"

	local success_count=0
	local rate_limited_count=0
	local total_requests=15

	log_info "Sending ${total_requests} rapid requests to test rate limiting..."

	for _ in $(seq 1 ${total_requests}); do
		local result
		# Test the root endpoint since it exists
		result=$(execute_curl "https://${TEST_HOST}/" "-k -o /dev/null -w '%{http_code}'")
		local exit_code="${result%%|*}"
		local output_file="${result##*|}"

		if [ "${exit_code}" -eq 0 ]; then
			local http_code
			http_code=$(tail -1 "${output_file}")

			if [[ "${http_code}" =~ ^(200|301|302)$ ]]; then
				success_count=$((success_count + 1))
			elif [ "${http_code}" = "503" ] || [ "${http_code}" = "429" ]; then
				rate_limited_count=$((rate_limited_count + 1))
			fi
		fi

		rm -f "${output_file}"

		# Small delay to avoid overwhelming the system
		sleep 0.1
	done

	log_info "Successful requests: ${success_count}/${total_requests}"
	log_info "Rate limited requests: ${rate_limited_count}/${total_requests}"

	if [ ${rate_limited_count} -gt 0 ]; then
		log_pass "Rate limiting is active and working (blocked ${rate_limited_count} requests)"
	else
		log_warn "No requests were rate limited (rate limit may be too permissive or not configured)"
	fi
}

# Test 8: Security headers
test_security_headers() {
	log_section "Test 8: Security Headers"
	TESTS_TOTAL=$((TESTS_TOTAL + 1))

	log_test "Checking for security headers in HTTPS responses"

	local result
	result=$(execute_curl "https://${TEST_HOST}/" "-k -I")
	local exit_code="${result%%|*}"
	local output_file="${result##*|}"

	if [ "${exit_code}" -eq 0 ]; then
		local headers
		headers=$(cat "${output_file}")

		local headers_found=0
		local headers_expected=0

		# Check for common security headers
		if echo "${headers}" | grep -qi "X-Frame-Options"; then
			log_pass "X-Frame-Options header present"
			headers_found=$((headers_found + 1))
		fi
		headers_expected=$((headers_expected + 1))

		if echo "${headers}" | grep -qi "X-Content-Type-Options"; then
			log_pass "X-Content-Type-Options header present"
			headers_found=$((headers_found + 1))
		fi
		headers_expected=$((headers_expected + 1))

		if echo "${headers}" | grep -qi "X-XSS-Protection"; then
			log_pass "X-XSS-Protection header present"
			headers_found=$((headers_found + 1))
		fi
		headers_expected=$((headers_expected + 1))

		if echo "${headers}" | grep -qi "Strict-Transport-Security"; then
			log_pass "Strict-Transport-Security (HSTS) header present"
			headers_found=$((headers_found + 1))
		fi
		headers_expected=$((headers_expected + 1))

		if [ ${headers_found} -eq 0 ]; then
			log_warn "No common security headers found"
		elif [ ${headers_found} -lt ${headers_expected} ]; then
			log_info "Some security headers missing (${headers_found}/${headers_expected} found)"
		fi

		if [ "${VERBOSE_MODE}" = true ]; then
			log_debug "Response headers:"
			echo "${headers}"
		fi
	else
		log_fail "Failed to retrieve headers (exit code: ${exit_code})"
	fi

	rm -f "${output_file}"
}

# Test 9: ModSecurity WAF
test_modsecurity() {
	log_section "Test 9: ModSecurity WAF"
	TESTS_TOTAL=$((TESTS_TOTAL + 1))

	log_test "Checking ModSecurity status"

	# Check nginx status output first
	if systemctl status nginx 2>&1 | grep -q "ModSecurity-nginx"; then
		local rules_info
		rules_info=$(systemctl status nginx 2>&1 | grep "ModSecurity-nginx" | head -1)
		log_pass "ModSecurity is loaded and active"
		log_info "${rules_info}"
	else
		# Check if ModSecurity is mentioned in nginx -T output
		if sudo nginx -T 2>&1 | grep -qi "modsecurity on"; then
			log_pass "ModSecurity configuration detected in nginx"
		else
			log_warn "ModSecurity status unclear - may not be enabled"
		fi
	fi
}

# Test 10: Upstream backend configuration
test_upstream_config() {
	log_section "Test 10: Upstream Backend Configuration"
	TESTS_TOTAL=$((TESTS_TOTAL + 1))

	log_test "Checking upstream backend configuration"

	if [ -r /etc/nginx/conf.d/upstreams.conf ]; then
		local upstreams
		upstreams=$(grep -E "^upstream" /etc/nginx/conf.d/upstreams.conf 2>/dev/null | awk '{print $2}' | tr -d '{')

		if [ -n "${upstreams}" ]; then
			log_pass "Upstream backends configured:"
			echo "${upstreams}" | while read -r upstream; do
				log_info "  - ${upstream}"
			done
		else
			log_warn "No upstreams found in configuration"
		fi

		if [ "${VERBOSE_MODE}" = true ]; then
			log_debug "Upstream configuration:"
			sudo cat /etc/nginx/conf.d/upstreams.conf
		fi
	else
		log_skip "Upstream configuration file not accessible or doesn't exist"
	fi
}

# Test 11: Nginx error log check
test_error_logs() {
	log_section "Test 11: Error Log Analysis"
	TESTS_TOTAL=$((TESTS_TOTAL + 1))

	log_test "Checking recent nginx error logs"

	if [ -r "${NGINX_ERROR_LOG}" ]; then
		local recent_errors
		recent_errors=$(sudo tail -100 "${NGINX_ERROR_LOG}" 2>/dev/null | grep -i "\[error\]" | tail -5)

		if [ -z "${recent_errors}" ]; then
			log_pass "No recent errors found in nginx error log"
		else
			log_warn "Recent errors found in nginx log:"
			echo "${recent_errors}" | while IFS= read -r line; do
				echo "  ${line}"
			done
		fi
	else
		log_skip "Error log not accessible: ${NGINX_ERROR_LOG}"
	fi
}

test_port_listening() {
	log_section "Test 12: Port Listening Verification"
	TESTS_TOTAL=$((TESTS_TOTAL + 1))

	log_test "Verifying nginx is listening on expected ports"

	local ports_ok=0
	local ports_expected=0

	# Check port 80
	if sudo netstat -tlnp 2>/dev/null | grep -q ":${HTTP_PORT}.*nginx" || sudo ss -tlnp 2>/dev/null | grep -q ":${HTTP_PORT}.*nginx"; then
		log_pass "Nginx listening on port ${HTTP_PORT} (HTTP)"
		ports_ok=$((ports_ok + 1))
	else
		log_fail "Nginx NOT listening on port ${HTTP_PORT}"
	fi
	ports_expected=$((ports_expected + 1))

	# Check port 443
	if sudo netstat -tlnp 2>/dev/null | grep -q ":${HTTPS_PORT}.*nginx" || sudo ss -tlnp 2>/dev/null | grep -q ":${HTTPS_PORT}.*nginx"; then
		log_pass "Nginx listening on port ${HTTPS_PORT} (HTTPS)"
		ports_ok=$((ports_ok + 1))
	else
		log_fail "Nginx NOT listening on port ${HTTPS_PORT}"
	fi
	ports_expected=$((ports_expected + 1))

	if sudo netstat -tlnp 2>/dev/null | grep nginx | grep -v ":${HTTP_PORT}\|:${HTTPS_PORT}" >/dev/null ||
		sudo ss -tlnp 2>/dev/null | grep nginx | grep -v ":${HTTP_PORT}\|:${HTTPS_PORT}" >/dev/null; then
		log_info "Additional ports detected:"
		sudo netstat -tlnp 2>/dev/null | grep nginx | grep -v ":${HTTP_PORT}\|:${HTTPS_PORT}" ||
			sudo ss -tlnp 2>/dev/null | grep nginx | grep -v ":${HTTP_PORT}\|:${HTTPS_PORT}" || true
	fi
}

test_swagger_ui() {
	log_section "Test 13: Swagger UI Accessibility"
	TESTS_TOTAL=$((TESTS_TOTAL + 1))

	log_test "Testing Swagger UI endpoint on backend service"

	local result
	result=$(execute_curl "http://localhost:${BACKEND_PORT}/swagger-ui/index.html" "")
	local exit_code="${result%%|*}"
	local output_file="${result##*|}"

	if [ "${exit_code}" -eq 0 ]; then
		local output
		output=$(cat "${output_file}")

		if echo "${output}" | grep -q "Swagger UI"; then
			log_pass "Swagger UI is accessible on backend port ${BACKEND_PORT}"
		else
			log_warn "Swagger UI endpoint accessible but content unexpected"
		fi
	else
		log_skip "Swagger UI endpoint not accessible on port ${BACKEND_PORT}"
	fi

	rm -f "${output_file}"

	log_test "Checking if Swagger UI is exposed through nginx gateway"
	result=$(execute_curl "https://${TEST_HOST}/swagger-ui/index.html" "-k")
	exit_code="${result%%|*}"
	output_file="${result##*|}"

	if [ "${exit_code}" -eq 0 ]; then
		local output
		output=$(cat "${output_file}")

		if echo "${output}" | grep -q "Swagger UI"; then
			log_pass "Swagger UI is also accessible through nginx gateway"
		else
			log_info "Nginx gateway accessible but Swagger UI not exposed (OK for security)"
		fi
	else
		log_info "Swagger UI not exposed through nginx gateway (OK for security)"
	fi

	rm -f "${output_file}"
}

display_test_summary() {
	log_section "Test Results Summary"

	echo ""
	echo "Total Tests: ${TESTS_TOTAL}"
	echo -e "${GREEN}Passed: ${TESTS_PASSED}${NC}"
	echo -e "${RED}Failed: ${TESTS_FAILED}${NC}"
	echo -e "${YELLOW}Skipped: ${TESTS_SKIPPED}${NC}"
	echo ""

	local pass_rate=0
	if [ ${TESTS_TOTAL} -gt 0 ]; then
		pass_rate=$((TESTS_PASSED * 100 / TESTS_TOTAL))
	fi

	echo "Pass Rate: ${pass_rate}%"
	echo ""

	if [ ${TESTS_FAILED} -eq 0 ]; then
		echo -e "${GREEN}All tests passed! Nginx API gateway is working correctly.${NC}"
		echo ""
		return 0
	else
		echo -e "${RED}Some tests failed. Please review the output above.${NC}"
		echo ""
		log_warn "Run with -v or --verbose flag to see detailed output"
		echo ""
		return 1
	fi
}

main() {
	log_section "VaikaParts Nginx API Gateway Test Suite"
	echo "Testing nginx at: ${TEST_HOST}"
	echo "Started at: $(date '+%Y-%m-%d %H:%M:%S')"
	if [ "${VERBOSE_MODE}" = true ]; then
		echo "(Verbose mode enabled)"
	fi
	echo ""

	if ! command -v curl &>/dev/null; then
		log_fail "curl is not installed. Please install it to run tests."
		exit 1
	fi

	if ! command -v systemctl &>/dev/null; then
		log_warn "systemctl not available. Some tests may be skipped."
	fi

	test_nginx_service_status
	test_nginx_configuration
	test_http_redirect
	test_https_connectivity
	test_ssl_certificate
	test_backend_connectivity
	test_rate_limiting
	test_security_headers
	test_modsecurity
	test_upstream_config
	test_error_logs
	test_port_listening
	test_swagger_ui

	display_test_summary
	local result=$?

	echo ""
	log_info "Test completed at: $(date '+%Y-%m-%d %H:%M:%S')"
	echo ""

	exit ${result}
}

# Run tests
main "$@"
