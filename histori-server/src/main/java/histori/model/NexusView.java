package histori.model;

import histori.model.base.NexusTags;
import histori.model.cache.VoteSummary;

public interface NexusView {

    String getUuid();

    String getOwner(); // may be null, like for SuperNexus
    boolean hasOwner();

    String getName();
    String getCanonicalName();

    boolean hasNexusType();
    String getNexusType();

    boolean hasMarkdown();
    String getMarkdown();

    boolean hasTags();
    NexusTags getTags();

    VoteSummary getVotes();
    boolean hasVotes();

    long getCtime();
}
