package histori.dao.search;

import histori.model.Account;
import histori.model.SuperNexus;
import histori.model.support.EntityVisibility;
import histori.model.support.GeoBounds;
import histori.model.support.GlobalSortOrder;
import histori.model.support.TimeRange;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.dao.shard.AbstractSingleShardDAO;

import java.io.Closeable;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.util.Iterator;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.notSupported;

/**
 * Iterate over all SuperNexus names within a GeoBounds and a TimeRange
 */
@Slf4j
public class SuperNexusIterator implements Iterator<String>, Closeable {

    public static final String SQL
            = "SELECT name "
            + "FROM super_nexus "
            + "WHERE "
            + "(   (north BETWEEN ? AND ?) AND ( (east BETWEEN ? AND ?) OR (west BETWEEN ? AND ?) ) "
            +  "OR (south BETWEEN ? AND ?) AND ( (east BETWEEN ? AND ?) OR (west BETWEEN ? AND ?) ) "
            + ") AND ( "
            + "(start_instant BETWEEN ? AND ?) OR (end_instant BETWEEN ? AND ?) "
            + ") ";
    public static final String PUBLIC_CLAUSE = " AND account IS NULL AND visibility = 'everyone'";
    public static final String PRIVATE_CLAUSE = " AND account = ? AND visibility = ?";

    private Connection conn = null;
    private PreparedStatement ps = null;
    private ResultSet rs = null;

    public SuperNexusIterator(AbstractSingleShardDAO<SuperNexus> dao,
                              TimeRange range,
                              GeoBounds bounds,
                              Account account,
                              EntityVisibility visibility,
                              GlobalSortOrder sort) {
        boolean publicOnly = visibility == EntityVisibility.everyone;
        final String sortClause;
        switch (sort) {
            case newest:              sortClause = "ctime DESC";      break;
            case oldest:              sortClause = "ctime ASC";       break;
            case up_vote:             sortClause = "up_votes DESC";   break;
            case down_vote:           sortClause = "down_votes DESC"; break;
            case vote_count:          sortClause = "vote_count DESC"; break;
            case vote_tally: default: sortClause = "vote_tally DESC"; break;
        }
        final String sql = (publicOnly ? SQL + PUBLIC_CLAUSE : SQL + PRIVATE_CLAUSE) + " ORDER BY " + sortClause;
        try {
            this.conn = dao.getDatabase().getConnection();
            this.ps = conn.prepareStatement(sql);

            int i = 1;
            ps.setDouble(i++, bounds.getNorth());
            ps.setDouble(i++, bounds.getSouth());
            ps.setDouble(i++, bounds.getEast());
            ps.setDouble(i++, bounds.getWest());
            ps.setDouble(i++, bounds.getEast());
            ps.setDouble(i++, bounds.getWest());
            ps.setObject(i++, new BigDecimal(range.start()), Types.NUMERIC);
            ps.setObject(i++, new BigDecimal(range.end()), Types.NUMERIC);
            ps.setObject(i++, new BigDecimal(range.start()), Types.NUMERIC);
            ps.setObject(i++, new BigDecimal(range.end()), Types.NUMERIC);
            if (!publicOnly) {
                ps.setString(i++, account.getUuid());
                ps.setString(i++, visibility.name());
            }

            this.rs = ps.executeQuery();

        } catch (Exception e) {
            String msg = "SuperNexusIterator: " + e;
            log.error(msg, e);
            try { close(); } catch (Exception ignored) {}
            die(msg, e);
        }
    }

    @Override public boolean hasNext() {
        try { return rs.next(); } catch (SQLException e) { return die("hasNext: "+e, e); }
    }

    @Override public String next() {
        try { return rs.getString(1); } catch (Exception e) { return die("next: "+e, e); }
    }

    @Override public void remove() { notSupported(); }

    @Override public void close() throws IOException {
        if (rs != null) try { rs.close(); } catch (Exception e) { log.warn("error closing result set: "+e); }
        rs = null;
        if (ps != null) try { ps.close(); } catch (Exception e) { log.warn("error closing statement: "+e); }
        ps = null;
        if (conn != null) try { conn.close(); } catch (Exception e) { log.warn("error closing connection: "+e); }
        conn = null;
    }

}
