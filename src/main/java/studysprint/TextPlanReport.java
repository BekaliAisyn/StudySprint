package studysprint;

public final class TextPlanReport implements PlanReport {
    public String render(Plan plan) {
        StringBuilder text=new StringBuilder("STUDY SPRINT\nBekali Aisyn | SE-2517\n\n");
        text.append(plan.name()).append("\nStrategy: ").append(plan.strategy())
            .append("\nBudget: ").append(plan.budget()).append(" min | Scheduled: ").append(plan.scheduledMinutes()).append(" min\n\n");
        for(Session s:plan.sessions()) text.append(s.done()?"[x] ":"[ ] ").append(s.title()).append(" | ")
            .append(s.subject()).append(" | ").append(s.minutes()).append(" min\n");
        return text.toString();
    }
}
