package studysprint;

import java.util.Comparator;
import java.util.List;

public final class PriorityStrategy implements SchedulingStrategy {
    public String name() { return "Priority first"; }
    public List<Task> order(List<Task> tasks) {
        return tasks.stream().filter(t -> !t.done()).sorted(Comparator.comparingInt(Task::priority)
                .reversed().thenComparing(Task::due).thenComparing(Task::id)).toList();
    }
}
