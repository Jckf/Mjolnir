package it.flaten.mjolnir.events;

import it.flaten.mjolnir.beans.Event;
import org.bukkit.event.HandlerList;

@SuppressWarnings("UnusedDeclaration")
public class IsBannedEvent extends org.bukkit.event.Event {
    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlerList() {
        return IsBannedEvent.handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return IsBannedEvent.handlers;
    }

    private final String player;
    private Event event;

    public IsBannedEvent(final String player) {
        this.player = player;
    }

    public String getPlayer() {
        return this.player;
    }

    public void setEvent(final Event event) {
        this.event = event;
    }

    public Event getEvent() {
        return this.event;
    }
}
