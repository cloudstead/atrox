package histori.dao;

import histori.model.Permalink;
import org.cobbzilla.util.string.Base64;
import org.cobbzilla.wizard.dao.UniquelyNamedEntityDAO;
import org.springframework.stereotype.Repository;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.security.ShaUtil.sha256_hex;

@Repository
public class PermalinkDAO extends UniquelyNamedEntityDAO<Permalink> {

    public static final int MIN_PERMALINK_CHARS = 5;

    public Permalink getOrCreate(String json) {
        final String fullHash = Base64.encodeBytes(sha256_hex(json).getBytes());
        for (int i=MIN_PERMALINK_CHARS; i<fullHash.length(); i++) {
            final String name = fullHash.substring(0, i);
            final Permalink found = findByName(name);
            if (found == null) {
                final Permalink newLink = (Permalink) new Permalink().setJson(json).setName(name);
                return create(newLink);

            } else if (found.getJson().equals(json)) {
                return found;
            }

        }
        return die("getOrCreate: error creating ("+fullHash+") based on json: "+json);
    }

}
