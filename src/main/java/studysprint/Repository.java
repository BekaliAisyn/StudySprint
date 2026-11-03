package studysprint;

import java.io.IOException;
import java.util.List;

public interface Repository {
    record Snapshot(List<Task> tasks, List<Plan> plans) {
        public Snapshot { tasks = List.copyOf(tasks); plans = List.copyOf(plans); }
    }
    Snapshot load() throws IOException;
    void save(Snapshot snapshot) throws IOException;
}
