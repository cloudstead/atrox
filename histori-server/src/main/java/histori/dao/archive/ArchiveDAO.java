package histori.dao.archive;

import histori.archive.EntityArchive;
import histori.model.Account;
import org.cobbzilla.wizard.dao.AbstractCRUDDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate4.HibernateTemplate;

import java.util.List;

import static org.hibernate.criterion.Restrictions.and;
import static org.hibernate.criterion.Restrictions.eq;

public class ArchiveDAO<E extends EntityArchive> extends AbstractCRUDDAO<E> {

    @Autowired protected HibernateTemplate archiveHibernateTemplate;
    @Override public HibernateTemplate getHibernateTemplate() { return archiveHibernateTemplate; }

    public List<E> findArchives(Account account, String id) {
        return list(criteria().add(and(
                eq("identifier", id),
                eq("owner", account.getUuid()))), 0, 100);
    }

}
