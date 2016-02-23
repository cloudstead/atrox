lexer grammar WikiArticleLexer;

// default mode: everything outside of an infobox
START_PLAINLIST: StartPlainlist -> pushMode(IN_PLAINLIST) ;
START_INFOBOX : StartInfoBox -> pushMode(IN_INFOBOX) ;
END_INFOBOX : EndInfoBox ;
START_LINK : StartLink -> pushMode(IN_LINK) ;
MARKUP : (~('{' | '[' | ']'))+ ;
SL_MARKUP : (~('{' | '[' | ']' | '\n'))+ ;

mode IN_PLAINLIST;
NEWLINE : '\n' ;
PL_INFOBOX : StartInfoBox -> pushMode(IN_INFOBOX), type(START_INFOBOX) ;
PL_LINK : StartLink -> pushMode(IN_LINK), type(START_LINK) ;
PL_MARKUP : (~('\n'))+ -> type(MARKUP) ;
END_PLAINLIST : EndPlainlist -> popMode ;

mode IN_INFOBOX;
INFOBOX_NAME : InfoBoxName ;
INFOBOX_ATTRS : AttrSep -> pushMode(IN_ATTRS), type(ATTR_SEP) ;
INFOBOX_LINK : StartLink -> pushMode(IN_LINK), type(START_LINK) ;
END_IN_INFOBOX : EndInfoBox -> popMode, type(END_INFOBOX) ;

mode IN_ATTRS;
ATTR_PLAINLIST: StartPlainlist -> pushMode(IN_PLAINLIST) ;
ATTR_SEP : AttrSep ;
//ATTR_EQ : {has_attr_values=true} WS* '=' WS* -> pushMode(IN_ATTR_VALUE);
ATTR_EQ : WS* '=' WS* -> pushMode(IN_ATTR_VALUE);
ATTR_NAME : (NameChar|'°') (NameChar|','|':'|'-'|'–'|'°'|WS)* ;
ATTR_LINK : StartLink -> pushMode(IN_LINK), type(START_LINK);
ATTR_INFOBOX :  StartInfoBox -> pushMode(IN_INFOBOX), type(START_INFOBOX);
END_ATTRS : EndInfoBox -> skip, popMode, popMode, type(END_INFOBOX) ;

mode IN_ATTR_VALUE;
ATTR_VALUE_PLAINLIST: StartPlainlist -> pushMode(IN_PLAINLIST), type(START_PLAINLIST) ;
ATTR_VALUE_END : AttrSep -> skip, popMode ;
END_ALL_ATTRS : EndInfoBox -> popMode, popMode, popMode, type(END_INFOBOX) ;
ATTR_BODY : AttrValueChar+ ;
NEST_INFOBOX :  StartInfoBox -> pushMode(IN_INFOBOX), type(START_INFOBOX);
ATTR_VALUE_LINK : StartLink -> pushMode(IN_LINK), type(START_LINK) ;

mode IN_LINK;
LINK_SEP : AttrSep ;
LINK_PART : '\\' | ~[|\[\]]+ ;
NEST_LINK : StartLink -> pushMode(IN_LINK), type(START_LINK);
END_LINK : WS* ']' ']'? -> popMode ;

Directive : [A-Z]+ ;

InfoBoxName : (Directive ':')? Name ;

fragment AttrSep : WS* ('|' | '\'' | ';') WS* ;

fragment StartInfoBox : '{{' WS* ;

fragment EndInfoBox : WS* '}}' WS* ;

fragment StartLink : '[' '['? WS* ;

fragment Name : NameStartChar (NameChar|WS)* ;

fragment AttrValueChar : NameChar | '\\' | ~[|\[\]{}] | WS ;

fragment Plainlist : ('P'|'p')('L'|'l')('A'|'a')('I'|'i')('N'|'n')('L'|'l')('I'|'i')('S'|'s')('T'|'t') ;
fragment EndPl : ('E'|'e')('N'|'n')('D'|'d') Plainlist ;

fragment StartPlainlist : (StartInfoBox Plainlist WS* '|') | (StartInfoBox Plainlist '}}');
fragment EndPlainlist : (StartInfoBox EndPl)? EndInfoBox;

fragment NameChar
   : NameStartChar
   | '0'..'9'
   | '_'
   | '.'
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
