package studysprint;

/** Decorator: enriches an existing report through the same component interface. */
public final class ProgressReportDecorator implements PlanReport {
    private final PlanReport wrapped;
    public ProgressReportDecorator(PlanReport wrapped) { this.wrapped=wrapped; }
    public String render(Plan plan) {
        long complete=plan.sessions().stream().filter(Session::done).count();
        int minutes=plan.sessions().stream().filter(Session::done).mapToInt(Session::minutes).sum();
        return wrapped.render(plan)+"\nPROGRESS SUMMARY\nCompleted sessions: "+complete+" / "+plan.sessions().size()
            +"\nCompleted minutes: "+minutes+" / "+plan.scheduledMinutes()+"\n";
    }
}
