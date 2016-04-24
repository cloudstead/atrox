@TypeDefs({
        @TypeDef(name = NexusTags.JSONB_TYPE,
                typeClass = JSONBUserType.class,
                parameters = {@Parameter(name=JSONBUserType.PARAM_CLASS, value="histori.model.base.NexusTags")})
})

package histori.model.base;

import org.cobbzilla.wizard.model.json.JSONBUserType;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;