package histori.main.wiki;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.main.BaseMainOptions;
import org.kohsuke.args4j.Argument;

public class ArticlePathOptions extends BaseMainOptions {

    public static final String USAGE_TITLE = "The title to determine a path for";
    @Argument(usage=USAGE_TITLE)
    @Getter @Setter private String title;

}
