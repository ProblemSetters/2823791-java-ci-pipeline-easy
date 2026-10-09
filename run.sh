#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

mkdir -p data

mvn -B -ntp -DskipTests package 2>&1

echo ""
echo "State file: data/pipeline.json"
echo ""
echo "Usage:"
echo "  ./pipeline start <run>"
echo "  ./pipeline stage <run> <pass|fail>"
echo "  ./pipeline retry <run>"
echo ""
echo "Reset:"
echo "  bash reset.sh"
