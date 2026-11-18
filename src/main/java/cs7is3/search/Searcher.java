package cs7is3.search;

import cs7is3.output.RunWriter;
import cs7is3.query.QueryBuilder;
import cs7is3.topics.Topic;
import cs7is3.topics.TopicParser;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.*;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class Searcher implements AutoCloseable {
    public static final int TOP_N = 1000;

    private final DirectoryReader reader;
    private final IndexSearcher searcher;

    public Searcher(Path indexDir, Similarity similarity) throws IOException {
        this.reader = DirectoryReader.open(FSDirectory.open(indexDir));
        this.searcher = new IndexSearcher(reader);
        this.searcher.setSimilarity(similarity);
    }

    public List<RunWriter.Result> search(Query query, int n) throws IOException {
        TopDocs top = searcher.search(query, n);
        StoredFields stored = searcher.storedFields();
        List<RunWriter.Result> results = new ArrayList<>(top.scoreDocs.length);
        for (ScoreDoc sd : top.scoreDocs) {
            String docno = stored.document(sd.doc, Set.of("docno")).get("docno");
            results.add(new RunWriter.Result(docno, sd.score));
        }
        return results;
    }

    public List<RunWriter.Result> search(Query query, Query fallback, int n) throws IOException {
        List<RunWriter.Result> results = search(query, n);
        if (results.size() >= n || fallback == null) return results;
        Set<String> seen = new HashSet<>();
        for (RunWriter.Result r : results) seen.add(r.docno());
        float floor = results.isEmpty() ? 0f : results.get(results.size() - 1).score();
        int added = 0;
        for (RunWriter.Result r : search(fallback, n + results.size())) {
            if (results.size() >= n) break;
            if (!seen.add(r.docno())) continue;
            added++;
            results.add(new RunWriter.Result(r.docno(), floor - added * 0.001f));
        }
        return results;
    }

    public int numDocs() {
        return reader.numDocs();
    }

    public static Similarity similarity(String spec) {
        String[] parts = spec.toLowerCase(Locale.ROOT).split(":", 2);
        String[] p = parts.length > 1 ? parts[1].split(",") : new String[0];
        return switch (parts[0]) {
            case "bm25" -> p.length == 2
                    ? new BM25Similarity(Float.parseFloat(p[0]), Float.parseFloat(p[1]))
                    : new BM25Similarity();
            case "lmd" -> new LMDirichletSimilarity(p.length == 1 ? Float.parseFloat(p[0]) : 1000f);
            case "lmjm" -> new LMJelinekMercerSimilarity(p.length == 1 ? Float.parseFloat(p[0]) : 0.7f);
            case "classic" -> new ClassicSimilarity();
            default -> throw new IllegalArgumentException("Unknown retrieval model: " + spec
                    + " (use bm25, bm25:K1,B, lmd[:MU], lmjm[:LAMBDA] or classic)");
        };
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println("Usage: Searcher <indexFolder> <topicsFile> <runFile> [runName] [model] [T|TD|TDN] [titleFieldBoost]");
            System.exit(1);
        }
        Path indexDir = Path.of(args[0]);
        Path topicsFile = Path.of(args[1]);
        Path runFile = Path.of(args[2]);
        String runName = args.length > 3 ? args[3] : "baseline_v1";
        String model = args.length > 4 ? args[4] : "bm25";
        QueryBuilder.Mode mode = QueryBuilder.Mode.valueOf(args.length > 5 ? args[5].toUpperCase(Locale.ROOT) : "TD");
        float titleBoost = args.length > 6 ? Float.parseFloat(args[6]) : QueryBuilder.DEFAULT_TITLE_FIELD_BOOST;

        List<Topic> topics = new TopicParser().parse(topicsFile);
        QueryBuilder qb = new QueryBuilder(mode, titleBoost);
        long start = System.currentTimeMillis();
        int shortTopics = 0;
        try (Searcher s = new Searcher(indexDir, similarity(model));
             RunWriter w = RunWriter.open(runFile, runName)) {
            System.out.printf("Index: %,d documents; model %s; query mode %s; title boost %.2f; %d topics%n",
                    s.numDocs(), model, mode, titleBoost, topics.size());
            for (Topic t : topics) {
                List<RunWriter.Result> results = s.search(qb.build(t), qb.buildFallback(t), TOP_N);
                if (results.size() < TOP_N) {
                    shortTopics++;
                    System.err.printf("NOTE: topic %d has only %d results even after the fallback query%n",
                            t.number(), results.size());
                }
                w.write(t.number(), results);
            }
        }
        System.out.printf("Wrote %s (%d topics, %d with fewer than %d results) in %.1f seconds%n",
                runFile, topics.size(), shortTopics, TOP_N, (System.currentTimeMillis() - start) / 1000.0);
    }
}
