parser grammar WikiArticleParser;

options { tokenVocab=WikiArticleLexer; }

freeform : MARKUP ;

article : (infobox | link | freeform)+;

linkTarget : LINK_PART ;
linkMetaString : LINK_PART ;
linkMeta : (linkMetaString | link)+ ;
link : START_LINK (linkTarget (LINK_SEP linkMeta)*)+ END_LINK ;

attrName : ATTR_NAME ;
attrText : ATTR_BODY ;
attrValue : (attrText | infobox | link)+ ;
attr : attrName (ATTR_EQ attrValue?)? ATTR_SEP* ;

infoboxName : INFOBOX_NAME ;
infobox : START_INFOBOX infoboxName (ATTR_SEP+ attr*)? END_INFOBOX ;
