# CS7IS3 Assignment 2: Project Journal

**Group name:** ______________________  **Group number:** ______

This journal is mandatory. Everyone logs their own work here as they go (commit small, pull first). Items marked `____` are blanks that the team still has to fill in. Nothing in a blank has been invented.

## Team and roles

| Person | Name | Role |
|---|---|---|
| P1 | ____________ | Parsing (FT, LA Times) and the index (ParsedDoc, AnalyzerFactory, FtParser, LaTimesParser, Indexer) |
| P2 | ____________ | Topics and query generation (Topic, TopicParser, QueryBuilder) |
| P3 | ____________ | Parsing (FBIS, Federal Register) and searching (FbisParser, Fr94Parser, Searcher) |
| P4 | ____________ (left the group, see entry 3) | Setup, output, evaluation, integration. Taken over by P1 from 18 Nov 2026 |

## Meeting log

### Entry 1: Kick-off meeting
- **Date:** ____ Nov 2026
- **Participants:** ____________________________________
- **Roles:** as in the table above
- **Summary:**
  - Group name chosen: ____________________
  - Work division agreed (see the team's Phase 1 plan document, "CS7IS3_Assignment2_Phase1_Plan" for the four-person split and the shared file names and formats)
  - Fixed formats: index fields `docno, title, text, source, date`; DOCNO always trimmed; run line `topic Q0 DOCNO rank score runName`; 1,000 results per topic; no hard-coded paths
  - Other decisions: ____________________________________

### Entry 2: Person 1 progress, 17 Nov 2026
- Pushed the first version of the repo (`pom.xml`, `.gitignore`, `README.md`) because it was empty, together with P1's classes.
- Built and tested on the full FT and LA Times folders:
  - FtParser: 210,158 documents (expected about 210,000).
  - LaTimesParser: 131,896 documents (expected about 131,900).
  - Indexer: test index of 342,054 documents in 106 seconds, 0 untrimmed or empty DOCNOs, 0 duplicate DOCNOs.
- A bug found and fixed while testing: a `"\s+"` regex written without the double backslash compiled (Java accepts `\s` as a space escape) but did not collapse whitespace.
- Another bug found and fixed before the first push: the ignore entry `index/` also hid the Java package `cs7is3/index`. Ignore entries are now anchored to the repo root.
- Pending: FBIS and FR94 in the Indexer (needs P3's parsers), full index of about 528,000 documents.

### Entry 3: Person 4 left the group, 18 Nov 2026
- Person 4 has left the group. P1 has taken over the Person 4 role as well as their own.
- Reassigned to P1 and done today (D-numbers from the team plan): D1 repo structure, D2 this journal, D3 trec_eval built (WSL, Ubuntu), D4 trec_eval tested on fake data, D5 `RunWriter`, D6 run validation script, D8 trec_eval wrapper scripts (`scripts/eval.bat`, `scripts/eval.sh`), D9 `results/run_log.csv`, D10 submission question drafted for the lecturer.
- Deferred until P2 and P3 have pushed: D7 `Main`, D8 `run_all` script, D11 Friday evaluation, the final checks and the `phase1` tag.

## Design decisions

### Indexing (P1)
- **Fields:** `docno` (stored, exact string, not analysed) so results can be matched against the qrels; `title` and `text` (analysed, not stored, to keep the index small); `source` and `date` (stored, exact).
- **Analyzer:** Lucene `EnglishAnalyzer` (English stop words, possessive removal, Porter stemming), created in one place (`AnalyzerFactory`) and used for both indexing and query building so they always match.
- **Why:** a standard, well-understood baseline for news text. Tuning (other stemmers, phrase handling, different stop lists) comes after the first league table, not before.
- **Parsing:** plain regex splitting on `<DOC>`; the tags are SGML, not well-formed XML, so an XML parser would fail. Files are read as ISO-8859-1 so no byte is rejected. Entities `&amp; &lt; &gt;` are decoded.

### Query strategy (P2)
____ (P2 to write: what is used from the topic, boosts, why the narrative is left out of the baseline, ideas for after 20 Nov)

### Retrieval model (P3)
____ (P3 to write: BM25 and its parameters, why, models to try after 20 Nov)

### Evaluation setup (P1 as P4)
- trec_eval 10.0-rc3 built from https://github.com/usnistgov/trec_eval in WSL2 Ubuntu. Test on fake data prints `map all 1.0000`.
- Run files are checked with `scripts/validate_run.py` before they are shared. Every run is logged in `results/run_log.csv`.

## Contribution tables

### Implementation
| Person | Date | Contribution |
|---|---|---|
| P1 | 17 Nov | ParsedDoc, AnalyzerFactory, ParseUtil, FtParser, LaTimesParser, Indexer (FT + LA Times), repo first push, README |
| P1 (as P4) | 18 Nov | trec_eval build and test, RunWriter, validate_run.py, eval scripts, run_log.csv, journal |
| P2 | ____ | ____ |
| P3 | ____ | ____ |

### Research paper (due 4 Dec 2026)
| Person | Date | Notes or section contribution |
|---|---|---|
| P1 | ____ | ____ |
| P2 | ____ | ____ |
| P3 | ____ | ____ |
