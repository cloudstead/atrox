package histori.main.internal;

import histori.main.HistoriApiMain;
import org.cobbzilla.wizard.client.ApiClientBase;

import static org.cobbzilla.util.json.JsonUtil.toJson;

public class ShardUpdateMain extends HistoriApiMain<ShardUpdateOptions> {

    public static void main (String[] args) { main(ShardUpdateMain.class, args); }

    @Override protected void run() throws Exception {
        final ApiClientBase api = getApiClient();
        final ShardUpdateOptions opts = getOptions();
        if (opts.hasUuid()) {
            out(api.doPost("/admin/shards/" + opts.getShardSet() + "/shard/" + opts.getUuid(), toJson(opts.getShardMap())));
        } else {
            out(api.doPut("/admin/shards/" + opts.getShardSet(), toJson(opts.getShardMap())));
        }
    }

}
