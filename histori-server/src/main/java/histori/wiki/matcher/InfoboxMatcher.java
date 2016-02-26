package histori.wiki.matcher;

import histori.wiki.WikiNode;

public abstract class InfoboxMatcher implements NodeMatcher {

    public boolean matches(WikiNode node) { return node.getType().isInfobox() && infoboxMatches(node); }

    protected abstract boolean infoboxMatches(WikiNode node);

}
