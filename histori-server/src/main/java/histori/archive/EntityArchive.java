package histori.archive;

import histori.model.VersionedEntity;

public interface EntityArchive extends VersionedEntity {

    public String getIdentifier();
    public EntityArchive setIdentifier(String uuid);
    public String getIdentifier(VersionedEntity entity);

}
