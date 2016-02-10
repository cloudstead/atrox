package atrox.archive;

import org.cobbzilla.wizard.model.Identifiable;

public interface EntityArchive extends Identifiable {

    public String getOriginalUuid ();
    public void setOriginalUuid (String uuid);

    // annoying working for lack of mixins. easier than doing cglib madness
//    public String[] getUniqueProperties();
//    public String getOwner();
//    public SocialEntity setEntityVersion(int version);

}
