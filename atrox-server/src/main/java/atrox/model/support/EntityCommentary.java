package atrox.model.support;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.Size;

@Embeddable
public class EntityCommentary {

    public static final int HEADLINE_MAXLEN = 100;
    public static final int SUBHEAD_MAXLEN = 200;
    public static final int MARKDOWN_MAXLEN = 100000;

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
