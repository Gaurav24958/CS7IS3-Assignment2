package cs7is3.parse;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class FbisParser {
    public static final String SOURCE = "FBIS";

    private static final Pattern METADATA = Pattern.compile("<F P=10[0-5]>.*?</F>", Pattern.DOTALL);
    private static final Pattern TEXT_MARKER = Pattern.compile("\\[(Text|Excerpt|Excerpts)\\]");

    private static final Pattern OTHER_ENTITY = Pattern.compile("&(?!amp;|lt;|gt;)[A-Za-z0-9]+;");

    public List<ParsedDoc> parseFile(Path file) {
        List<ParsedDoc> docs = new ArrayList<>();
        if (ParseUtil.isReadme(file)) return docs;
        try {
            for (String block : ParseUtil.splitDocs(ParseUtil.read(file))) {
                String docno = ParseUtil.clean(ParseUtil.field(block, "DOCNO"));
                if (docno.isEmpty()) continue;
                String body = ParseUtil.field(block, "ABS") + " " + ParseUtil.field(block, "TEXT");
                body = METADATA.matcher(body).replaceAll(" ");
                body = TEXT_MARKER.matcher(body).replaceAll(" ");
                docs.add(new ParsedDoc(docno,
                        clean(ParseUtil.field(block, "TI")),
                        clean(body),
                        clean(ParseUtil.field(block, "DATE1")),
                        SOURCE));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return docs;
    }

    private static String clean(String s) {
        return ParseUtil.clean(OTHER_ENTITY.matcher(s).replaceAll(" "));
    }

    public static void main(String[] args) throws IOException {
        FbisParser p = new FbisParser();
        int count = 0, emptyText = 0, emptyTitle = 0;
        ParsedDoc first = null;
        try (Stream<Path> files = Files.walk(Path.of(args[0]))) {
            for (Path f : (Iterable<Path>) files.filter(Files::isRegularFile).sorted()::iterator) {
                for (ParsedDoc d : p.parseFile(f)) {
                    if (first == null) first = d;
                    count++;
                    if (d.text.isEmpty()) emptyText++;
                    if (d.title.isEmpty()) emptyTitle++;
                }
            }
        }
        System.out.printf("FBIS documents: %,d (empty text: %d, empty title: %d)%n", count, emptyText, emptyTitle);
        if (first != null)
            System.out.println(first.docno + " | " + first.date + " | " + first.title + "\n"
                    + first.text.substring(0, Math.min(300, first.text.length())));
    }
}
