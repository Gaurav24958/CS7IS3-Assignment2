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
  - Work division agreed (see `Claude outputs/CS7IS3_Assignment2_Phase1_Plan.docx` for the four-person split and the shared file names and formats)
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

### Entry 4: Persons 2 and 3 progress, 18 Nov 2026
- Pushed `FbisParser`, `Fr94Parser` (P3), `Topic`, `TopicParser`, `QueryBuilder` (P2) and `Searcher` (P3) on branch `p2-p3-topics-search`.
- FbisParser: 130,471 documents. Fr94Parser: 55,630 documents, with PJG comments, boilerplate blocks and SGML entities (`&hyph;` 507k times, `&blank;` 241k times) removed.
- Full test index with all four sources: 528,155 documents in 76 s, 0 bad and 0 duplicate DOCNOs.
- Baseline run `baseline_v1` (BM25, title + description) and test runs with BM25 variants, LM Dirichlet, LM Jelinek-Mercer and TF-IDF all pass `validate_run.py`.
- Problem found and fixed: a title-only query for a rare word (topic 403, "osteoporosis") matched only 85 documents, below the required 1,000. Short lists are now topped up from a broader title + description + narrative query.
- Pending: P1 to enable FBIS and FR94 in `Indexer` (A6) and write `Main`; MAP once the qrels arrive on 20 Nov.

## Design decisions

### Indexing (P1)
- **Fields:** `docno` (stored, exact string, not analysed) so results can be matched against the qrels; `title` and `text` (analysed, not stored, to keep the index small); `source` and `date` (stored, exact).
- **Analyzer:** Lucene `EnglishAnalyzer` (English stop words, possessive removal, Porter stemming), created in one place (`AnalyzerFactory`) and used for both indexing and query building so they always match.
- **Why:** a standard, well-understood baseline for news text. Tuning (other stemmers, phrase handling, different stop lists) comes after the first league table, not before.
- **Parsing:** plain regex splitting on `<DOC>`; the tags are SGML, not well-formed XML, so an XML parser would fail. Files are read as ISO-8859-1 so no byte is rejected. Entities `&amp; &lt; &gt;` are decoded.

### Query strategy (P2)
- **Baseline (mode TD):** title + description. Title words are chosen by the topic author and are the most reliable, so they get weight 1.0; description words add context (weight 0.5). A word used in both adds up.
- **Same analyzer as the index** (`AnalyzerFactory`), so query terms are stemmed exactly like document terms. The query is built term by term (no QueryParser), so punctuation in topics cannot cause parse errors.
- **Task words removed** from the description and narrative ("documents", "relevant", "discuss", "find", "what" ...): they describe the search task, not the subject.
- **Fields:** every term is searched in `text` and in `title`, with title matches boosted 1.5x, since a word in the headline usually means the article is about it.
- **Narrative left out of the baseline:** it often says what is *not* relevant ("... are not relevant"), and adding those words would pull in exactly the wrong documents. Mode TDN adds only the narrative sentences without "not relevant", at weight 0.25.
- **Fallback:** if the query matches fewer than 1,000 documents, the list is topped up from the broader TDN query.
- **After 20 Nov:** compare T / TD / TDN, tune the section weights and the title boost, then pseudo-relevance feedback (Rocchio / RM3: add the best terms from the top 5-10 documents and search again). The weights above are starting values, not tuned.

### Retrieval model (P3)
- **Baseline:** BM25 with Lucene's defaults (k1 = 1.2, b = 0.75). BM25 is the standard strong baseline for TREC ad hoc news retrieval and handles the very different document lengths (short FBIS notes vs long Federal Register rules) through b.
- **Switchable without code changes or re-indexing:** `bm25:K1,B`, `lmd:MU` (LM Dirichlet), `lmjm:LAMBDA` (LM Jelinek-Mercer) and `classic` (TF-IDF). All produce valid runs.
- **Parsing choices that affect retrieval:** FR94 typesetting comments, filing/billing codes and entities are stripped; FBIS metadata tags are removed so that country, source and ID codes are not counted as article words.
- **After 20 Nov:** grid over BM25 k1 (0.6-1.6) and b (0.3-0.9), LM Dirichlet mu (500-3000) and Jelinek-Mercer lambda; measure the effect of the FR94 cleanup; possibly fuse runs from different models. To avoid overfitting to the 25 judged topics, prefer settings that work well over a wide range.

### Evaluation setup (P1 as P4)
- trec_eval 10.0-rc3 built from https://github.com/usnistgov/trec_eval in WSL2 Ubuntu. Test on fake data prints `map all 1.0000`.
- Run files are checked with `scripts/validate_run.py` before they are shared. Every run is logged in `results/run_log.csv`.

## Contribution tables

### Implementation
| Person | Date | Contribution |
|---|---|---|
| P1 | 17 Nov | ParsedDoc, AnalyzerFactory, ParseUtil, FtParser, LaTimesParser, Indexer (FT + LA Times), repo first push, README |
| P1 (as P4) | 18 Nov | trec_eval build and test, RunWriter, validate_run.py, eval scripts, run_log.csv, journal |
| P2 | 18 Nov | Topic, TopicParser, QueryBuilder (T / TD / TDN modes, section weights, title boost, fallback query) |
| P3 | 18 Nov | FbisParser, Fr94Parser, Searcher (BM25, LM Dirichlet, LM Jelinek-Mercer, TF-IDF; top-up to 1,000 results); full four-source index test |

### Research paper (due 4 Dec 2026)
| Person | Date | Notes or section contribution |
|---|---|---|
| P1 | ____ | ____ |
| P2 | ____ | ____ |
| P3 | ____ | ____ |
