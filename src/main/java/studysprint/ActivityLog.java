package studysprint;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** A non-GUI observer, independent from the window's refresh subscriber. */
public final class ActivityLog implements Consumer<EventBus.Event> {
    private final List<String> entries = new ArrayList<>();
    public void accept(EventBus.Event event) {
        entries.add(0, event.at() + "  " + event.message());
        if (entries.size() > 100) entries.remove(entries.size() - 1);
    }
    public List<String> entries() { return List.copyOf(entries); }
}
