package cs7is3.output;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Writes results in TREC run format, one result per line, space separated:
 * {@code topicNumber Q0 DOCNO rank score runName}, e.g. {@code 404 Q0 FT911-1 1 12.3456 baseline_v1}.
 * Rank starts at 1 and follows the order of the list given, so pass results best first.
 */
public class RunWriter implements AutoCloseable {

    /** One ranked result: the DOCNO and its score. Searcher output can be adapted with one line. */
    public record Result(String docno, float score) {}

    private final Writer out;
    private final String runName;

    public RunWriter(Writer out, String runName) {
        this.out = out;
        this.runName = runName;
    }

    /** Opens (and overwrites) the given file as UTF-8. */
    public static RunWriter open(Path file, String runName) throws IOException {
        if (file.getParent() != null) Files.createDirectories(file.getParent());
        BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8);
        return new RunWriter(w, runName);
    }

    /** Writes all results of one topic. Results must already be sorted best first. */
    public void write(int topicNumber, List<Result> results) throws IOException {
        int rank = 1;
        for (Result r : results) {
            String docno = r.docno().strip(); // belt and braces: DOCNO must never carry spaces
            // Locale.ROOT so the score always uses a dot, never a decimal comma.
            out.write(String.format(Locale.ROOT, "%d Q0 %s %d %.6f %s%n", topicNumber, docno, rank, r.score(), runName));
            rank++;
        }
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

    /** Small self-check: java cs7is3.output.RunWriter */
    public static void main(String[] args) throws IOException {
        java.io.StringWriter sw = new java.io.StringWriter();
        try (RunWriter w = new RunWriter(sw, "baseline_v1")) {
            w.write(404, List.of(new Result("FT911-1", 12.3456f), new Result(" LA010189-0001 ", 11.0f)));
        }
        System.out.print(sw);
    }
}
