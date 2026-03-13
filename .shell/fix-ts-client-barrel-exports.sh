#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly SCRIPT_DIR

PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
readonly PROJECT_ROOT

readonly GENERATED_DIR="${PROJECT_ROOT}/build/generated/openapi-ts-client"
readonly APIS_BARREL="${GENERATED_DIR}/src/apis/index.ts"
readonly MODELS_BARREL="${GENERATED_DIR}/src/models/index.ts"

log_info() { echo "[INFO]  $*" >&2; }
log_error() { echo "[ERROR] $*" >&2; }

validate_environment() {
	log_info "Validating generated client directory..."

	if [[ ! -d "${GENERATED_DIR}" ]]; then
		log_error "Generated client directory not found: '${GENERATED_DIR}'."
		log_error "Run './gradlew generateTypeScriptClient' first."
		exit 1
	fi

	if ! command -v node >/dev/null 2>&1; then
		log_error "Node.js is not installed or not on PATH."
		exit 1
	fi

	log_info "Validation passed."
}

rewrite_barrel() {
	local barrel_file="${1}"

	if [[ ! -f "${barrel_file}" ]]; then
		log_info "Barrel file not found, skipping: '${barrel_file}'."
		return 0
	fi

	log_info "Rewriting barrel: '${barrel_file}'..."

	node --input-type=module <<EOF
    import { readFileSync, writeFileSync, readdirSync } from 'fs';
    import { resolve, dirname, basename } from 'path';

    const barrelPath = '${barrel_file}';
    const barrelDir  = dirname(barrelPath);
    const content    = readFileSync(barrelPath, 'utf8');

    const starExportRe = /^export\s+\*\s+from\s+['"](\.\/.+)['"]\s*;?\s*$/gm;
    const modules = [];
    let match;
    while ((match = starExportRe.exec(content)) !== null) {
        const specifier = match[1];
        const moduleName = basename(specifier);
        const tsFile = resolve(barrelDir, specifier + '.ts');
        modules.push({ specifier, moduleName, tsFile });
    }

    if (modules.length === 0) {
        process.stdout.write('[INFO]  No wildcard exports found — barrel left unchanged.\n');
        process.exit(0);
    }

    const exportedNamesRe = /export\s+(?:type\s+)?(?:\{([^}]+)\}|(?:class|interface|enum|function|const|let|var|type|abstract\s+class)\s+([A-Za-z_$][A-Za-z0-9_$]*))/g;

    const moduleExports = new Map(); // moduleName -> Set<string>

    for (const { specifier, moduleName, tsFile } of modules) {
        let src;
        try {
            src = readFileSync(tsFile, 'utf8');
        } catch {
            moduleExports.set(moduleName, null);
            continue;
        }

        const names = new Set();
        let m;
        while ((m = exportedNamesRe.exec(src)) !== null) {
            if (m[1]) {
                for (const part of m[1].split(',')) {
                    const trimmed = part.trim();
                    if (!trimmed) continue;
                    const asParts = trimmed.split(/\s+as\s+/);
                    names.add(asParts[asParts.length - 1].trim());
                }
            } else if (m[2]) {
                names.add(m[2]);
            }
        }

        moduleExports.set(moduleName, names);
    }

    const identifierOwners = new Map();
    for (const [moduleName, names] of moduleExports.entries()) {
        if (names === null) continue;
        for (const name of names) {
            if (!identifierOwners.has(name)) identifierOwners.set(name, []);
            identifierOwners.get(name).push(moduleName);
        }
    }

    const collisions = new Set(
        [...identifierOwners.entries()]
            .filter(([, owners]) => owners.length > 1)
            .map(([name]) => name)
    );

    if (collisions.size > 0) {
        process.stderr.write('[INFO]  Colliding export names detected: ' + [...collisions].join(', ') + '\n');
    }

    const lines = ['// This file is auto-rewritten by fix-ts-client-barrel-exports.sh', '// Manual edits will be overwritten on the next client generation.', ''];

    for (const { specifier, moduleName, tsFile } of modules) {
        const names = moduleExports.get(moduleName);

        if (names === null) {
            lines.push(\`export * from '\${specifier}';\`);
        continue;
    }

    const exports = [];
    for (const name of names) {
        if (collisions.has(name)) {
            const alias = moduleName + name;
            exports.push(\`\${name} as \${alias}\`);
        } else {
            exports.push(name);
        }
    }

    if (exports.length === 0) continue;

    lines.push(\`export { \${exports.join(', ')} } from '\${specifier}';\`);
}

writeFileSync(barrelPath, lines.join('\n') + '\n', 'utf8');
process.stdout.write('[INFO]  Barrel rewritten with ' + modules.length + ' module(s), ' + collisions.size + ' collision(s) resolved.\n');
EOF
}

main() {
	validate_environment
	rewrite_barrel "${APIS_BARREL}"
	rewrite_barrel "${MODELS_BARREL}"
	log_info "Barrel export rewrite complete."
}

main "$@"
