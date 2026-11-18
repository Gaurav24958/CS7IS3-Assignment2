#!/usr/bin/env python3
"""Validate a TREC run file before it is shared or submitted.

Usage: python scripts/validate_run.py <run file> [--per-topic N] [--run-name NAME]

Checks (per topic, N defaults to 1000):
  * every line has exactly 6 fields: topic Q0 DOCNO rank score runName, with Q0 literal
  * exactly N lines per topic
  * DOCNO is not blank and has no spaces; no DOCNO repeated within a topic
  * ranks run 1..N in order
  * scores never increase down the list
  * one run name throughout (and equal to --run-name if given)
Exit code 0 if valid, 1 otherwise. Prints every problem found (first 20 per kind of problem).
"""
import argparse
import sys
from collections import OrderedDict


def validate(path, per_topic=1000, run_name=None):
    problems = []

    def bad(msg):
        problems.append(msg)

    topics = OrderedDict()  # topic -> list of (lineno, docno, rank, score)
    run_names = set()
    with open(path, encoding="utf-8") as f:
        for lineno, raw in enumerate(f, 1):
            line = raw.rstrip("\n").rstrip("\r")
            if not line.strip():
                bad(f"line {lineno}: blank line")
                continue
            parts = line.split(" ")
            if len(parts) != 6:
                bad(f"line {lineno}: expected 6 space-separated fields, got {len(parts)}: {line[:80]!r}")
                continue
            topic, q0, docno, rank, score, name = parts
            if q0 != "Q0":
                bad(f"line {lineno}: second field must be Q0, got {q0!r}")
            if not docno.strip():
                bad(f"line {lineno}: blank DOCNO")
            try:
                rank_i = int(rank)
                score_f = float(score)
            except ValueError:
                bad(f"line {lineno}: rank or score is not a number: {rank!r} {score!r}")
                continue
            run_names.add(name)
            topics.setdefault(topic, []).append((lineno, docno, rank_i, score_f))

    if not topics:
        bad("file contains no results")
    if len(run_names) > 1:
        bad(f"more than one run name used: {sorted(run_names)}")
    if run_name and run_names and run_names != {run_name}:
        bad(f"run name is {sorted(run_names)}, expected {run_name!r}")

    for topic, rows in topics.items():
        if len(rows) != per_topic:
            bad(f"topic {topic}: {len(rows)} lines, expected exactly {per_topic}")
        seen = set()
        prev_score = None
        for i, (lineno, docno, rank, score) in enumerate(rows, 1):
            if docno in seen:
                bad(f"topic {topic}, line {lineno}: DOCNO {docno} repeated")
            seen.add(docno)
            if rank != i:
                bad(f"topic {topic}, line {lineno}: rank is {rank}, expected {i}")
            if prev_score is not None and score > prev_score:
                bad(f"topic {topic}, line {lineno}: score {score} is higher than the previous {prev_score}")
            prev_score = score
    return problems, len(topics)


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("run_file")
    ap.add_argument("--per-topic", type=int, default=1000, help="results required per topic (default 1000)")
    ap.add_argument("--run-name", default=None, help="expected run name (optional)")
    args = ap.parse_args()
    problems, n_topics = validate(args.run_file, args.per_topic, args.run_name)
    if problems:
        for p in problems[:50]:
            print("INVALID:", p)
        if len(problems) > 50:
            print(f"... and {len(problems) - 50} more problems")
        print(f"FAILED: {len(problems)} problem(s) in {args.run_file}")
        return 1
    print(f"OK: {args.run_file} is valid ({n_topics} topics, {args.per_topic} results each)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
