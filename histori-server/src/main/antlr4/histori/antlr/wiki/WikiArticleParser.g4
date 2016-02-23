parser grammar WikiArticleParser;

options { tokenVocab=WikiArticleLexer; }

freeform : MARKUP ;

article : (plainlist | infobox | link | freeform)+;

linkTarget : LINK_PART ;
linkMetaString : LINK_PART ;
linkMeta : (linkMetaString | link)+ ;
link : START_LINK (linkTarget (LINK_SEP linkMeta)*)+ END_LINK ;

attrName : (ATTR_NAME | infobox)+ ;
attrText : ATTR_BODY ;
attrValue : (plainlist | attrText | infobox | link)+ ;
attr : attrName (ATTR_EQ attrValue?)? ATTR_SEP* ;

plainlistEntry : (freeform | infobox | link)+ ;
plainlist : START_PLAINLIST (NEWLINE plainlistEntry)+ END_PLAINLIST ;

infoboxName : INFOBOX_NAME ;
infobox : START_INFOBOX infoboxName (ATTR_SEP+ attr*)? WS* END_INFOBOX ;
