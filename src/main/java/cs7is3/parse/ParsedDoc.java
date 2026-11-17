package cs7is3.parse;

/** One parsed document from any of the four collections. All fields are plain Strings. */
public class ParsedDoc {
    public final String docno;   // trimmed, e.g. FT911-1, LA010189-0001
    public final String title;
    public final String text;
    public final String date;
    public final String source;  // FT, LATIMES, FBIS or FR94

    public ParsedDoc(String docno, String title, String text, String date, String source) {
        this.docno = docno;
        this.title = title;
        this.text = text;
        this.date = date;
        this.source = source;
    }

    public String getDocno()  { return docno; }
    public String getTitle()  { return title; }
    public String getText()   { return text; }
    public String getDate()   { return date; }
    public String getSource() { return source; }
}
