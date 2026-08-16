package com.etherstories.escore.managers;

import com.etherstories.escore.events.ServerEvent;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class EventManager {

    private final Map<String, ServerEvent> events = new LinkedHashMap<>();
    private final AtomicInteger idCounter = new AtomicInteger(1);

    public ServerEvent createEvent(Player creator, String name, String description, int maxParticipants) {
        String id = "event_" + idCounter.getAndIncrement();
        ServerEvent event = new ServerEvent(id, name, description, creator.getName(), maxParticipants);
        events.put(id, event);
        return event;
    }

    public boolean removeEvent(String id) {
        return events.remove(id) != null;
    }

    public ServerEvent getEvent(String id) {
        return events.get(id);
    }

    public Collection<ServerEvent> getAllEvents() {
        return Collections.unmodifiableCollection(events.values());
    }

    public boolean join(String eventId, UUID uuid) {
        ServerEvent event = events.get(eventId);
        return event != null && event.join(uuid);
    }

    public boolean leave(String eventId, UUID uuid) {
        ServerEvent event = events.get(eventId);
        return event != null && event.leave(uuid);
    }

    public void clear() {
        events.clear();
    }
}
