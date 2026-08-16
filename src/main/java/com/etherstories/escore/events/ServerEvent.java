package com.etherstories.escore.events;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class ServerEvent {

    private final String id;
    private final String creator;
    private final long createdAt;
    private String name;
    private String description;
    private int maxParticipants; // 0 = unlimited
    private final Set<UUID> participants = new LinkedHashSet<>();

    public ServerEvent(String id, String name, String description, String creator, int maxParticipants) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.creator = creator;
        this.maxParticipants = maxParticipants;
        this.createdAt = System.currentTimeMillis();
    }

    public boolean join(UUID uuid) {
        if (maxParticipants > 0 && participants.size() >= maxParticipants) return false;
        return participants.add(uuid);
    }

    public boolean leave(UUID uuid) {
        return participants.remove(uuid);
    }

    public boolean hasJoined(UUID uuid) {
        return participants.contains(uuid);
    }

    public String getId()          { return id; }
    public String getName()        { return name; }
    public void setName(String n)  { this.name = n; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public String getCreator()     { return creator; }
    public long getCreatedAt()     { return createdAt; }
    public int getMaxParticipants(){ return maxParticipants; }
    public void setMaxParticipants(int m) { this.maxParticipants = m; }
    public Set<UUID> getParticipants() { return Collections.unmodifiableSet(participants); }

    public String getParticipantDisplay() {
        return maxParticipants > 0
                ? participants.size() + "/" + maxParticipants
                : String.valueOf(participants.size());
    }
}
