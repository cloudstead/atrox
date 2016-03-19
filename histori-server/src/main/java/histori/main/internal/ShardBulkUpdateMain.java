package histori.main.internal;

import histori.main.HistoriApiMain;
import org.cobbzilla.wizard.client.ApiClientBase;

import static org.cobbzilla.util.json.JsonUtil.toJson;

public class ShardBulkUpdateMain extends HistoriApiMain<ShardBulkUpdateOptions> {

    public static void main(String[] args) { main(ShardBulkUpdateMain.class, args); }

    @Override protected void run() throws Exception {
        final ShardBulkUpdateOptions opts = getOptions();
        final ApiClientBase api = getApiClient();
        out(api.doPost("/admin/shards/" + opts.getShardSetName(), toJson(opts.getShardSet())));
    }

}
