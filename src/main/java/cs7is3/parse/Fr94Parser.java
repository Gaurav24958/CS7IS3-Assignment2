package cs7is3.parse;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Fr94Parser {
    public static final String SOURCE = "FR94";

    private static final Pattern COMMENT = Pattern.compile("<!--.*?-->", Pattern.DOTALL);

    private static final Pattern BOILERPLATE =
            Pattern.compile("<(BILLING|FRFILING|RINDOCK|CFRNO|SIGNER|SIGNJOB)>.*?</\\1>", Pattern.DOTALL);
    private static final Map<String, String> ENTITIES = Map.of(
            "&hyph;", "-", "&blank;", " ", "&sect;", " ", "&para;", " ",
            "&rsquo;", "'", "&lsquo;", "'", "&rdquo;", "\"", "&ldquo;", "\"",
            "&mdash;", " - ", "&ndash;", "-");

    private static final Pattern OTHER_ENTITY = Pattern.compile("&(?!amp;|lt;|gt;)[A-Za-z0-9]+;");

    public List<ParsedDoc> parseFile(Path file) {
        List<ParsedDoc> docs = new ArrayList<>();
        if (ParseUtil.isReadme(file)) return docs;
        try {
            for (String block : ParseUtil.splitDocs(ParseUtil.read(file))) {
                String docno = ParseUtil.clean(ParseUtil.field(block, "DOCNO"));
                if (docno.isEmpty()) continue;
                String text = ParseUtil.field(block, "TEXT");
                text = COMMENT.matcher(text).replaceAll(" ");
                text = BOILERPLATE.matcher(text).replaceAll(" ");
                docs.add(new ParsedDoc(docno,
                        clean(COMMENT.matcher(ParseUtil.field(block, "DOCTITLE")).replaceAll(" ")),
                        clean(text),
                        dateFromDocno(docno),
                        SOURCE));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return docs;
    }

    private static String clean(String s) {
        for (Map.Entry<String, String> e : ENTITIES.entrySet()) s = s.replace(e.getKey(), e.getValue());
        return ParseUtil.clean(OTHER_ENTITY.matcher(s).replaceAll(" "));
    }

    static String dateFromDocno(String docno) {
        return docno.length() >= 8 && docno.startsWith("FR") ? docno.substring(2, 8) : "";
    }

    public static void main(String[] args) throws IOException {
        Fr94Parser p = new Fr94Parser();
        int count = 0, emptyText = 0, withTitle = 0, leftovers = 0;
        ParsedDoc sample = null;
        try (Stream<Path> files = Files.walk(Path.of(args[0]))) {
            for (Path f : (Iterable<Path>) files.filter(Files::isRegularFile).sorted()::iterator) {
                for (ParsedDoc d : p.parseFile(f)) {
                    count++;
                    if (d.text.isEmpty()) emptyText++;
                    if (!d.title.isEmpty()) {
                        withTitle++;
                        if (sample == null) sample = d;
                    }
                    if (d.text.contains("PJG") || d.text.contains("&hyph;") || d.text.contains("&blank;")) leftovers++;
                }
            }
        }
        System.out.printf("FR94 documents: %,d (empty text: %d, with a title: %d, with PJG/entity leftovers: %d)%n",
                count, emptyText, withTitle, leftovers);
        if (sample != null)
            System.out.println(sample.docno + " | " + sample.date + " | " + sample.title + "\n"
                    + sample.text.substring(0, Math.min(400, sample.text.length())));
    }
}
