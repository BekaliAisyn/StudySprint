package studysprint;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

/** Facade: transactional use cases coordinate validation, patterns, storage and events. */
public final class StudyFacade {
    private final Repository repository;
    private final EventBus events;
    private Repository.Snapshot state;
    public StudyFacade(Repository repository,EventBus events) throws IOException {
        this.repository=repository; this.events=events; state=repository.load();
    }
    public List<Task> tasks() { return state.tasks(); }
    public List<Plan> plans() { return state.plans(); }
    private void commit(List<Task> tasks,List<Plan> plans,String event) throws IOException {
        Repository.Snapshot next=new Repository.Snapshot(tasks,plans);
        repository.save(next); // State and observers change only if persistence succeeds.
        state=next; events.publish(event);
    }
    public Task saveTask(String id,String title,String subject,int minutes,int priority,LocalDate due,boolean done) throws IOException {
        boolean create=id==null;
        if(!create && tasks().stream().noneMatch(t->t.id().equals(id))) throw new IllegalArgumentException("Task no longer exists.");
        Task task=new Task(create?UUID.randomUUID().toString():id,title,subject,minutes,priority,due,done);
        List<Task> next=new ArrayList<>(tasks()); next.removeIf(t->t.id().equals(task.id())); next.add(task);
        commit(next,plans(),(create?"Created task: ":"Updated task: ")+task.title()); return task;
    }
    public void deleteTask(String id) throws IOException {
        List<Task> next=new ArrayList<>(tasks());
        if(!next.removeIf(t->t.id().equals(id))) throw new IllegalArgumentException("Select an existing task.");
        commit(next,plans(),"Deleted task; saved plan snapshots retained.");
    }
    public Plan generate(String name,int budget,SchedulingStrategy strategy) throws IOException {
        if(strategy==null) throw new IllegalArgumentException("Choose a strategy.");
        PlanBuilder builder=new PlanBuilder().named(name).configured(strategy.name(),budget);
        int remaining=budget;
        for(Task t:strategy.order(tasks())) {
            if(remaining<=0) break;
            int minutes=Math.min(remaining,t.minutes());
            builder.add(new Session(t.id(),t.title(),t.subject(),minutes,false)); remaining-=minutes;
        }
        Plan plan=builder.build(); List<Plan> next=new ArrayList<>(plans()); next.add(plan);
        commit(tasks(),next,"Generated plan: "+plan.name()+" using "+plan.strategy()); return plan;
    }
    public Plan plan(String id) { return plans().stream().filter(p->p.id().equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Select a saved plan.")); }
    public void toggleSession(String id,int index) throws IOException {
        Plan old=plan(id);
        if(index<0 || index>=old.sessions().size()) throw new IllegalArgumentException("Select a session.");
        List<Session> sessions=new ArrayList<>(old.sessions()); Session s=sessions.get(index); sessions.set(index,s.completed(!s.done()));
        Plan updated=new Plan(old.id(),old.name(),old.strategy(),old.budget(),old.created(),sessions);
        List<Plan> next=new ArrayList<>(plans()); next.set(next.indexOf(old),updated);
        commit(tasks(),next,"Session "+(s.done()?"reopened: ":"completed: ")+s.title());
    }
    public void deletePlan(String id) throws IOException {
        Plan old=plan(id); List<Plan> next=new ArrayList<>(plans()); next.remove(old); commit(tasks(),next,"Deleted plan: "+old.name());
    }
    public String report(String id,boolean progress) {
        PlanReport report=new TextPlanReport(); if(progress) report=new ProgressReportDecorator(report);
        return report.render(plan(id));
    }
    public void seedIfEmpty() throws IOException {
        if(!tasks().isEmpty() || !plans().isEmpty()) return;
        LocalDate today=LocalDate.now();
        commit(List.of(new Task(UUID.randomUUID().toString(),"Review design patterns","Software Design",45,3,today.plusDays(2),false),
                new Task(UUID.randomUUID().toString(),"Solve graph exercises","Algorithms",60,2,today.plusDays(1),false),
                new Task(UUID.randomUUID().toString(),"Practice SQL joins","Databases",30,1,today.plusDays(3),false)),List.of(),"Loaded three sample tasks.");
    }
}
