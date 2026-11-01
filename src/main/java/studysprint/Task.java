package studysprint;

import java.time.LocalDate;

/** An editable study requirement. Plans keep snapshots, not mutable task references. */
public record Task(String id, String title, String subject, int minutes,
                   int priority, LocalDate due, boolean done) {
    public Task {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Task ID is required.");
        if (title == null || title.isBlank() || title.length() > 120)
            throw new IllegalArgumentException("Title must contain 1-120 characters.");
        if (subject == null || subject.isBlank() || subject.length() > 80)
            throw new IllegalArgumentException("Subject must contain 1-80 characters.");
        if (minutes < 5 || minutes > 480) throw new IllegalArgumentException("Duration must be 5-480 minutes.");
        if (priority < 1 || priority > 3) throw new IllegalArgumentException("Priority must be 1-3.");
        if (due == null) throw new IllegalArgumentException("Due date is required.");
        title = title.trim(); subject = subject.trim();
    }
}
