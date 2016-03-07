package histori.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.validation.HasValue;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Size;

import static histori.ApiConstants.NAME_MAXLEN;

@NoArgsConstructor @Accessors(chain=true)
@Entity @Table(uniqueConstraints=@UniqueConstraint(columnNames={"owner", "name"}))
public class Bookmark extends AccountOwnedEntity {

    @HasValue(message="err.name.empty")
    @Size(min=2, max=NAME_MAXLEN, message="err.name.length")
    @Column(length=NAME_MAXLEN, nullable=false)
    @Getter @Setter private String name;

    @HasValue(message="err.bookmark.json.empty")
    @Size(max=16000, message="err.bookmark.json.length")
    @Column(length=16000, nullable=false)
    @Getter @Setter private String json;

}
