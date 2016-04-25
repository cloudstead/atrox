package histori.dao.search;

import org.cobbzilla.util.string.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public class NexusQueryTerms extends TreeSet<NexusQueryTerm> {

    public NexusQueryTerms(String query) { if (!empty(query)) addAll(collectTerms(query)); }

    protected List<NexusQueryTerm> collectTerms(String query) {
        final List<String> rawTerms = StringUtil.splitIntoTerms(query);
        final List<NexusQueryTerm> terms = new ArrayList<>();
        for (int i=0; i<rawTerms.size(); i++) {
            final String rawTerm = rawTerms.get(i);
            final NexusQueryTerm term;
            if (NexusQueryTerm.isBareQualifier(rawTerm) && rawTerms.size() > i+1) {
                term = NexusQueryTerm.create(rawTerm+rawTerms.get(i+1));
                i++;
            } else {
                term = NexusQueryTerm.create(rawTerm);
            }
            if (term != null) terms.add(term);
        }
        return terms;
    }

}
