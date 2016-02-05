package atrox.model.tags;

import atrox.model.AccountOwnedEntity;
import org.cobbzilla.wizard.model.ResultPage;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

@MappedSuperclass
public abstract class EntityTag extends AccountOwnedEntity {

    @Transient @Override public String getSortField() { return "ctime"; }
    @Transient @Override public ResultPage.SortOrder getSortOrder() { return ResultPage.SortOrder.DESC; }

}
