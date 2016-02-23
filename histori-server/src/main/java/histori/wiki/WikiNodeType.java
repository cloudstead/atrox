package histori.wiki;

public enum WikiNodeType {

    article, string, link, infobox, attribute, plainlist, plainlist_entry, plainlist_header, wikitable;

    public boolean isArticle () { return this == article; }
    public boolean isString () { return this == string; }
    public boolean isLink () { return this == link; }
    public boolean isInfobox () { return this == infobox; }
    public boolean isAttribute () { return this == attribute; }
    public boolean isPlainlist () { return this == plainlist; }
    public boolean isPlainlistEntry () { return this == plainlist_entry; }
    public boolean isPlainlistHeader () { return this == plainlist_header; }
    public boolean isWikitable () { return this == wikitable; }

}
