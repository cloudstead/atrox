package histori.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;
import javax.validation.constraints.Size;

@Entity @Accessors(chain=true)
public class MapImage extends AccountOwnedEntity {

    @Column(length=1024, nullable=false, updatable=false)
    @Getter @Setter private String uri;

    @Size(max=1024, message="err.mapImage.fileName.tooLong")
    @Column(length=1024, nullable=false)
    @Getter @Setter private String fileName;

    @Transient @Getter @Setter private String url;
}
