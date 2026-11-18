package cs7is3.topics;

/**
 * One TREC topic. All text fields are single-line with whitespace collapsed and the
 * "Description:" / "Narrative:" prefixes removed.
 *
 * @param number    topic number, e.g. 404 (used as the first column of the run file)
 * @param title     short keyword query, e.g. "Ireland, peace talks"
 * @param desc      one or two sentence description of the information need
 * @param narrative what makes a document relevant or not relevant
 */
public record Topic(int number, String title, String desc, String narrative) {}
