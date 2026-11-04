package studysprint;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Observer: subscribers react only after a successful persistence operation. */
public final class EventBus {
    public record Event(String message, Instant at) {}
    private final List<Consumer<Event>> listeners = new ArrayList<>();
    public void subscribe(Consumer<Event> listener) { listeners.add(listener); }
    public void publish(String message) {
        Event event = new Event(message, Instant.now());
        for (Consumer<Event> listener : List.copyOf(listeners)) {
            try { listener.accept(event); }
            catch (RuntimeException error) { System.err.println("Observer failed: " + error.getMessage()); }
        }
    }
}
