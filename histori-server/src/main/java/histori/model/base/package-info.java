@TypeDefs({
        @TypeDef(name = NexusTags.TAGS_JSONB_TYPE,
                typeClass = JSONBUserType.class,
                parameters = {@Parameter(name=JSONBUserType.PARAM_CLASS, value="histori.model.base.NexusTags")}),
        @TypeDef(name = NexusTemplate.NEXUS_TEMPLATE_JSONB_TYPE,
                typeClass = JSONBUserType.class,
                parameters = {@Parameter(name=JSONBUserType.PARAM_CLASS, value="histori.model.template.NexusTemplate")})
})

package histori.model.base;

import histori.model.template.NexusTemplate;
import org.cobbzilla.wizard.model.json.JSONBUserType;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;