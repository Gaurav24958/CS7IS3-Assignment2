package cs7is3.topics;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the TREC topics file. Each topic sits between <top> and </top>; the <num>, <title>, <desc> and <narr>
 * sections have no closing tags, so each section runs until the next section tag (or the end of the topic).
 */
public class TopicParser {
    private static final Pattern TOP = Pattern.compile("<top>(.*?)</top>", Pattern.DOTALL);
    private static final Pattern NUM = Pattern.compile("<num>\\s*(?:Number:)?\\s*(\\d+)");
    private static final String NEXT = "(?=<num>|<title>|<desc>|<narr>|$)";
    private static final Pattern TITLE = Pattern.compile("<title>(.*?)" + NEXT, Pattern.DOTALL);
    private static final Pattern DESC = Pattern.compile("<desc>\\s*(?:Description:)?(.*?)" + NEXT, Pattern.DOTALL);
    private static final Pattern NARR = Pattern.compile("<narr>\\s*(?:Narrative:)?(.*?)" + NEXT, Pattern.DOTALL);

    public List<Topic> parse(Path topicsFile) throws IOException {
        String content = Files.readString(topicsFile, StandardCharsets.ISO_8859_1);
        List<Topic> topics = new ArrayList<>();
        Matcher top = TOP.matcher(content);
        while (top.find()) {
            String block = top.group(1);
            Matcher num = NUM.matcher(block);
            if (!num.find()) throw new IOException("Topic without a number: " + block.strip().lines().findFirst().orElse(""));
            topics.add(new Topic(Integer.parseInt(num.group(1)),
                    section(TITLE, block), section(DESC, block), section(NARR, block)));
        }
        return topics;
    }

    /** First match of the section pattern with whitespace collapsed, or "" if the section is missing. */
    private static String section(Pattern p, String block) {
        Matcher m = p.matcher(block);
        return m.find() ? m.group(1).replaceAll("\\s+", " ").strip() : "";
    }

    /** Test main: java cs7is3.topics.TopicParser <topics file>. Prints the count and every topic. */
    public static void main(String[] args) throws IOException {
        List<Topic> topics = new TopicParser().parse(Path.of(args[0]));
        System.out.println("Topics: " + topics.size());
        for (Topic t : topics) {
            System.out.println(t.number() + " | " + t.title());
            System.out.println("    D: " + t.desc());
            System.out.println("    N: " + t.narrative());
        }
    }
}
