package histori.dao.search;

import histori.model.Nexus;
import lombok.Getter;
import org.cobbzilla.util.collection.mappy.MappyList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class NexusSearchResults extends MappyList<String, Nexus> {

    public static final int MAX_RESULTS = 200;

    @Getter private final List<SuperNexusSearchTask> tasks = new ArrayList<>();

    public Callable<SuperNexusSearchTaskResult> addTask(SuperNexusSearchTask task) {
        tasks.add(task);
        return task;
    }

    @Override public Nexus put(String key, Nexus value) {
        Nexus rval = super.put(key, value);
        if (size() > MAX_RESULTS) {
            for (SuperNexusSearchTask task : tasks) task.cancel();
        }
        return rval;
    }
}
