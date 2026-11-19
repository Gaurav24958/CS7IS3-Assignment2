# CS7IS3 Assignment 2: Lucene search over FT, LA Times, FBIS and Federal Register

Group project: build a Lucene index over four TREC news collections (about 528,000 documents), turn topics into queries, and write the best 1,000 results per topic in TREC run format. Phase 1 deadline: **Fri 20 Nov 2026**. Matching against the qrels is on **DOCNO**, so DOCNO must be trimmed and exact everywhere.

This README was written by Person 1 when the repo was still empty. It records what exists, what is pending and why.

## Update 18 Nov: Person 4 has left, Person 1 now owns the Person 4 files
The repo setup, run writer, validation, evaluation scripts, run log and journal are now maintained by Person 1. Everyone else: send P1 a message if you need a change in those files. The rules below still apply to everyone.

## Rules for everyone (originally written as notes for Person 4)
Person 1 pushed first versions of `pom.xml`, `.gitignore` and `README.md` because the repo was empty and the code needed something to build with, so:

- **`git pull` before you add anything.** Merge with what is here; do not overwrite or force-push.
- **`pom.xml` is a placeholder.** Improve it as you like, but keep `lucene-core`, `lucene-analysis-common` and `lucene-queryparser`, and keep the Lucene version (10.2.2, needs Java 21+) unless the whole team agrees to change it. Everyone must use the same version.
- **Keep the `.gitignore` entries** for `/index/`, `/index_test/`, `/target/` (anchored with a leading slash, otherwise `index/` also ignores the source package `cs7is3/index`), the data folder (`Assignment Two/`) and run files. Never commit the index, the collection or results files.
- **Do not re-create the class files that already exist** (listed under "Done"). Add only the files owned by other people that are still missing (see "Not in the repo yet").
- **README:** add the parts you own (Java and Lucene versions, trec_eval command, run scripts). Edit this file, do not replace it.
- No hard-coded paths. Data folder, index folder, topics file and output file are command-line arguments.

## Layout (agreed team plan)
`src/main/java/cs7is3/`

| File | Owner | Status |
|---|---|---|
| `parse/ParsedDoc.java` | P1 | done |
| `AnalyzerFactory.java` | P1 | done |
| `parse/ParseUtil.java` | P1 | done (small shared helper, not in the original plan) |
| `parse/FtParser.java` | P1 | done |
| `parse/LaTimesParser.java` | P1 | done |
| `index/Indexer.java` | P1 | done, all four sources (full index 528,155 documents) |
| `topics/Topic.java`, `topics/TopicParser.java`, `query/QueryBuilder.java` | P2 | done |
| `parse/FbisParser.java`, `parse/Fr94Parser.java`, `search/Searcher.java` | P3 | done |
| `output/RunWriter.java` | P4 (now P1) | done |
| `scripts/validate_run.py`, `scripts/eval.bat`, `scripts/eval.sh`, `results/run_log.csv` | P4 (now P1) | done |
| `Main.java`, `scripts/run_all.bat`, `scripts/run_all.sh` | P4 (now P1) | done |

## Done (Person 1, tasks A1 to A5)
- **ParsedDoc**: plain object with `docno`, `title`, `text`, `date`, `source`.
- **AnalyzerFactory.create()**: returns an `EnglishAnalyzer` (English stop words, possessive removal, Porter stemming). **Indexing and query building must both use it** so they match. P2: use this in `QueryBuilder`.
- **FtParser**: `List<ParsedDoc> parseFile(Path)`. Splits on `<DOC>`, takes DOCNO (trimmed), HEADLINE as title, TEXT as text, DATE as date, source `FT`. Skips `readmeft` and `readfrcg`. **210,158 documents** (expected about 210,000).
- **LaTimesParser**: same interface. Strips `<P>` tags, trims the space-padded DOCNO, source `LATIMES`. **131,896 documents** (expected about 131,900).
- **Indexer**: `FSDirectory` + `IndexWriter` with `AnalyzerFactory`. Prints a count per source, the total, bad and duplicate DOCNO counts, and the time. Test build of FT + LA Times: **342,054 documents in 106 s, 0 bad DOCNOs, 0 duplicates**. A quick BM25 query for "Ireland peace talks" on the test index returns results.

Index fields:

| Field | Type | Stored |
|---|---|---|
| `docno` | StringField (exact, not analysed) | yes |
| `title` | TextField (analysed) | no |
| `text` | TextField (analysed) | no |
| `source` | StringField (FT, LATIMES, FBIS, FR94) | yes |
| `date` | StringField | yes |

Note: FT dates look like `910514`, LA Times dates are free text (`January 1, 1989, Sunday, Home Edition`). The date is stored as-is and not used for searching.

