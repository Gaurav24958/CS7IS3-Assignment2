#!/usr/bin/env bash
# Builds the code, builds the index, writes the run file and validates it (Linux, macOS, WSL).
# Usage: scripts/run_all.sh <dataFolder> <indexFolder> <topicsFile> <outputFile> <runName> [model] [T|TD|TDN]
# Set SKIP_INDEX=1 to reuse an existing index folder.
set -e
[ $# -ge 5 ] || { echo "usage: $0 <dataFolder> <indexFolder> <topicsFile> <outputFile> <runName> [model] [mode]"; exit 1; }
DATA="$1"; INDEX="$2"; TOPICS="$3"; OUT="$4"; RUN="$5"; MODEL="${6:-bm25}"; MODE="${7:-TD}"
cd "$(dirname "$0")/.."
echo "== 1/4 build"
mvn -q compile
mvn -q dependency:build-classpath -Dmdep.outputFile=cp.txt
CP="target/classes:$(cat cp.txt)"
if [ "$SKIP_INDEX" = "1" ]; then echo "== 2/4 index skipped"; else
  echo "== 2/4 index"; java -cp "$CP" cs7is3.Main index "$DATA" "$INDEX"
fi
echo "== 3/4 search"
java -cp "$CP" cs7is3.Main search "$INDEX" "$TOPICS" "$OUT" "$RUN" "$MODEL" "$MODE"
echo "== 4/4 validate"
python3 scripts/validate_run.py "$OUT"
echo "DONE. Next: bash scripts/eval.sh <qrels> $OUT"
