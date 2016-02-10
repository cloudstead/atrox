package atrox.model.support;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum EntitySearchType {

    none, all, mine, others;

    @JsonCreator public static EntitySearchType create (String val) {
        try {
            return EntitySearchType.valueOf(val.toLowerCase());
        } catch (Exception e) {
            log.warn("create: invalid tag type: "+val);
            return all;
        }
    }

}
