package histori.dao.search;

import lombok.Getter;
import lombok.Setter;

public class SuperNexusSearchTaskResult {

    @Getter @Setter private SuperNexusSearchTask task;
    @Getter @Setter private Boolean result;
    @Getter @Setter private Exception exception;

    public boolean isSuccess () { return result != null && result; }

    public SuperNexusSearchTaskResult(SuperNexusSearchTask task, Exception e) {
        this.task = task;
        this.result = false;
        this.exception = e;
    }

    public SuperNexusSearchTaskResult(SuperNexusSearchTask task) {
        this.task = task;
        this.result = true;
    }
}
