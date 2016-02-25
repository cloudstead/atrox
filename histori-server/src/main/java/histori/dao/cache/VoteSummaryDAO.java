package histori.dao.cache;

import histori.dao.VoteDAO;
import histori.model.Vote;
import histori.model.cache.VoteSummary;
import histori.server.HistoriConfiguration;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.dao.BackgroundFetcherDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@Repository @Slf4j
public class VoteSummaryDAO extends BackgroundFetcherDAO<VoteSummary> {

    @Autowired private VoteDAO voteDAO;
    @Autowired private HistoriConfiguration configuration;

    @Override protected Callable<VoteSummary> newEntityJob(String uuid, Map<String, Object> context) { return new VoteSummaryJob(uuid); }

    @Override public int getThreadPoolSize() { return configuration.getThreadPoolSizes().get(getClass().getSimpleName()); }

    @AllArgsConstructor
    private class VoteSummaryJob implements Callable<VoteSummary> {

        private String uuid;

        @Override public VoteSummary call() throws Exception {
            final VoteSummary summary = new VoteSummary(uuid);
            final List<Vote> votes = voteDAO.findByEntity(uuid);
            for (Vote vote : votes) {
                summary.tally(vote);
            }
            return summary;
        }
    }

}
