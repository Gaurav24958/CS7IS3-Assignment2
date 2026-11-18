#!/usr/bin/env bash
# Usage: scripts/eval.sh <qrels file> <run file> [extra trec_eval args]
# Runs trec_eval (built in ~/trec_eval, see README) and prints MAP.
set -e
[ $# -ge 2 ] || { echo "usage: $0 <qrels> <run> [extra trec_eval args]"; exit 1; }
TREC_EVAL="${TREC_EVAL:-$HOME/trec_eval/trec_eval}"
QRELS="$1"; RUN="$2"; shift 2
"$TREC_EVAL" -m map "$@" "$QRELS" "$RUN"
