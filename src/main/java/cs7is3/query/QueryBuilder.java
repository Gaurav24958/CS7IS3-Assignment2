package cs7is3.query;

import cs7is3.AnalyzerFactory;
import cs7is3.topics.Topic;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.regex.Pattern;

public class QueryBuilder {
    public enum Mode { T, TD, TDN }

    public static final String TEXT_FIELD = "text";
    public static final String TITLE_FIELD = "title";

    public static final float TITLE_WEIGHT = 1.0f;
    public static final float DESC_WEIGHT = 0.5f;
    public static final float NARR_WEIGHT = 0.25f;
    public static final float DEFAULT_TITLE_FIELD_BOOST = 1.5f;

    private static final List<String> FILLER = List.of(
            "what", "which", "who", "whom", "whose", "where", "when", "why", "how",
            "find", "identify", "information", "document", "documents", "relevant", "relevance",
            "discuss", "discusses", "discussing", "discussion", "describe", "describes", "describing",
            "mention", "mentions", "mentioned", "mentioning", "provide", "provides", "contain", "contains",
            "include", "includes", "including", "concerning", "regarding", "about", "any", "also", "other",
            "e.g", "i.e", "etc", "example", "examples", "specific", "particular", "issue", "issues",
            "do", "does", "did", "has", "have", "had", "been", "being", "were", "would", "should", "could",
            "can", "may", "might", "must", "only", "well", "more", "most", "some", "all", "each", "either");

    private static final Pattern NEGATIVE =
            Pattern.compile("\\b(not\\s+(\\w+\\s+){0,2}relevant|irrelevant|non-?relevant|not\\s+of\\s+interest)\\b",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern SENTENCE_END = Pattern.compile("(?<=[.;!?])\\s+");

    private final Analyzer analyzer;
    private final Mode mode;
    private final float titleFieldBoost;
    private final Set<String> fillerTerms;

    public QueryBuilder() {
        this(Mode.TD, DEFAULT_TITLE_FIELD_BOOST);
    }

    public QueryBuilder(Mode mode, float titleFieldBoost) {
        this.analyzer = AnalyzerFactory.create();
        this.mode = mode;
        this.titleFieldBoost = titleFieldBoost;

        this.fillerTerms = new HashSet<>(analyze(String.join(" ", FILLER)));
    }

    public Mode mode() { return mode; }

    public Query build(Topic topic) {
        Map<String, Float> weights = termWeights(topic);
        BooleanQuery.Builder b = new BooleanQuery.Builder();
        for (Map.Entry<String, Float> e : weights.entrySet()) {
            Query inText = new TermQuery(new Term(TEXT_FIELD, e.getKey()));
            Query inTitle = new TermQuery(new Term(TITLE_FIELD, e.getKey()));
            b.add(new BoostQuery(inText, e.getValue()), BooleanClause.Occur.SHOULD);
            b.add(new BoostQuery(inTitle, e.getValue() * titleFieldBoost), BooleanClause.Occur.SHOULD);
        }
        return b.build();
    }

    public Query buildFallback(Topic topic) {
        return new QueryBuilder(Mode.TDN, titleFieldBoost).build(topic);
    }

    public Map<String, Float> termWeights(Topic topic) {
        Map<String, Float> w = new LinkedHashMap<>();
        add(w, analyze(topic.title()), TITLE_WEIGHT, false);
        if (mode != Mode.T) add(w, analyze(topic.desc()), DESC_WEIGHT, true);
        if (mode == Mode.TDN) add(w, analyze(positiveNarrative(topic.narrative())), NARR_WEIGHT, true);
        return w;
    }

    static String positiveNarrative(String narrative) {
        StringBuilder sb = new StringBuilder();
        for (String sentence : SENTENCE_END.split(narrative))
            if (!NEGATIVE.matcher(sentence).find()) sb.append(sentence).append(' ');
        return sb.toString().strip();
    }

    private void add(Map<String, Float> w, List<String> terms, float weight, boolean dropFiller) {
        for (String t : terms) {
            if (dropFiller && fillerTerms.contains(t)) continue;
            w.merge(t, weight, Float::sum);
        }
    }

    private List<String> analyze(String text) {
        List<String> out = new ArrayList<>();
        try (TokenStream ts = analyzer.tokenStream(TEXT_FIELD, text)) {
            CharTermAttribute term = ts.addAttribute(CharTermAttribute.class);
            ts.reset();
            while (ts.incrementToken()) out.add(term.toString());
            ts.end();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out;
    }
}
