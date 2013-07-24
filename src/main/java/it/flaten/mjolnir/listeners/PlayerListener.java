package it.flaten.mjolnir.listeners;

import it.flaten.mjolnir.Mjolnir;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * {@link Listener} for {@link org.bukkit.entity.Player} related events.
 *
 * @author Jim Flaten
 */
public class PlayerListener implements Listener {
    /**
     * Plugin instance.
     *
     * A place to store a reference to the running plugin.
     */
    private Mjolnir plugin;

    /**
     * Constructor.
     *
     * Instantiates {@link PlayerListener}.
     *
     * @param plugin A reference to the running plugin.
     */
    public PlayerListener(Mjolnir plugin) {
        this.plugin = plugin;
    }

    /**
     * Handle player login event.
     *
     * Checks whether or not the connecting player is banned in Mjölnir. Kicks
     * with the configured message if so.
     *
     * @param event A {@link PlayerLoginEvent} passed from the Bukkit server.
     */
    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        String player = event.getPlayer().getName();

        if (this.plugin.isBanned(player)) {
            event.setKickMessage(this.plugin.buildKickMessage(this.plugin.why(player)));
            event.setResult(PlayerLoginEvent.Result.KICK_BANNED);
        }
    }

    /**
     * Handle player quit event.
     *
     * Removes the player from the {@link Mjolnir#why(String)} {@link it.flaten.mjolnir.beans.Event} cache.
     *
     * @param event
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.plugin.why(
            event.getPlayer().getName(),
            null
        );
    }
}
