package histori.wiki.finder;

public interface WikiDataFinder<T> {

    public static final String[] IGNORED_INFOBOX_NAMES = {
            "Refimprove", "Copypaste", "no footnotes", "morerefs", "Use American English", "Use British English", "Use dmy dates"
    };

    public T find ();

}
