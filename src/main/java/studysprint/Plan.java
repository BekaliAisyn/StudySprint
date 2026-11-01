package studysprint;

import java.time.Instant;
import java.util.List;

public record Plan(String id, String name, String strategy, int budget,
                   Instant created, List<Session> sessions) {
    public Plan {
        if (id == null || id.isBlank() || name == null || name.isBlank() || name.length() > 120)
            throw new IllegalArgumentException("Plan name must contain 1-120 characters.");
        if (!List.of("Deadline first", "Priority first").contains(strategy))
            throw new IllegalArgumentException("Unknown scheduling strategy.");
        if (budget < 5 || budget > 480) throw new IllegalArgumentException("Budget must be 5-480 minutes.");
        if (created == null || sessions == null || sessions.isEmpty())
            throw new IllegalArgumentException("A plan needs at least one unfinished task.");
        sessions = List.copyOf(sessions);
        if (sessions.stream().mapToInt(Session::minutes).sum() > budget)
            throw new IllegalArgumentException("Sessions exceed the budget.");
        name = name.trim();
    }
    public int scheduledMinutes() { return sessions.stream().mapToInt(Session::minutes).sum(); }
}
