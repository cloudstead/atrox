@TypeDefs({
        @TypeDef(name = NexusTags.TAGS_JSONB_TYPE,
                typeClass = JSONBUserType.class,
                parameters = {@Parameter(name=JSONBUserType.PARAM_CLASS, value="histori.model.base.NexusTags")}),
        @TypeDef(name = Nexus.NEXUS_JSONB_TYPE,
                typeClass = JSONBUserType.class,
                parameters = {@Parameter(name=JSONBUserType.PARAM_CLASS, value="histori.model.Nexus")})
})

package histori.model.base;

import histori.model.Nexus;
import org.cobbzilla.wizard.model.json.JSONBUserType;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;