## Done (Person 1, 19 Nov): full index
- **A6:** `Indexer` now has all four parsers (`ft,latimes,fbis,fr94` is the default). Full index built from the real data: FT 210,158, LA Times 131,896, FBIS 130,471, FR94 55,630 = **528,155 documents, 0 bad DOCNOs, 0 duplicates** (took 534 s on one laptop; P3 measured 76 s on another machine, so timing depends on disk and CPU).
- P3's parsers expose `List<ParsedDoc> parseFile(Path)` and return trimmed DOCNOs, so they plugged in without changes.

## Baseline run, 19 Nov
`baseline_v1`: BM25 (k1 1.2, b 0.75), query mode TD, title boost 1.5. 50 topics x 1,000 results, written in about 2 seconds, passes `validate_run.py`. Checks done by P1:
- All 50,000 DOCNOs match their source's format (`FT911-1`, `LA010189-0001`, `FBIS3-1`, `FR940104-0-00001`), none with spaces.
- All four sources appear in the results: FBIS 17,262, FT 14,459, LA Times 13,971, FR94 4,308 of the 50,000. Top 10 only: FBIS 201, LA Times 184, FT 103, FR94 12 of 500. Two topics have no FR94 document at all.
- Top 5 read by hand for topics 403 (osteoporosis), 404 (Ireland peace talks) and 410 (Schengen delays): all on topic.
- MAP is not known yet. The qrels arrive on 20 Nov.
- Observed, not fixed (idea for after the first MAP): FBIS texts start with `Language: English Article Type:BFN` header words that are not part of the article.

## Done (Persons 2 and 3, 18 Nov)
**P3 parsers**, same interface as the FT and LA Times parsers, each with a test main that prints the count and a sample:
- **FbisParser**: title from `<TI>`, date from `<DATE1>`, text from `<ABS>` + `<TEXT>`. The `<F P=100>` to `<F P=105>` tags (country, ID codes, source, language) are metadata and are removed; `<F P=106>`/`<F P=107>` are kept as text. Entities other than `&amp; &lt; &gt;` (`&yen;`, `&pound;`) become spaces. **130,471 documents** (expected about 130,500), 3 with an empty title.
- **Fr94Parser**: removes the `<!-- PJG ... -->` typesetting comments and the boilerplate blocks `BILLING, FRFILING, RINDOCK, CFRNO, SIGNER, SIGNJOB`; decodes `&hyph;` to `-` and `&blank;`, `&sect;`, `&para;` etc. to spaces (otherwise "hyph" and "blank" would be indexed as words, 500k+ times). Title from `<DOCTITLE>` (10,953 of the documents have one). The `<DATE>` tag holds effective dates, so the date comes from the DOCNO (`FR940104-0-00001` -> `940104`). **55,630 documents** (expected about 55,600), 161 with empty text (volume header pages), 0 with leftover PJG comments or entities.

**Full index test** (done by P3 before the Indexer was updated): 528,155 documents in 76 s, 0 bad DOCNOs, 0 duplicates. Reproduced by P1 on 19 Nov with the committed `Indexer`.

**P2 topics and queries**
- **TopicParser**: `List<Topic> parse(Path)`; `Topic` is a record `(int number, String title, String desc, String narrative)` with the "Description:"/"Narrative:" prefixes removed and whitespace collapsed. 50 topics (401-450), none with an empty section.
- **QueryBuilder**: `Query build(Topic)`. Mode `T`, `TD` (default) or `TDN`. Terms go through `AnalyzerFactory`, are weighted by section (title 1.0, description 0.5, narrative 0.25, summed if a term repeats) and searched in `text` and in `title` (title field boost 1.5). Task words ("documents", "relevant", "discuss", "what" ...) are dropped from the description and narrative. In `TDN`, narrative sentences that say "not relevant" are dropped. The query is built from analyzed terms, not QueryParser, so topic punctuation cannot break it. `buildFallback(Topic)` returns the broader `TDN` query (see Searcher).

**P3 Searcher**
- `new Searcher(indexDir, Searcher.similarity("bm25"))`, then `List<RunWriter.Result> search(Query, Query fallback, int n)`. DOCNO is read from the stored field.
- Models by spec string: `bm25`, `bm25:K1,B`, `lmd[:MU]` (default 1000), `lmjm[:LAMBDA]` (default 0.7), `classic`. No re-index needed to switch.
- If the main query matches fewer than n documents (e.g. title-only `lmd` on topic 403 "osteoporosis" matches only 85), the list is topped up with unseen results of the fallback query, scored just below the last real result, so every topic has exactly 1,000 lines.
- `Searcher.main` is a stand-alone test entry point; the normal way to run a search is `Main search` (below). Both call the same classes. All of `bm25 TD`, `bm25 T`, `bm25:0.9,0.4 TDN`, `lmd T`, `lmjm:0.7 TD` and `classic TD` produce run files that pass `validate_run.py` (50 topics x 1,000), in under a second each.

