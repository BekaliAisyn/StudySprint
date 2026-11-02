package studysprint;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Builder: gradually assembles a plan; build is the single validation boundary. */
public final class PlanBuilder {
    private String name;
    private String strategy;
    private int budget;
    private final List<Session> sessions = new ArrayList<>();
    public PlanBuilder named(String value) { name = value; return this; }
    public PlanBuilder configured(String value, int minutes) { strategy = value; budget = minutes; return this; }
    public PlanBuilder add(Session session) { sessions.add(session); return this; }
    public Plan build() { return new Plan(UUID.randomUUID().toString(), name, strategy, budget, Instant.now(), sessions); }
}
