package it.flaten.mjolnir.storages;

import com.avaje.ebean.*;
import it.flaten.mjolnir.Mjolnir;
import it.flaten.mjolnir.beans.Event;

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
    private Mjolnir plugin;

    /**
     * Constructor.
     *
     * Instantiates {@link NativeStorage}.
     *
     * @param plugin A reference to the running plugin.
     */
    public NativeStorage(Mjolnir plugin) {
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
    public Event saveEvent(String player,String op,Event.EventType type,String reason,int expires) {
        Event event = this.plugin.getDatabase().createEntityBean(Event.class);

        event.setTime((int) (System.currentTimeMillis() / 1000L));
        event.setPlayer(player);
        event.setOp(op);
        event.setType(type);
        event.setReason(reason);
        event.setExpires(expires);

        this.plugin.getDatabase().save(event);

        return event;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Event> loadEvents(String player) {
        Query<Event> query = this.plugin.getDatabase()
            .find(Event.class)
            .where()
                .ieq("player",player)
            .orderBy("id ASC");

        List<Event> events = query.findList();

        if (events == null || events.size() == 0) {
            return null;
        }

        return events;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Event loadActiveEvent(String player) {
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
    public void shutdown() {

    }
}
