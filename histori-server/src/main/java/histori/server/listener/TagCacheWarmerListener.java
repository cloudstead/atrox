package histori.server.listener;

import histori.dao.TagDAO;
import histori.server.HistoriConfiguration;
import org.cobbzilla.wizard.server.RestServer;
import org.cobbzilla.wizard.server.RestServerLifecycleListenerBase;

public class TagCacheWarmerListener extends RestServerLifecycleListenerBase<HistoriConfiguration> {

    @Override public void onStart(RestServer server) {
        server.getConfiguration().getBean(TagDAO.class).warmCache();
        super.onStart(server);
    }

}
