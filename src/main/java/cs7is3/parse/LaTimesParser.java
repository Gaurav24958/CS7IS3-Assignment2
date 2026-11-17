package cs7is3.parse;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Parses LA Times files (latimes/la010189 ...). Fields are wrapped in <P> tags, removed by clean(). */
public class LaTimesParser {
    public static final String SOURCE = "LATIMES";

    public List<ParsedDoc> parseFile(Path file) {
        List<ParsedDoc> docs = new ArrayList<>();
        if (ParseUtil.isReadme(file)) return docs;
        try {
            for (String block : ParseUtil.splitDocs(ParseUtil.read(file))) {
                String docno = ParseUtil.clean(ParseUtil.field(block, "DOCNO")); // padded with spaces in file
                if (docno.isEmpty()) continue;
                docs.add(new ParsedDoc(docno,
                        ParseUtil.clean(ParseUtil.field(block, "HEADLINE")),
                        ParseUtil.clean(ParseUtil.field(block, "TEXT")),
                        ParseUtil.clean(ParseUtil.field(block, "DATE")),
                        SOURCE));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return docs;
    }

    public List<ParsedDoc> parseAll(Path laDir) throws IOException {
        List<ParsedDoc> all = new ArrayList<>();
        try (Stream<Path> files = Files.walk(laDir)) {
            for (Path f : (Iterable<Path>) files.filter(Files::isRegularFile).sorted()::iterator)
                all.addAll(parseFile(f));
        }
        return all;
    }

    /** Test main: java cs7is3.parse.LaTimesParser <latimes folder> */
    public static void main(String[] args) throws IOException {
        List<ParsedDoc> docs = new LaTimesParser().parseAll(Path.of(args[0]));
        System.out.println("LATIMES documents: " + docs.size());
        ParsedDoc d = docs.get(0);
        System.out.println(d.docno + " | " + d.date + " | " + d.title + "\n" + d.text.substring(0, Math.min(300, d.text.length())));
    }
}
