package atrox.main.wiki;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum WikiParseTokenType {

    START_BLOCK ("{{"),
    END_BLOCK ("}}"),
    ATTR_SEP ("|"),
    ATTR_EQUALS ("=");


    @Getter private String symbol;

}
