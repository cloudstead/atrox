parser grammar WikiArticleParser;

options { tokenVocab=WikiArticleLexer; }

freeform : MARKUP ;

article : (block | link | freeform)+;

linkTarget : LINK_PART ;
linkMetaString : LINK_PART ;
linkMeta : (linkMetaString | link)+ ;
link : START_LINK (linkTarget (LINK_SEP linkMeta)*)+ END_LINK ;

attrName : ATTR_NAME ;
attrText : ATTR_BODY ;
attrValue : (attrText | block | link)+ ;
attr : attrName (ATTR_EQ attrValue?)? ATTR_SEP* ;

blockName : BLOCK_NAME ;
block : START_BLOCK blockName (ATTR_SEP+ attr*)? END_BLOCK ;
