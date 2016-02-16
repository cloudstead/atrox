package histori.wiki;

public enum WikiNodeType {

    article, string, link, infobox, attribute;

    public boolean isArticle () { return this == article; }
    public boolean isString () { return this == string; }
    public boolean isLink () { return this == link; }
    public boolean isInfobox () { return this == infobox; }
    public boolean isAttribute () { return this == attribute; }

}
