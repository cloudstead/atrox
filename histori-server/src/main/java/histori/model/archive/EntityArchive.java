package histori.model.archive;

import histori.model.VersionedEntity;

public interface EntityArchive extends VersionedEntity {

    String getIdentifier();
    EntityArchive setIdentifier(String uuid);
    String getIdentifier(VersionedEntity entity);

}
