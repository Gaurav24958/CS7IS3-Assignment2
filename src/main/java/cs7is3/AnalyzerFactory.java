package cs7is3;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;

/** Single place that decides the analyzer, so indexing and query building always match. */
public final class AnalyzerFactory {
    private AnalyzerFactory() {}

    public static Analyzer create() {
        return new EnglishAnalyzer(); // English stop words + possessive removal + Porter stemming
    }
}
