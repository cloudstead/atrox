package histori.wiki.linematcher;

public interface LineMatcher {

    public void configure (String args);

    public boolean matches(String line);

}
