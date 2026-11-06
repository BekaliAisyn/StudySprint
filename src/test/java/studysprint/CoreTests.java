package studysprint;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

/** Dependency-free automated regression suite. Failing checks terminate with a nonzero exit code. */
public final class CoreTests {
    @FunctionalInterface interface Checked { void run() throws Exception; }
    private static int passed;
    private static final LocalDate DATE=LocalDate.of(2026,11,8);
    private static void test(String name,Checked action) throws Exception { action.run(); passed++; System.out.println("PASS | "+name); }
    private static void check(boolean condition,String message) {if(!condition) throw new AssertionError(message);}
    private static void rejects(Checked action) throws Exception {
        try {action.run();} catch(IllegalArgumentException expected) {return;} throw new AssertionError("Expected validation error");
    }
    private static final class Memory implements Repository {
        Snapshot data=new Snapshot(List.of(),List.of()); boolean fail;
        public Snapshot load() {return data;}
        public void save(Snapshot next) throws IOException {if(fail) throw new IOException("Simulated disk failure"); data=next;}
    }
    private static StudyFacade populated(Repository repo,EventBus bus) throws Exception {
        StudyFacade f=new StudyFacade(repo,bus);
        f.saveTask(null,"Urgent reading","Algorithms",60,1,DATE,false);
        f.saveTask(null,"Important practice","Design",45,3,DATE.plusDays(2),false); return f;
    }
    public static void main(String[] args) throws Exception {
        test("01 Main journey creates a budget-limited plan and exports progress",()->{
            StudyFacade f=populated(new Memory(),new EventBus()); Plan p=f.generate("Morning",90,new DeadlineStrategy());
            check(p.sessions().size()==2 && p.scheduledMinutes()==90,"budget allocation");
            check(p.sessions().get(1).minutes()==30,"partial task allocation");
            f.toggleSession(p.id(),0); check(f.report(p.id(),true).contains("Completed minutes: 60 / 90"),"progress export");
        });
        test("02 Switching strategies changes first session",()->{
            StudyFacade f=populated(new Memory(),new EventBus());
            check(f.generate("A",60,new DeadlineStrategy()).sessions().get(0).title().equals("Urgent reading"),"deadline first");
            check(f.generate("B",60,new PriorityStrategy()).sessions().get(0).title().equals("Important practice"),"priority first");
        });
        test("03 Observer receives successful mutations",()->{
            EventBus bus=new EventBus(); ActivityLog log=new ActivityLog(); bus.subscribe(log); int[] refresh={0}; bus.subscribe(e->refresh[0]++);
            StudyFacade f=populated(new Memory(),bus); f.generate("A",60,new DeadlineStrategy());
            check(log.entries().size()==3 && refresh[0]==3,"both observers called");
        });
        test("04 Save and reload tasks plans and session progress",()->{
            Path path=Files.createTempDirectory("study-test-").resolve("data.properties");
            try {
                StudyFacade f=populated(new PropertiesRepository(path),new EventBus()); Plan p=f.generate("Saved",90,new PriorityStrategy()); f.toggleSession(p.id(),0);
                StudyFacade restored=new StudyFacade(new PropertiesRepository(path),new EventBus());
                check(restored.tasks().equals(f.tasks()) && restored.plans().equals(f.plans()),"round trip");
            } finally {Files.deleteIfExists(path); Files.delete(path.getParent());}
        });
        test("05 Empty task title rejected without mutation",()->{
            StudyFacade f=new StudyFacade(new Memory(),new EventBus()); rejects(()->f.saveTask(null," ","Math",30,1,DATE,false)); check(f.tasks().isEmpty(),"no bad task");
        });
        test("06 Duration and priority boundaries",()->{
            for(int minutes:new int[]{0,4,481}) rejects(()->new Task("a","A","B",minutes,1,DATE,false));
            for(int priority:new int[]{0,4}) rejects(()->new Task("a","A","B",30,priority,DATE,false));
            new Task("a","A","B",5,1,DATE,false); new Task("b","A","B",480,3,DATE,false);
        });
        test("07 Empty backlog and all-done backlog rejected",()->{
            StudyFacade f=new StudyFacade(new Memory(),new EventBus()); rejects(()->f.generate("A",30,new DeadlineStrategy()));
            f.saveTask(null,"A","B",30,1,DATE,true); rejects(()->f.generate("A",30,new PriorityStrategy()));
        });
        test("08 Invalid plan names and budgets rejected",()->{
            StudyFacade f=populated(new Memory(),new EventBus());
            rejects(()->f.generate(" ",30,new DeadlineStrategy())); rejects(()->f.generate("A",4,new DeadlineStrategy())); rejects(()->f.generate("A",481,new DeadlineStrategy()));
            check(f.plans().isEmpty(),"no bad plans persisted");
        });
        test("09 Task CRUD retains independent saved snapshots",()->{
            StudyFacade f=populated(new Memory(),new EventBus()); Task t=f.tasks().get(0); Plan p=f.generate("A",60,new DeadlineStrategy());
            f.saveTask(t.id(),"Edited","Math",20,2,DATE,true); check(f.tasks().stream().anyMatch(x->x.title().equals("Edited")),"edit saved");
            f.deleteTask(t.id()); check(f.plan(p.id()).sessions().get(0).title().equals("Urgent reading"),"snapshot remains");
            f.deletePlan(p.id()); check(f.plans().isEmpty(),"plan deleted");
        });
        test("10 Disk failure preserves memory and emits no event",()->{
            Memory repo=new Memory(); EventBus bus=new EventBus(); ActivityLog log=new ActivityLog(); bus.subscribe(log); StudyFacade f=populated(repo,bus); repo.fail=true;
            try {f.deleteTask(f.tasks().get(0).id()); throw new AssertionError("expected I/O failure");} catch(IOException expected) { }
            check(f.tasks().size()==2 && log.entries().size()==2,"transaction rollback");
        });
        test("11 Corrupt storage rejected without overwriting",()->{
            Path path=Files.createTempFile("study-corrupt-",".properties"); String bad="version=1\ntasks=not-a-number\nplans=0\n";
            try {Files.writeString(path,bad); try {new PropertiesRepository(path).load(); throw new AssertionError("expected I/O error");} catch(IOException expected) { }
                check(Files.readString(path).equals(bad),"corrupt file preserved");
            } finally {Files.delete(path);}
        });
        test("12 Decorator adds optional content without changing base",()->{
            StudyFacade f=populated(new Memory(),new EventBus()); Plan p=f.generate("A",60,new DeadlineStrategy());
            String base=f.report(p.id(),false), extra=f.report(p.id(),true);
            check(!base.contains("PROGRESS SUMMARY") && extra.startsWith(base) && extra.contains("PROGRESS SUMMARY"),"decoration");
        });
        test("13 Missing objects and invalid session indexes rejected",()->{
            StudyFacade f=populated(new Memory(),new EventBus()); Plan p=f.generate("A",60,new DeadlineStrategy());
            rejects(()->f.deleteTask("missing")); rejects(()->f.toggleSession(p.id(),-1)); rejects(()->f.toggleSession(p.id(),9)); rejects(()->f.plan("missing"));
        });
        test("14 Builder snapshot cannot be changed after build",()->{
            PlanBuilder b=new PlanBuilder().named("A").configured("Deadline first",60).add(new Session("a","A","B",30,false));
            Plan p=b.build(); b.add(new Session("b","B","B",30,false)); check(p.sessions().size()==1,"builder isolation");
            try {p.sessions().clear(); throw new AssertionError("mutable list");} catch(UnsupportedOperationException expected) { }
        });
        test("15 Unicode and special characters survive storage",()->{
            Path path=Files.createTempDirectory("study-unicode-").resolve("data.properties");
            try {StudyFacade f=new StudyFacade(new PropertiesRepository(path),new EventBus());
                f.saveTask(null,"Қазақ тілі = Java: тест","Язык\\Code",30,2,DATE,false);
                check(new PropertiesRepository(path).load().tasks().equals(f.tasks()),"unicode round trip");
            } finally {Files.deleteIfExists(path); Files.delete(path.getParent());}
        });
        System.out.println("RESULT: "+passed+"/"+passed+" tests passed");
    }
}
