lexer grammar WikiArticleLexer;

// default mode: everything outside of a block
START_BLOCK : StartBlock -> pushMode(IN_BLOCK) ;
END_BLOCK : WS* '}}' WS* ;
START_LINK : StartLink -> pushMode(IN_LINK) ;
MARKUP : (~('{' | '[' | ']'))+ ;

mode IN_BLOCK;
BLOCK_NAME : BlockName ;
BLOCK_ATTRS : AttrSep -> pushMode(IN_ATTRS), type(ATTR_SEP) ;
BLOCK_LINK : StartLink -> pushMode(IN_LINK), type(START_LINK) ;
END_IN_BLOCK : WS* '}}' WS* -> popMode, type(END_BLOCK) ;

mode IN_ATTRS;
ATTR_SEP : AttrSep ;
//ATTR_EQ : {has_attr_values=true} WS* '=' WS* -> pushMode(IN_ATTR_VALUE);
ATTR_EQ : WS* '=' WS* -> pushMode(IN_ATTR_VALUE);
ATTR_NAME : NameChar (NameChar|':'|WS)* ;
ATTR_LINK : StartLink -> pushMode(IN_LINK), type(START_LINK);
END_ATTRS : WS* '}}' -> skip, popMode, popMode, type(END_BLOCK) ;

mode IN_ATTR_VALUE;
ATTR_VALUE_END : AttrSep -> skip, popMode ;
END_ALL_ATTRS : WS* '}}' -> popMode, popMode, popMode, type(END_BLOCK) ;
//END_ALL_ATTRS : {has_attr_values} WS* '}}' -> skip, popMode, type(END_BLOCK) ;
//END_ALL_ATTRS2 : {!has_attr_values} WS* '}}' -> skip, popMode, type(END_BLOCK) ;
ATTR_BODY : AttrValueChar+ ;
NEST_BLOCK :  StartBlock -> pushMode(IN_BLOCK), type(START_BLOCK);
ATTR_VALUE_LINK : StartLink -> pushMode(IN_LINK), type(START_LINK) ;

mode IN_LINK;
LINK_SEP : AttrSep ;
LINK_PART : '\\' | ~[|\[\]]+ ;
NEST_LINK : StartLink -> pushMode(IN_LINK), type(START_LINK);
END_LINK : WS* ']' ']'? -> popMode ;

Directive : [A-Z]+ ;

BlockName : (Directive ':')? Name ;

fragment AttrSep : WS* '|' WS* ;

fragment StartBlock : '{{' WS* ;

fragment StartLink : '[' '['? WS* ;

fragment Name : NameStartChar (NameChar|WS)* ;

fragment AttrValueChar : NameChar | '\\' | ~[|\[\]{}] | WS ;

fragment NameChar
   : NameStartChar
   | '0'..'9'
   | '_'
   | '\u00B7'
   | '\u0300'..'\u036F'
   | '\u203F'..'\u2040'
   ;
fragment NameStartChar
   : 'A'..'Z' | 'a'..'z'
   | '\u00C0'..'\u00D6'
   | '\u00D8'..'\u00F6'
   | '\u00F8'..'\u02FF'
   | '\u0370'..'\u037D'
   | '\u037F'..'\u1FFF'
   | '\u200C'..'\u200D'
   | '\u2070'..'\u218F'
   | '\u2C00'..'\u2FEF'
   | '\u3001'..'\uD7FF'
   | '\uF900'..'\uFDCF'
   | '\uFDF0'..'\uFFFD'
   ;

WS : [ \t\n\r]+ -> skip ;
