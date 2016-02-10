package atrox.dao.tag;

import atrox.dao.SocialEntityDAO;
import atrox.model.Account;
import atrox.model.canonical.CanonicalEntity;
import atrox.model.history.EntityHistory;
import atrox.model.support.EntitySearchOrder;
import atrox.model.support.EntitySearchType;
import atrox.model.support.EntityVisibility;
import atrox.model.tag.GenericEntityTag;
import org.cobbzilla.wizard.dao.HibernateCallbackImpl;

import java.util.ArrayList;
import java.util.List;

public abstract class GenericTagDAO<T extends GenericEntityTag> extends SocialEntityDAO<T> {


    public static final String[] PARAMS_UUID = new String[]{"uuid", "nameFragment"};
    public static final String[] PARAMS_UUID_AND_OWNER = new String[]{"uuid", "owner"};

    public List<GenericEntityTag> findTags(Account account, CanonicalEntity canonical, EntitySearchType searchType, EntitySearchOrder searchOrder) {

        if (searchType == EntitySearchType.none) return new ArrayList<>();

        final String[] params;
        final Object[] values;
        T tagProto = getEntityProto();
        final String simpleName = tagProto.simpleName();
        final String orderClause = orderClause(searchOrder);
        // todo: fix this
        if (true) return new ArrayList<>();
        String historyQuery = "from "+simpleName+"History h ";
        if (account == null) {
            if (searchType == EntitySearchType.mine) return new ArrayList<>(); // not logged in!
            historyQuery += "where h."+tagProto.tagField()+" = :uuid " +
                    "and h.visibility = '"+ EntityVisibility.everyone +"' " +
                    "order by " + orderClause;
            params = PARAMS_UUID;
            values = new Object[]{canonical.getUuid()};

        } else {
            historyQuery += "where h."+tagProto.tagField()+" = :uuid ";
            switch (searchType) {
                case mine:
                    historyQuery += "and x.owner = :owner ";
                    break;
                case others:
                    historyQuery += "and ( x.owner != :owner and x.visibility = '"+ EntityVisibility.everyone+"' ) ";
                    break;
                default:
                    historyQuery += "and ( x.owner = :owner or x.visibility = '"+ EntityVisibility.everyone+"' ) ";
                    break;
            }
            params = PARAMS_UUID_AND_OWNER;
            values = new Object[]{account.getUuid(), canonical.getUuid()};
        }
        List<EntityHistory> histories = (List<EntityHistory>)
                hibernateTemplate.execute(new HibernateCallbackImpl(historyQuery, params, values, 0, 100));
        List<GenericEntityTag> tags = new ArrayList<>();
        historyQuery = "from "+getEntityClass().getSimpleName();
        // todo...
        for (EntityHistory history : histories) {

        }
        return tags;
    }

    public String orderClause(EntitySearchOrder entitySearchOrder) {
        switch (entitySearchOrder) {
            case most_upvotes: default: return "order by x.up_votes desc ";
            case most_downvotes: return "order by x.down_votes desc ";
            case newest: return " x.ctime desc ";
            case oldest: return" x.ctime asc ";
        }
    }


}
