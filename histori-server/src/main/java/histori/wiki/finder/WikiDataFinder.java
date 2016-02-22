package histori.wiki.finder;

public interface WikiDataFinder<T> {

    public static final String INFOBOX_REFIMPROVE = "Refimprove";
    public static final String INFOBOX_COPYPASTE = "Copypaste";
    public static final String INFOBOX_NO_FOOTNOTES = "no footnotes";

    public T find ();

}
