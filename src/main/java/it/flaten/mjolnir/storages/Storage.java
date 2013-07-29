package it.flaten.mjolnir.storages;

import it.flaten.mjolnir.beans.Event;

import java.util.List;

/**
 * Interface defining the structure of a storage.
 *
 * @author Jim Flaten
 */
public interface Storage {
    /**
     * Create tables to store data.
     *
     * This method is automatically invoked when the plugin
     * is enabled, and is meant to prepare storage space for
     * the data.
     */
    public void createTables();

    /**
     * Write an {@link Event} to storage.
     *
     * This method is used to save an {@link Event} to the storage
     * for later use. It is also responsible for firing {@link it.flaten.mjolnir.Mjolnir#preProcess(it.flaten.mjolnir.beans.Event)}
     * and {@link it.flaten.mjolnir.Mjolnir#postProcess(it.flaten.mjolnir.beans.Event)}, and respecting {@link it.flaten.mjolnir.events.NewEventEvent#isCancelled()}.
     *
     * @param player  The name of the player this {@link Event} belongs to.
     * @param op      The name of the player who created this event.
     * @param type    The {@link it.flaten.mjolnir.beans.Event.EventType} of this {@link Event}.
     * @param reason  The reason for this {@link Event}.
     * @param expires A UNIX timestamp denoting expiration time.
     * @return        The resulting {@link Event}.
     */
    public Event saveEvent(final String player,final String op,final Event.EventType type,final String reason,final int expires);

    /**
     * Load all {@link Event}s for a given player.
     *
     * Fetches all stored {@link Event}s for the given player.
     *
     * @param player The name of the player whose {@link Event}s to fetch.
     * @return       A {@link List} of {@link Event}s.
     */
    public List<Event> loadEvents(final String player);

    /**
     * Load the active {@link Event} for a given player.
     *
     * Fetch the newest non-expired {@link Event} for the given player. Will
     * return null if no {@link Event} is found.
     *
     * @param player The name of the player wose {@link Event} to fetch.
     * @return       The active {@link Event}, or null.
     */
    public Event loadActiveEvent(final String player);

    /**
     * Shut down the storage.
     *
     * This method is automatically invoked when the plugin
     * is disabled, and is meant to handle final writes, closes,
     * cleanups, and similar.
     */
    public void shutdown();
}
