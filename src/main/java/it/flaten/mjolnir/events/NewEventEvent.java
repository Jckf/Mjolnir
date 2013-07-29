package it.flaten.mjolnir.events;

import it.flaten.mjolnir.beans.Event;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

@SuppressWarnings("UnusedDeclaration")
public class NewEventEvent extends org.bukkit.event.Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;

    public static HandlerList getHandlerList() {
        return NewEventEvent.handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return NewEventEvent.handlers;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(final boolean cancelled) {
        this.cancelled = cancelled;
    }

    private final Event event;

    public NewEventEvent(final Event event) {
        this.event = event;
    }

    public it.flaten.mjolnir.beans.Event getEvent() {
        return this.event;
    }
}
