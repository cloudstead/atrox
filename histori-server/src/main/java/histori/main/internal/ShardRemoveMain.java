package histori.main.internal;

import histori.main.HistoriApiMain;
import org.cobbzilla.wizard.client.ApiClientBase;

public class ShardRemoveMain extends HistoriApiMain<ShardRemoveOptions> {

    public static void main (String[] args) { main(ShardRemoveMain.class, args); }

    @Override protected void run() throws Exception {
        final ApiClientBase api = getApiClient();
        final ShardRemoveOptions opts = getOptions();
        out(api.doDelete("/admin/shards/" + opts.getShardSet() + "/shard/" + opts.getUuid()));
    }
}
