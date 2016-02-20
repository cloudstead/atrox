package histori.model.base;

import histori.model.AccountOwnedEntity;
import histori.model.VersionedEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@MappedSuperclass @Accessors(chain=true) @ToString(of={"owner","entity","vote"})
public abstract class VoteBase extends AccountOwnedEntity implements VersionedEntity {

    private static final String[] ID_FIELDS = {"owner", "entity"};
    public String[] getIdentifiers() { return new String [] { getOwner(), getEntity() }; }
    public String[] getIdentifierFields() { return ID_FIELDS; }

    @Column(length=UUID_MAXLEN, nullable=false, updatable=false)
    @Getter @Setter private String entity;

    @Max(message="err.vote.tooHigh", value=1)
    @Min(message="err.vote.tooLow", value=-1)
    @Column(nullable=false)
    @Getter @Setter private short vote;

    @Getter @Setter private int version;
}
