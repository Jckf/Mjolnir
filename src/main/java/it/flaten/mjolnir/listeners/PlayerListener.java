package it.flaten.mjolnir.listeners;

import it.flaten.mjolnir.Mjolnir;
import it.flaten.mjolnir.tasks.PlayerTask;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;

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
    private final Mjolnir plugin;

    /**
     * Task ID map.
     *
     * A place to store players' task IDs.
     */
    private final Map<String,Integer> taskMap = new HashMap<>();

    /**
     * Constructor.
     *
     * Instantiates {@link PlayerListener}.
     *
     * @param plugin A reference to the running plugin.
     */
    public PlayerListener(final Mjolnir plugin) {
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
    public void onPlayerLogin(final PlayerLoginEvent event) {
        String player = event.getPlayer().getName();

        /**
         * Check if the player is banned.
         *
         * Kick with the appropriate message if so.
         */
        if (this.plugin.isBanned(player)) {
            event.setKickMessage(this.plugin.buildKickMessage(this.plugin.why(player)));
            event.setResult(PlayerLoginEvent.Result.KICK_BANNED);
            return;
        }

        /**
         * Start the player's task.
         *
         * This will make sure the player is kicked if a temporary unban expires, or
         * if a ban is added externally.
         */
        this.taskMap.put(
            player,
            this.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(this.plugin,new PlayerTask(this.plugin,player),20 * 60,20 * 60)
        );
    }

    /**
     * Handle player quit event.
     *
     * Removes the player from the {@link Mjolnir#why(String)} {@link it.flaten.mjolnir.beans.Event} cache.
     *
     * @param event A {@link PlayerQuitEvent} passed from the Bukkit server.
     */
    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        String player = event.getPlayer().getName();

        /**
         * Stop the player's task.
         */
        this.plugin.getServer().getScheduler().cancelTask(this.taskMap.get(player));

        /**
         * Remove the task ID from memory.
         */
        this.taskMap.remove(player);

        /**
         * Remove the {@link Event} stored for this player.
         */
        this.plugin.why(
            player,
            null
        );
    }
}
