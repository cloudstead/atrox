package histori.model.support;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.Size;

@Embeddable @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
public class EntityCommentary {

    public static final int HEADLINE_MAXLEN = 100;
    public static final int SUBHEAD_MAXLEN = 200;
    public static final int MARKDOWN_MAXLEN = 100000;

    public EntityCommentary (String headline) { setHeadline(headline); }
    public EntityCommentary (String headline, String subhead) { setHeadline(headline); setSubhead(subhead); }

    @Size(max=HEADLINE_MAXLEN, message="err.headline.tooLong")
    @Column(length=HEADLINE_MAXLEN)
    @Getter @Setter private String headline;

    @Size(max=SUBHEAD_MAXLEN, message="err.subhead.tooLong")
    @Column(length=SUBHEAD_MAXLEN)
    @Getter @Setter private String subhead;

    @Size(max=MARKDOWN_MAXLEN, message="err.markdown.tooLong")
    @Column(length=MARKDOWN_MAXLEN)
    @Getter @Setter private String markdown;

}
