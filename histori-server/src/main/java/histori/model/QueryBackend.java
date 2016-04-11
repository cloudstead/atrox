package histori.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum QueryBackend {

    es, pg;

    public boolean isElasticSearch () { return this == es; }
    public boolean isPostgresql () { return this == pg; }

    @JsonCreator public static QueryBackend create (String val) {
        try {
            return valueOf(val.toLowerCase());
        } catch (Exception e) {
            log.warn("create("+val+"): "+e);
            return null;
        }
    }
}
