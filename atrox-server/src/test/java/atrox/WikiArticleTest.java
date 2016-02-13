package atrox;

import atrox.main.wiki.ParsedWikiArticle;
import atrox.main.wiki.WikiArticle;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class WikiArticleTest {

    private static final String TEST
        = "{{block1" +
            "|attr1" +
            "|attr2=aval2" +
            "|attr3=[[plain link]]" +
            "|attr4=b4 [[inner link|with meta]] after" +
            "|attr5=a long [[link|with [[this one inside]] of it]] wowzers.}} " +
            "[[link|outside everything]] and " +
            "[[another| link outside with [[another one]] inside of it]] OK " +
"{{block2|nested=look at {{this block|insideof = another | crazy=and it has a [[double-embedded|extra [[link|foobar~~!@]] inside!]] with two more {{nested block}}{{here is the second one}}}}"
            ;

    @Test public void testParseArticle () throws Exception {
        final ParsedWikiArticle article = new WikiArticle("some title", TEST).parse();

        // todo: add validations on nodes here
    }

}
