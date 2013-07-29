package it.flaten.mjolnir.storages;

import com.avaje.ebean.*;
import it.flaten.mjolnir.Mjolnir;
import it.flaten.mjolnir.beans.Event;
import it.flaten.mjolnir.events.NewEventEvent;

import javax.persistence.PersistenceException;
import java.util.List;

/**
 * Implementation of Bukkit's native database, for Mjölnir.
 *
 * @author Jim Flaten
 */
public class NativeStorage implements Storage {
    /**
     * Plugin instance.
     *
     * A place to store a reference to the running plugin.
     */
    private final Mjolnir plugin;

    /**
     * Constructor.
     *
     * Instantiates {@link NativeStorage}.
     *
     * @param plugin A reference to the running plugin.
     */
    public NativeStorage(final Mjolnir plugin) {
        this.plugin = plugin;
    }

    /**
     * Create database tables.
     *
     * Tries to count the number of {@link Event}s in the database, and
     * invokes {@link it.flaten.mjolnir.Mjolnir#installDDL()} if an @{link PersistenceException} is thrown.
     */
    @Override
    public void createTables() {
        try {
            this.plugin.getDatabase().find(Event.class).findRowCount();
        } catch (PersistenceException exception) {
            this.plugin.installDDL();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Event saveEvent(final String player,final String op,final Event.EventType type,final String reason,final int expires) {
        final Event event = this.plugin.getDatabase().createEntityBean(Event.class);

        event.setTime((int) (System.currentTimeMillis() / 1000L));
        event.setPlayer(player);
        event.setOp(op);
        event.setType(type);
        event.setReason(reason);
        event.setExpires(expires);

        NewEventEvent newEventEvent = this.plugin.preProcess(event);

        if (newEventEvent.isCancelled()) {
            return null;
        }

        this.plugin.getDatabase().save(event);

        this.plugin.postProcess(event);

        return event;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Event> loadEvents(final String player) {
        final Query<Event> query = this.plugin.getDatabase()
            .find(Event.class)
            .where()
                .ieq("player",player)
            .orderBy("id ASC");

        final List<Event> events = query.findList();

        if (events == null || events.size() == 0) {
            return null;
        }

        return events;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Event loadActiveEvent(final String player) {
        return this.plugin.getDatabase()
            .find(Event.class)
            .where()
                .ieq("player",player)
                .disjunction()
                    .eq("expires",0)
                    .ge("expires",(int) (System.currentTimeMillis() / 1000L))
            .orderBy("id DESC")
            .setMaxRows(1)
            .findUnique();
    }

    /**
     * Does nothing.
     *
     * The native Bukkit database does not need to be shut down.
     */
    @Override
    @SuppressWarnings("EmptyMethod")
    public void shutdown() {

    }
}
