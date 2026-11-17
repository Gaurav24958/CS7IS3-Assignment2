package cs7is3.parse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Small helpers shared by the FT and LA Times parsers. */
final class ParseUtil {
    private ParseUtil() {}

    private static final Pattern DOC = Pattern.compile("<DOC>(.*?)</DOC>", Pattern.DOTALL);
    private static final Pattern TAGS = Pattern.compile("<[^>]*>");

    /** Reads a whole file as ISO-8859-1 so no byte is ever rejected. */
    static String read(Path file) throws IOException {
        return new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1);
    }

    /** Returns the raw content of every <DOC>...</DOC> block. */
    static List<String> splitDocs(String content) {
        List<String> out = new ArrayList<>();
        Matcher m = DOC.matcher(content);
        while (m.find()) out.add(m.group(1));
        return out;
    }

    /** Content of the first <tag>...</tag> in the block, or "" if absent. */
    static String field(String block, String tag) {
        Matcher m = Pattern.compile("<" + tag + ">(.*?)</" + tag + ">", Pattern.DOTALL).matcher(block);
        return m.find() ? m.group(1) : "";
    }

    /** Removes any remaining tags, decodes the three SGML entities, collapses whitespace. */
    static String clean(String s) {
        s = TAGS.matcher(s).replaceAll(" ");
        s = s.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
        return s.replaceAll("\\s+", " ").trim();
    }

    /** True for the collection readme files, which must be skipped. */
    static boolean isReadme(Path file) {
        String n = file.getFileName().toString().toLowerCase();
        return n.startsWith("read") || n.startsWith(".");
    }
}