## Build and run
Requires Java 21+ (tested on Java 25) and Maven.

```
mvn compile
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt
```

Parser check (prints the count and one sample document):
```
java -cp "target/classes;$(cat cp.txt)" cs7is3.parse.FtParser "<path>/Assignment Two/ft"
java -cp "target/classes;$(cat cp.txt)" cs7is3.parse.LaTimesParser "<path>/Assignment Two/latimes"
```

**Everything in one command** (build, index, search, validate). Windows:
```
scripts\run_all.bat "<path>\Assignment Two" index "<path>\topics" results\baseline_v1.run baseline_v1 bm25 TD
```
Linux, macOS or WSL: `bash scripts/run_all.sh <same arguments>`. Set `SKIP_INDEX=1` first to reuse an existing index. Arguments: data folder, index folder, topics file, output run file, run name, optional model (`bm25`, `bm25:K1,B`, `lmd:MU`, `lmjm:LAMBDA`, `classic`) and query mode (`T`, `TD`, `TDN`).

**Step by step** with `Main` (mode `index` or `search`):
```
java -cp "target/classes;$(cat cp.txt)" cs7is3.Main index "<path>/Assignment Two" index
java -cp "target/classes;$(cat cp.txt)" cs7is3.Main search index "<path>/topics" results/baseline_v1.run baseline_v1 bm25 TD
python scripts/validate_run.py results/baseline_v1.run
```
`index` arguments: data folder, index folder, optional comma-separated sources (default `ft,latimes,fbis,fr94`). `search` arguments: index folder, topics file, output file, run name, optional model, query mode and title boost. On Linux or macOS use `:` instead of `;` in the classpath. The data folder is the one that contains `ft/`, `latimes/`, `fbis/` and `fr94/`. The index folder is created locally and must not be committed; neither are the topics file or the qrels.

## Run files, validation and evaluation (Person 4 tasks, done by P1)

**RunWriter** (`output/RunWriter.java`) writes one line per result: `topicNumber Q0 DOCNO rank score runName`, space separated, rank from 1 in the order given (pass results best first), score with a dot decimal. Use it as:
```java
try (RunWriter w = RunWriter.open(Path.of("results/baseline_v1.run"), "baseline_v1")) {
    w.write(404, List.of(new RunWriter.Result("FT911-1", 12.3456f), ...));
}
```
`RunWriter.Result(String docno, float score)` is a tiny record. P3: adapt `Searcher` output to it with one line, or tell P1 if you would rather have a different type.

**Validate every run file before it is shared** (Python 3, no packages needed):
```
python scripts/validate_run.py results/baseline_v1.run
```
It checks exactly 1,000 lines per topic, no repeated DOCNO per topic, ranks 1..1000, scores never increasing, no blank DOCNO, six fields per line, and one run name. It exits with a non-zero code and prints the problem if anything is wrong. Use `--per-topic N` for small tests.

**trec_eval (Windows via WSL2).** One-time build, in the Ubuntu terminal (needs `build-essential` and `git`):
```
git clone https://github.com/usnistgov/trec_eval.git ~/trec_eval
cd ~/trec_eval && make
```
Test on the fake files in `scripts/test_trec_eval/` (prints `map all 1.0000`):
```
scripts\eval.bat scripts\test_trec_eval\qrels.txt scripts\test_trec_eval\run.txt
```
Same thing on Linux, macOS or inside WSL:
```
bash scripts/eval.sh scripts/test_trec_eval/qrels.txt scripts/test_trec_eval/run.txt
```
`eval.bat` converts the Windows paths for WSL and runs `~/trec_eval/trec_eval -m map <qrels> <run>`. Add extra trec_eval options after the two paths in `eval.sh`, for example `-q` for per-topic scores. Matching is on **DOCNO**, not DOCID.

**Run log:** add one row to `results/run_log.csv` for every run (`date,run name,who,what changed,MAP,notes`). Run files themselves (`results/*.run`) are not committed.

## Still pending, and why
| Item | Why |
|---|---|
| D11 Friday: run trec_eval on the qrels, log MAP | The qrels arrive on 20 Nov. Command: `scripts\eval.bat <qrels> results\baseline_v1.run`. |
| Submit the run file for the league table | How to submit is an open question for the lecturer. |
| Tag the final version (e.g. `phase1`) | After the branch is merged to `main` and the team agrees on the version. |
| Fresh-clone check on a second machine, second person confirms the trec_eval command | Needs another team member. |
| Journal entry 1 blanks (group name, meeting date, participants), paper contributions | Facts only the team has. |
| Tuning, query expansion, model comparison | On purpose not yet: no qrels to measure against, and only half the topics are judged, so tune with care to avoid overfitting. |
