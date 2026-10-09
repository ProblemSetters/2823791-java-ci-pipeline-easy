#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

cat > "$SCRIPT_DIR/data/pipeline.json" <<'EOF'
{
  "stages": ["build","test","deploy"],
  "runs": [
    {"run":"R41","stage":"deploy","status":"passed","results":[{"stage":"build","result":"pass"},{"stage":"test","result":"pass"},{"stage":"deploy","result":"pass"}]},
    {"run":"R42","stage":"test","status":"failed","results":[{"stage":"build","result":"pass"},{"stage":"test","result":"fail"}]},
    {"run":"R43","stage":"test","status":"running","results":[{"stage":"build","result":"pass"}]},
    {"run":"R44","stage":"deploy","status":"running","results":[{"stage":"build","result":"pass"},{"stage":"test","result":"pass"}]},
    {"run":"R45","stage":"deploy","status":"failed","results":[{"stage":"build","result":"pass"},{"stage":"test","result":"pass"},{"stage":"deploy","result":"fail"}]},
    {"run":"R46","stage":"build","status":"running","results":[]},
    {"run":"R47","stage":"deploy","status":"passed","results":[{"stage":"build","result":"pass"},{"stage":"test","result":"fail"},{"stage":"test","result":"pass"},{"stage":"deploy","result":"pass"}]},
    {"run":"R49","stage":"build","status":"failed","results":[{"stage":"build","result":"fail"}]},
    {"run":"R50","stage":"test","status":"running","results":[{"stage":"build","result":"fail"},{"stage":"build","result":"pass"}]},
    {"run":"R51","stage":"test","status":"failed","results":[{"stage":"build","result":"pass"},{"stage":"test","result":"fail"}]},
    {"run":"R410","stage":"deploy","status":"running","results":[{"stage":"build","result":"pass"},{"stage":"test","result":"pass"},{"stage":"deploy","result":"fail"}]},
    {"run":"R510","stage":"deploy","status":"passed","results":[{"stage":"build","result":"pass"},{"stage":"test","result":"pass"},{"stage":"deploy","result":"fail"},{"stage":"deploy","result":"fail"},{"stage":"deploy","result":"pass"}]}
  ]
}
EOF

echo "Restored data/pipeline.json to its initial state"
