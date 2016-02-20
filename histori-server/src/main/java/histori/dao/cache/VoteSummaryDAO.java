package histori.dao.cache;

import histori.dao.VoteDAO;
import histori.model.Vote;
import histori.model.cache.VoteSummary;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.dao.AbstractRedisDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.*;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

@Repository @Slf4j
public class VoteSummaryDAO extends AbstractRedisDAO<VoteSummary> {

    private static final long MAX_SUMMARY_AGE = TimeUnit.DAYS.toMillis(1);

    @Autowired private VoteDAO voteDAO;

    @Override public VoteSummary get(Serializable id) {
        final VoteSummary voteSummary = super.get(id);
        if (voteSummary != null) return voteSummary;
        return queueJob(id.toString());
    }

    private Map<String, VoteSummaryJobResult> jobs = new ConcurrentHashMap<>();

    private ExecutorService executor = Executors.newFixedThreadPool(20);

    public VoteSummary queueJob(String uuid) {
        VoteSummaryJobResult result = jobs.get(uuid);
        try {
            if (result != null) {
                if (result.isRunning()) {
                    // todo -- if it has been running too long, kill it and maybe restart it
                    return null;
                }
                if (result.getSummaryAge() > MAX_SUMMARY_AGE) {
                    jobs.put(uuid, new VoteSummaryJobResult(executor.submit(new VoteSummaryJob(uuid))));
                }
                return result.getSummary();

            } else {
                jobs.put(uuid, new VoteSummaryJobResult(executor.submit(new VoteSummaryJob(uuid))));
                return null;
            }
        } catch (Exception e) {
            log.error("queueJob: "+e, e);
            return null;
        }
    }

    @AllArgsConstructor
    private class VoteSummaryJob implements Callable<VoteSummary> {

        private String uuid;

        @Override public VoteSummary call() throws Exception {
            final VoteSummary summary = new VoteSummary(uuid);
            for (Vote vote : voteDAO.findByEntity(uuid)) {
                summary.tally(vote);
            }
            return summary;
        }
    }

    private class VoteSummaryJobResult {

        private Future<VoteSummary> future;

        public VoteSummaryJobResult(Future<VoteSummary> future) { this.future = future; }

        private VoteSummary summary = null;
        private Long lastRun = null;

        public boolean isRunning() { return summary == null; }

        public long getSummaryAge () { return lastRun == null ? Long.MAX_VALUE : System.currentTimeMillis() - lastRun; }

        public VoteSummary getSummary () {
            if (summary != null) return summary;
            try {
                summary = future.get(50, TimeUnit.MILLISECONDS);
                lastRun = System.currentTimeMillis();
                update(summary);

            } catch (InterruptedException|ExecutionException e) {
                return die(e);

            } catch (TimeoutException e) {}
            return summary;
        }
    }
}
