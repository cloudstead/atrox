package histori.main.internal;

import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.main.BaseMain;
import org.cobbzilla.wizard.model.shard.ShardMap;
import org.cobbzilla.wizard.model.shard.ShardRange;

import java.util.ArrayList;
import java.util.List;

import static org.cobbzilla.util.daemon.ZillaRuntime.now;
import static org.cobbzilla.util.daemon.ZillaRuntime.uuid;

public class ShardSqlGenMain extends BaseMain<ShardSqlGenOptions> {

    public static void main (String[] args) { main(ShardSqlGenMain.class, args); }

    @Override protected void run() throws Exception {

        final ShardSqlGenOptions opts = getOptions();
        final List<String> jdbcUrls = opts.getJdbcUrlsList();

        final ShardRange[] ranges = calculateShardRanges(opts.getDbCount(), opts.getNumLogicalShards());
        final List<ShardMap> maps = new ArrayList<>();

        for (String shardSet : opts.getShardSetList()) {
            for (int i=0; i<ranges.length; i++) {
                maps.add(new ShardMap()
                        .setShardSet(shardSet)
                        .setRange(ranges[i])
                        .setUrl(jdbcUrls.get(i))
                        .setAllowRead(true)
                        .setAllowWrite(true));
            }
            if (opts.hasDbMaster()) {
                maps.add(new ShardMap()
                        .setShardSet(shardSet)
                        .setRange(new ShardRange(0, opts.getNumLogicalShards()))
                        .setUrl(opts.getDbMaster())
                        .setAllowRead(false)
                        .setAllowWrite(true));
            }
        }

        final StringBuilder b = new StringBuilder();
        for (ShardMap map : maps) {
            b.append(toSql(map)).append("\n");
        }
        if (opts.hasOutfile()) {
            FileUtil.toFile(opts.getOutfile(), b.toString());
        } else {
            out(b.toString());
        }
    }

    private String toSql(ShardMap map) {
        final long ctime = now();
        return "INSERT INTO "+getOptions().getShardTable()+" (uuid, ctime, mtime, shard_set, url, logical_start, logical_end, allow_read, allow_write"
                + ") VALUES ("
                + q(uuid())+", "+ctime+", "+ctime+", "+q(map.getShardSet())+", "+q(map.getUrl())+", "+map.getRange().getLogicalStart()+", "+map.getRange().getLogicalEnd()+", "+b(map.isAllowRead())+", "+b(map.isAllowWrite())
                + ");";
    }

    // format a boolean value for a SQL statement
    private String b(boolean val) { return Boolean.valueOf(val).toString().toUpperCase(); }

    // quote a string for a SQL statement
    private String q(String val) { return "'" + val.replace("'", "''") + "'"; }

    private ShardRange[] calculateShardRanges(int dbCount, int numLogicalShards) {
        final ShardRange[] ranges = new ShardRange[dbCount];
        int start = 0;
        double increment = ((double)numLogicalShards) / ((double)dbCount);
        for (int i=0; i<dbCount; i++) {
            int end = (int)(((double) (i+1)) * increment);
            ranges[i] = new ShardRange(start, end);
            start = end;
        }
        return ranges;
    }
}
