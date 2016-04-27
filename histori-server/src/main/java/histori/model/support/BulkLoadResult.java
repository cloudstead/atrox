package histori.model.support;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.wizard.validation.ValidationResult;

import java.util.ArrayList;
import java.util.List;

import static org.cobbzilla.util.daemon.ZillaRuntime.now;

public class BulkLoadResult {

    @Getter @Setter private long startTime;
    @Getter @Setter private boolean completed;
    @Getter @Setter private boolean cancelled = false;
    @Getter @Setter private Exception exception;
    @Getter @Setter private List<String> successes = new ArrayList<>();
    @Getter private ValidationResult validation = new ValidationResult();

    @JsonIgnore public long getAge () { return now() - startTime; }

}
