package atrox.model;

import atrox.ApiConstants;
import atrox.model.support.EntityVisibility;
import atrox.model.support.TimePoint;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.ResultPage;
import org.cobbzilla.wizard.model.SemanticVersion;
import org.cobbzilla.wizard.model.StrongIdentifiableBase;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.util.HashMap;
import java.util.Map;

@MappedSuperclass @Accessors(chain=true)
public abstract class AccountOwnedEntity extends StrongIdentifiableBase {

    public static final int HEADLINE_MAXLEN = 100;
    public static final int SUBHEAD_MAXLEN = 200;
    public static final int MARKDOWN_MAXLEN = 100000;

    @Transient public abstract String[] getUniqueProperties();
    @Transient public abstract String getSortField();
    @Transient public abstract ResultPage.SortOrder getSortOrder();

    @Column(nullable=false, length=UUID_MAXLEN)
    @Getter @Setter private String owner;

    @Embedded
    @Getter @Setter private SemanticVersion version;

    @Override public void beforeCreate() {
        super.beforeCreate();
    }

    public String incrementVersion () {
        version = SemanticVersion.incrementPatch(version);
        return version.toString();
    }

    @Column(nullable=false, length=20)
    @Enumerated(EnumType.STRING)
    @Getter @Setter private EntityVisibility visibility = EntityVisibility.owner;

    @Getter @Setter private int upVotes;
    @Getter @Setter private int downVotes;

    @Size(max=HEADLINE_MAXLEN, message="err.headline.tooLong")
    @Column(length=HEADLINE_MAXLEN)
    @Getter @Setter private String headline;

    @Size(max=SUBHEAD_MAXLEN, message="err.subhead.tooLong")
    @Column(length=SUBHEAD_MAXLEN)
    @Getter @Setter private String subhead;

    @Size(max=MARKDOWN_MAXLEN, message="err.markdown.tooLong")
    @Column(length=MARKDOWN_MAXLEN)
    @Getter @Setter private String markdown;

    public Map<String, String> getBounds(TimePoint start, TimePoint end) {
        final Map<String, String> bounds = new HashMap<>();
        bounds.put(ApiConstants.BOUND_RANGE, TimePoint.formatSearchRange(start, end));
        return bounds;
    }

}
