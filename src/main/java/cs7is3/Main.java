package cs7is3;

import cs7is3.index.Indexer;
import cs7is3.output.RunWriter;
import cs7is3.query.QueryBuilder;
import cs7is3.search.Searcher;
import cs7is3.topics.Topic;
import cs7is3.topics.TopicParser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Command-line entry point that connects the pieces.
 *
 * <pre>
 * index  &lt;dataFolder&gt; &lt;indexFolder&gt; [sources]
 * search &lt;indexFolder&gt; &lt;topicsFile&gt; &lt;outputFile&gt; &lt;runName&gt; [model] [T|TD|TDN] [titleBoost]
 * </pre>
 * index: builds the Lucene index from the four collections (default all four).
 * search: TopicParser, then QueryBuilder, then Searcher (top 1,000), then RunWriter, for every topic.
 * The model is a spec such as bm25, bm25:1.2,0.75, lmd:1000, lmjm:0.7 or classic (default bm25).
 * Every path is an argument; nothing is hard-coded.
 */
public class Main {

    private static final String USAGE = """
            Usage:
              Main index  <dataFolder> <indexFolder> [sources, default ft,latimes,fbis,fr94]
              Main search <indexFolder> <topicsFile> <outputFile> <runName> [model] [T|TD|TDN] [titleBoost]
            Examples:
              Main index  "Assignment Two" index
              Main search index topics results/baseline_v1.run baseline_v1 bm25 TD""";

    public static void main(String[] args) throws IOException {
        if (args.length == 0) fail("No mode given.");
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "index" -> index(args);
            case "search" -> search(args);
            default -> fail("Unknown mode '" + args[0] + "' (use index or search).");
        }
    }

    private static void index(String[] args) throws IOException {
        if (args.length < 3) fail("index needs a data folder and an index folder.");
        String sources = args.length > 3 ? args[3] : Indexer.ALL_SOURCES;
        new Indexer(Path.of(args[1]), Path.of(args[2])).index(List.of(sources.split(",")));
    }

    private static void search(String[] args) throws IOException {
        if (args.length < 5) fail("search needs an index folder, topics file, output file and run name.");
        Path indexDir = Path.of(args[1]);
        Path topicsFile = Path.of(args[2]);
        Path outputFile = Path.of(args[3]);
        String runName = args[4];
        String model = args.length > 5 ? args[5] : "bm25";
        QueryBuilder.Mode mode = QueryBuilder.Mode.valueOf(args.length > 6 ? args[6].toUpperCase(Locale.ROOT) : "TD");
        float titleBoost = args.length > 7 ? Float.parseFloat(args[7]) : QueryBuilder.DEFAULT_TITLE_FIELD_BOOST;

        List<Topic> topics = new TopicParser().parse(topicsFile);
        QueryBuilder qb = new QueryBuilder(mode, titleBoost);
        long start = System.currentTimeMillis();
        int written = 0, shortTopics = 0;
        try (Searcher searcher = new Searcher(indexDir, Searcher.similarity(model));
             RunWriter writer = RunWriter.open(outputFile, runName)) {
            System.out.printf("Index: %,d documents; model %s; query mode %s; title boost %.2f; %d topics%n",
                    searcher.numDocs(), model, mode, titleBoost, topics.size());
            for (Topic t : topics) {
                List<RunWriter.Result> results = searcher.search(qb.build(t), qb.buildFallback(t), Searcher.TOP_N);
                if (results.size() < Searcher.TOP_N) {
                    shortTopics++;
                    System.err.printf("NOTE: topic %d has only %d results even after the fallback query%n",
                            t.number(), results.size());
                }
                writer.write(t.number(), results);
                written += results.size();
            }
        }
        System.out.printf("Wrote %s: %,d results for %d topics (%d topics short of %d) in %.1f seconds%n",
                outputFile, written, topics.size(), shortTopics, Searcher.TOP_N,
                (System.currentTimeMillis() - start) / 1000.0);
    }

    private static void fail(String message) {
        System.err.println(message);
        System.err.println(USAGE);
        System.exit(1);
    }
}
