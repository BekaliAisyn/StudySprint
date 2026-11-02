package studysprint;

import java.util.Comparator;
import java.util.List;

public final class DeadlineStrategy implements SchedulingStrategy {
    public String name() { return "Deadline first"; }
    public List<Task> order(List<Task> tasks) {
        return tasks.stream().filter(t -> !t.done()).sorted(Comparator.comparing(Task::due)
                .thenComparing(Comparator.comparingInt(Task::priority).reversed()).thenComparing(Task::id)).toList();
    }
}
