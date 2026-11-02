package studysprint;

import java.util.List;

/** Strategy: replace ordering without changing budget allocation or the GUI. */
public interface SchedulingStrategy {
    String name();
    List<Task> order(List<Task> tasks);
}
