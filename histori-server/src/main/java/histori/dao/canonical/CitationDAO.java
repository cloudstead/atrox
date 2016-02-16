package histori.dao.canonical;

import histori.model.canonical.Citation;
import org.cobbzilla.util.http.URIUtil;
import org.springframework.stereotype.Repository;

import javax.validation.Valid;

import static org.cobbzilla.wizard.resources.ResourceUtil.invalidEx;

@Repository public class CitationDAO extends CanonicalEntityDAO<Citation> {

    @Override public Object preCreate(@Valid Citation entity) {
        final String name = entity.getName();

        if (!URIUtil.toUri(name).isAbsolute()) throw invalidEx("err.name.invalid", "Citation name was not a valid URL: "+ name, name);

        return super.preCreate(entity);
    }

}
