package histori.dao.archive;

import histori.model.archive.EntityArchive;
import histori.model.Account;
import org.cobbzilla.wizard.dao.DAO;

import java.util.List;

public interface ArchiveDAO<E extends EntityArchive> extends DAO<E> {

    List<E> findArchives(Account account, String id);

}
