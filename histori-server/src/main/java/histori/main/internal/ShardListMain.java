package histori.main.internal;

import histori.main.HistoriApiMain;
import org.cobbzilla.wizard.client.ApiClientBase;

public class ShardListMain extends HistoriApiMain<ShardListOptions> {

    public static void main (String[] args) { main(ShardListMain.class, args); }

    @Override protected void run() throws Exception {
        final ApiClientBase api = getApiClient();
        final ShardListOptions opts = getOptions();
        if (opts.hasShardSet()) {
            out(api.doGet("/admin/shards/"+opts.getShardSet()));
        } else {
            out(api.doGet("/admin/shards"));
        }
    }
}
