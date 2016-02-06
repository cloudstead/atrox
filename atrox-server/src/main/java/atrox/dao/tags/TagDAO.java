package atrox.dao.tags;

import atrox.dao.AccountOwnedEntityDAO;
import atrox.model.Account;
import atrox.model.AccountOwnedEntity;
import atrox.model.support.EntityVisibility;
import atrox.model.support.TagOrder;
import atrox.model.support.TagSearchType;
import atrox.model.tags.EntityTag;
import atrox.server.AtroxConfiguration;
import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.wizard.dao.HibernateCallbackImpl;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public abstract class TagDAO<E extends AccountOwnedEntity> extends AccountOwnedEntityDAO<E> {

    @Autowired protected AtroxConfiguration configuration;

    public List<EntityTag> findTags(Account account, String entityType, String uuid, TagSearchType tagSearchType, TagOrder tagOrder) {

        if (tagSearchType == TagSearchType.none) return new ArrayList<>();

        String queryString;
        final Object[] values;
        if (account == null) {
            if (tagSearchType == TagSearchType.mine) return new ArrayList<>(); // not logged in!
            queryString = "from " + getEntityClass().getSimpleName() + " x " +
                          "where x."+StringUtil.uncapitalize(entityType)+" = :uuid " +
                          "x.visibility = '"+ EntityVisibility.everyone +"' " +
                          "order by ";
            queryString += orderClause(tagOrder);
            values = new Object[]{uuid};

        } else {
            queryString = "from " + getEntityClass().getSimpleName() + " x " +
                    "where x."+StringUtil.uncapitalize(entityType)+" = :uuid ";
            switch (tagSearchType) {
                case mine:
                    queryString += "and x.owner = :owner ";
                    break;
                case others:
                    queryString += "and ( x.owner != :owner and x.visibility = '"+ EntityVisibility.everyone+"' ) ";
                    break;
                default:
                    queryString += "and ( x.owner = :owner or x.visibility = '"+ EntityVisibility.everyone+"' ) ";
                    break;

            }
            queryString += "order by (x.voteSummary.upVotes - x.voteSummary.downVotes) desc ";
            values = new Object[]{account.getUuid(), uuid};
        }
        return (List) hibernateTemplate.execute(new HibernateCallbackImpl(queryString, PARAMS_STARTSWITH, values, 0, 10));
    }

    public String orderClause(TagOrder tagOrder) {
        switch (tagOrder) {
            case most_upvotes: default: return "order by x.voteSummary.upVotes desc ";
            case most_downvotes: return "order by x.voteSummary.downVotes desc ";
            case highest_vote_total: return "order by (x.voteSummary.upVotes - x.voteSummary.downVotes) desc ";
            case newest: return " x.ctime desc ";
            case oldest: return" x.ctime asc ";
        }
    }

}
