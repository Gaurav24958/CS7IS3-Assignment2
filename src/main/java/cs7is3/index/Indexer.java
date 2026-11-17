package cs7is3.index;

import cs7is3.AnalyzerFactory;
import cs7is3.parse.FtParser;
import cs7is3.parse.LaTimesParser;
import cs7is3.parse.ParsedDoc;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Walks the collection folders, parses every file and writes the Lucene index.
 * Usage: Indexer <dataFolder> <indexFolder> [sources, e.g. ft,latimes,fbis,fr94]
 * Fields: docno (stored, exact), title (analysed), text (analysed), source (stored, exact), date (stored).
 */
public class Indexer {
    /** Expected sizes of the standard TREC collections, for the sanity comparison. */
    private static final Map<String, Integer> EXPECTED = Map.of("FT", 210000, "LATIMES", 131900, "FBIS", 130500, "FR94", 55600);

    private final Path dataDir;
    private final Path indexDir;

    public Indexer(Path dataDir, Path indexDir) {
        this.dataDir = dataDir;
        this.indexDir = indexDir;
    }

    /** folder name -> (source label, per-file parser). FBIS and FR94 are added once P3's parsers are merged. */
    private Map<String, Function<Path, List<ParsedDoc>>> parsers() {
        Map<String, Function<Path, List<ParsedDoc>>> m = new LinkedHashMap<>();
        m.put("ft", new FtParser()::parseFile);
        m.put("latimes", new LaTimesParser()::parseFile);
        // m.put("fbis", new FbisParser()::parseFile);   // TODO A6
        // m.put("fr94", new Fr94Parser()::parseFile);   // TODO A6
        return m;
    }

    public void index(Collection<String> sources) throws IOException {
        long start = System.currentTimeMillis();
        Map<String, Integer> counts = new LinkedHashMap<>();
        Set<String> seen = new HashSet<>();
        int badDocno = 0, duplicates = 0;

        IndexWriterConfig cfg = new IndexWriterConfig(AnalyzerFactory.create());
        cfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        try (IndexWriter writer = new IndexWriter(FSDirectory.open(indexDir), cfg)) {
            for (Map.Entry<String, Function<Path, List<ParsedDoc>>> e : parsers().entrySet()) {
                if (!sources.contains(e.getKey())) continue;
                List<Path> files;
                try (Stream<Path> s = Files.walk(dataDir.resolve(e.getKey()))) {
                    files = s.filter(Files::isRegularFile).sorted().toList();
                }
                for (Path f : files) {
                    for (ParsedDoc d : e.getValue().apply(f)) {
                        if (!d.docno.equals(d.docno.strip()) || d.docno.isEmpty()) badDocno++;
                        if (!seen.add(d.docno)) duplicates++;
                        writer.addDocument(toLucene(d));
                        counts.merge(d.source, 1, Integer::sum);
                    }
                }
                System.out.println("Finished folder " + e.getKey());
            }
            writer.commit();
        }

        int total = 0;
        for (Map.Entry<String, Integer> c : counts.entrySet()) {
            total += c.getValue();
            System.out.printf("%-8s %,9d documents (expected about %,d)%n", c.getKey(), c.getValue(), EXPECTED.get(c.getKey()));
        }
        System.out.printf("TOTAL    %,9d documents; bad DOCNOs: %d; duplicate DOCNOs: %d%n", total, badDocno, duplicates);
        System.out.printf("Indexing took %.1f seconds%n", (System.currentTimeMillis() - start) / 1000.0);
    }

    private static Document toLucene(ParsedDoc d) {
        Document doc = new Document();
        doc.add(new StringField("docno", d.docno, Field.Store.YES));
        doc.add(new TextField("title", d.title, Field.Store.NO));
        doc.add(new TextField("text", d.text, Field.Store.NO));
        doc.add(new StringField("source", d.source, Field.Store.YES));
        doc.add(new StringField("date", d.date, Field.Store.YES));
        return doc;
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: Indexer <dataFolder> <indexFolder> [ft,latimes,fbis,fr94]");
            System.exit(1);
        }
        List<String> sources = Arrays.asList((args.length > 2 ? args[2] : "ft,latimes").split(","));
        new Indexer(Path.of(args[0]), Path.of(args[1])).index(sources);
    }
}
