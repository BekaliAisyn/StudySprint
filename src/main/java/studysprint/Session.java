package studysprint;

/** Immutable snapshot preserves a saved plan when its source task is edited or deleted. */
public record Session(String taskId, String title, String subject, int minutes, boolean done) {
    public Session {
        if (taskId == null || taskId.isBlank() || title == null || title.isBlank()
                || subject == null || subject.isBlank() || minutes < 1 || minutes > 480)
            throw new IllegalArgumentException("Invalid study session.");
    }
    public Session completed(boolean value) { return new Session(taskId, title, subject, minutes, value); }
}
