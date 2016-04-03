package histori.dao.search;

import histori.model.Nexus;
import org.cobbzilla.wizard.client.ApiClientBase;
import org.springframework.stereotype.Repository;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.json.JsonUtil.toJson;

@Repository
public class ElasticSearchDAO {

    private final ApiClientBase client;

    public ElasticSearchDAO() {
        client = new ApiClientBase("http://127.0.0.1:9200");
    }

    public void index (Nexus nexus) {
        try {
            client.put("/histori/nexus/"+nexus.getVersion(), toJson(nexus));
        } catch (Exception e) {
            die("index: "+e, e);
        }
    }

}
