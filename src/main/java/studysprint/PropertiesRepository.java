package studysprint;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/** Adapter: translates domain snapshots into the Java Properties key/value API. */
public final class PropertiesRepository implements Repository {
    private final Path path;
    public PropertiesRepository(Path path) { this.path = path.toAbsolutePath(); }
    public Snapshot load() throws IOException {
        if (!Files.exists(path)) return new Snapshot(List.of(), List.of());
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { p.load(r); }
        try {
            if (!"1".equals(required(p, "version"))) throw new IllegalArgumentException("Unsupported version");
            List<Task> tasks = new ArrayList<>(); List<Plan> plans = new ArrayList<>();
            for (int i = 0; i < count(p, "tasks"); i++) {
                String k = "task." + i + ".";
                tasks.add(new Task(required(p,k+"id"),required(p,k+"title"),required(p,k+"subject"),
                        number(p,k+"minutes"),number(p,k+"priority"),LocalDate.parse(required(p,k+"due")),bool(p,k+"done")));
            }
            for (int i = 0; i < count(p, "plans"); i++) {
                String k = "plan." + i + "."; List<Session> sessions = new ArrayList<>();
                for (int j = 0; j < count(p,k+"sessions"); j++) {
                    String s = k+"session."+j+".";
                    sessions.add(new Session(required(p,s+"taskId"),required(p,s+"title"),required(p,s+"subject"),number(p,s+"minutes"),bool(p,s+"done")));
                }
                plans.add(new Plan(required(p,k+"id"),required(p,k+"name"),required(p,k+"strategy"),number(p,k+"budget"),Instant.parse(required(p,k+"created")),sessions));
            }
            if (tasks.stream().map(Task::id).distinct().count()!=tasks.size() || plans.stream().map(Plan::id).distinct().count()!=plans.size())
                throw new IllegalArgumentException("Duplicate IDs");
            return new Snapshot(tasks, plans);
        } catch (RuntimeException e) { throw new IOException("Invalid saved data. Original file was preserved: " + path, e); }
    }
    private static String required(Properties p, String key) {
        String v = p.getProperty(key); if (v==null) throw new IllegalArgumentException("Missing " + key); return v;
    }
    private static int number(Properties p,String key) { return Integer.parseInt(required(p,key)); }
    private static int count(Properties p,String key) {
        int v=number(p,key); if(v<0 || v>10000) throw new IllegalArgumentException("Invalid count"); return v;
    }
    private static boolean bool(Properties p,String key) {
        String v=required(p,key); if(!v.equals("true") && !v.equals("false")) throw new IllegalArgumentException("Invalid boolean");
        return Boolean.parseBoolean(v);
    }
    private static void put(Properties p,String key,Object value) { p.setProperty(key,String.valueOf(value)); }
    public void save(Snapshot snapshot) throws IOException {
        Properties p=new Properties(); put(p,"version",1); put(p,"tasks",snapshot.tasks().size()); put(p,"plans",snapshot.plans().size());
        for(int i=0;i<snapshot.tasks().size();i++) {
            Task t=snapshot.tasks().get(i); String k="task."+i+".";
            put(p,k+"id",t.id()); put(p,k+"title",t.title()); put(p,k+"subject",t.subject()); put(p,k+"minutes",t.minutes());
            put(p,k+"priority",t.priority()); put(p,k+"due",t.due()); put(p,k+"done",t.done());
        }
        for(int i=0;i<snapshot.plans().size();i++) {
            Plan plan=snapshot.plans().get(i); String k="plan."+i+".";
            put(p,k+"id",plan.id()); put(p,k+"name",plan.name()); put(p,k+"strategy",plan.strategy());
            put(p,k+"budget",plan.budget()); put(p,k+"created",plan.created()); put(p,k+"sessions",plan.sessions().size());
            for(int j=0;j<plan.sessions().size();j++) {
                Session s=plan.sessions().get(j); String sk=k+"session."+j+".";
                put(p,sk+"taskId",s.taskId()); put(p,sk+"title",s.title()); put(p,sk+"subject",s.subject());
                put(p,sk+"minutes",s.minutes()); put(p,sk+"done",s.done());
            }
        }
        Files.createDirectories(path.getParent());
        Path temp=Files.createTempFile(path.getParent(),"study-",".tmp");
        try {
            try(Writer w=Files.newBufferedWriter(temp,StandardCharsets.UTF_8)) { p.store(w,"Study Sprint v1"); }
            try { Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
            catch(AtomicMoveNotSupportedException e) { Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
}
