package histori.archive;

import histori.model.VersionedEntity;

public interface EntityArchive extends VersionedEntity {

    public String getOriginalUuid ();
    public EntityArchive setOriginalUuid (String uuid);

    public boolean archiveUuid();

    // annoying working for lack of mixins. easier than doing cglib madness
//    public String[] getUniqueProperties();
//    public String getOwner();
//    public SocialEntity setEntityVersion(int version);

}
