package it.flaten.mjolnir;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.flaten.mjolnir.beans.Event;
import it.flaten.mjolnir.commands.*;
import it.flaten.mjolnir.events.IsBannedEvent;
import it.flaten.mjolnir.events.NewEventEvent;
import it.flaten.mjolnir.listeners.PlayerListener;
import it.flaten.mjolnir.storages.NativeStorage;
import it.flaten.mjolnir.storages.Storage;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * The plugin's main class.
 *
 * We extend Bukkit's {@link JavaPlugin} class, and implement our own
 * entry and exit points to handle plugin initialization.
 *
 * @author Jim Flaten
 */
public class Mjolnir extends JavaPlugin {
    /**
     * Where we'll keep our {@link Storage} implementation.
     *
     * The variable is accessed by various methods that need
     * to communicate directly with the storage.
     */
    private Storage storage;

    /**
     * A list of our beans.
     *
     * This can be used by {@link Storage} implementations to see which
     * beans it should expect.
     */
    @SuppressWarnings("WeakerAccess")
    public final static List<Class<?>> databaseClasses = new ArrayList<Class<?>>() {{
        add(Event.class);
    }};

    private final Map<String,Event> whyMap = new HashMap<>();
    private final Map<UUID, Map<Integer, String>> nameHistoryCache = new HashMap<>();

    /**
     * Plugin entry point.
     *
     * This method is automatically invoked by the Bukkit server
     * implementation when the plugin is enabled. It handles plugin
     * configuration, storage initialization, Bukkit event bindings,
     * and command bindings.
     */
    @Override
    public void onEnable() {
        this.getLogger().info(" * Configuration...");

        /**
         * Copy default configuration.
         *
         * This will copy config.yml from the root of the plugin if no
         * configuration file exists on the server.
         */
        this.saveDefaultConfig();

        /**
         * Copy default configuration.
         *
         * This will add missing nodes to the plugin's configuration file
         * from the default configuration file, if any.
         */
        this.getConfig().options().copyDefaults(true);

        /**
         * Save the configuration.
         *
         * This will make sure the configuration file reflects what we're
         * working with.
         */
        this.saveConfig();

        this.getLogger().info(" * Storage...");

        /**
         * Detect and initialize {@link Storage} implementation.
         *
         * If we don't know which implementation the configuration asks for,
         * disable the plugin.
         */
        switch (this.getConfig().getString("storage.method").toLowerCase()) {
            case "native":
                this.storage = new NativeStorage(this);
                break;

            default:
                this.getLogger().severe("Unknown storage method!");
                this.getPluginLoader().disablePlugin(this);
                return;
        }

        /**
         * Create database tables.
         *
         * This will create the database tables required to store our beans,
         * unless they already exist.
         */
        this.storage.createTables();

        this.getLogger().info(" * Event handlers...");

        /**
         * Register event handlers.
         *
         * These are the classes we use to listen for Bukkit events representing
         * actions on the server.
         */
        this.getServer().getPluginManager().registerEvents(new PlayerListener(this),this);

        this.getLogger().info(" * Command handlers...");

        /**
         * Register command handlers.
         *
         * These are the classes that handle commands both in the console and
         * in-game.
         */
        this.getCommand("infractions").setExecutor(new InfoCommand(this));
        this.getCommand("tempban").setExecutor(new TempBanCommand(this));
        this.getCommand("ban").setExecutor(new BanCommand(this));
        this.getCommand("tempunban").setExecutor(new TempUnbanCommand(this));
        this.getCommand("unban").setExecutor(new UnbanCommand(this));
    }

    /**
     * Plugin exit point.
     *
     * This method is invoked by the Bukkit implementation before the
     * plugin is disabled. Usually before the server shuts down. It
     * releases command and event binds, and shuts down the storage.
     */
    @Override
    public void onDisable() {
        this.getLogger().info(" * Command handlers...");

        /**
         * Remove command handlers.
         *
         * Setting executor to null removes the last reference to the
         * handler class instances, disables processing of the commands,
         * and allows the memory to be freed.
         */
        this.getCommand("unban").setExecutor(null);
        this.getCommand("tempunban").setExecutor(null);
        this.getCommand("ban").setExecutor(null);
        this.getCommand("tempban").setExecutor(null);
        this.getCommand("infractions").setExecutor(null);

        this.getLogger().info(" * Event handlers...");

        /**
         * Unregister from events.
         *
         * Make sure we no longer receive any events before we shut down
         * the {@link Storage} instance.
         */
        HandlerList.unregisterAll(this);

        this.getLogger().info(" * Storage...");

        /**
         * Shut down the storage instance.
         *
         * What is done here depends on the implementing class. {@link it.flaten.mjolnir.storages.NativeStorage#shutdown()}
         * for example, does nothing.
         */
        this.storage.shutdown();

        /**
         * Remove the last reference to the {@link Storage} instance.
         *
         * This allows the memory to be freed.
         */
        this.storage = null;
    }

    /**
     * Get the beans used for storage.
     *
     * This information can be used by {@link Storage} implementations to prepare
     * for the types of data they will receive.
     *
     * @return A list of {@link Class}es.
     */
    @Override
    public List<Class<?>> getDatabaseClasses() {
        return Mjolnir.databaseClasses;
    }

    /**
     * Create tables required for data storage.
     *
     * This method does nothing more than expose the protected {@link org.bukkit.plugin.java.JavaPlugin#installDDL()}
     * in the JavaPlugin class so that classes implementing {@link Storage} can use
     * it if needed.
     */
    @Override
    public void installDDL() {
        super.installDDL();
    }

    /**
     * Get all events for a player.
     *
     * Fetchs all the events stored for the given player.
     *
     * @param player The name of the player whose {@link Event}s to fetch.
     * @return       A {@link List} of {@link Event}s, oldest first.
     */
    public List<Event> getEventHistory(final String player) {
        return this.storage.loadEvents(player);
    }

    /**
     * Get the active {@link Event} for a player.
     *
     * Fetches the latest stored {@link Event} for the given player
     * that has not expired. Will return null of no {@link Event} is
     * found.
     *
     * @param player The name of the player whose {@link Event} to fetch.
     * @return       The active {@link Event}, or null.
     */
    public Event getActiveEvent(final String player) {
        return this.storage.loadActiveEvent(player);
    }

    /**
     * Get an {@link Event} for an external plugin.
     *
     * Fires an {@link IsBannedEvent} and returns the {@link Event} returned from it,
     * or null if there is no {@link Event}.
     *
     * @param player The name of the player whose {@link Event} to fetch.
     * @return       An {@link Event} created by an external plugin, or null.
     */
    public Event getExternalEvent(final String player) {
        IsBannedEvent isBannedEvent = new IsBannedEvent(player);

        this.getServer().getPluginManager().callEvent(isBannedEvent);

        return isBannedEvent.getEvent();
    }

    /**
     * Build kick message.
     *
     * This method composes the message displayed to players who are kicked,
     * from an Event bean.
     *
     * @param event The {@link Event} that caused this kick.
     * @return      The message the kicked player will see.
     */
    public String buildKickMessage(final Event event) {
        String message = this.getConfig().getString("kick.message")
            .replace("<reason>",event.getReason());

        if (event.getExpires() > 0) {
            message += this.getConfig().getString("kick.expires.message")
                .replace("<expires>",new SimpleDateFormat(this.getConfig().getString("kick.expires.format")).format(event.getExpires() * 1000L));
        }

        return message.replace("&", String.valueOf(ChatColor.COLOR_CHAR));
    }

    /**
     * Build broadcast message.
     *
     * This method composes the message displayed to in-game players when a
     * player is kicked, from an {@link Event} bean.
     *
     * @param event The {@link Event} that caused this kick.
     * @return      The message in-game players will see.
     */
    public String buildBroadcastMessage(final Event event) {
        String message = this.getConfig().getString("broadcast.message")
            .replace("<player>",event.getPlayer())
            .replace("<op>", event.getOp())
            .replace("<type>", event.getType().toString().toLowerCase())
            .replace("<reason>",event.getReason());

        if (event.getExpires() > 0) {
            message += this.getConfig().getString("broadcast.expires.message")
                .replace("<expires>",new SimpleDateFormat(this.getConfig().getString("broadcast.expires.format")).format(event.getExpires() * 1000L));
        }

        return message.replace("&",String.valueOf(ChatColor.COLOR_CHAR));
    }

    /**
     * Broadcast a message.
     *
     * This will build a broadcast message with {@link #buildBroadcastMessage(it.flaten.mjolnir.beans.Event)} from
     * an {@link Event}, and send it to in-game players with the correct permissions.
     *
     * @param event The {@link Event} used to generate the message.
     */
    public void broadcast(final Event event) {
        this.getServer().broadcast(
            ChatColor.GRAY + this.buildBroadcastMessage(event),
            "mjolnir.info"
        );
    }

    /**
     * Permanently ban a player.
     *
     * Used to permanently ban a player, with no reason.
     *
     * @param player The name of the player to kick and ban.
     * @param op     The name of the player who executes the ban.
     * @return       The resulting ban {@link Event}.
     */
    public Event banPlayer(final String player,final String op) {
        return this.storage.saveEvent(player, op, Event.EventType.BAN, "", 0);
    }

    /**
     * Permanently unban a player.
     *
     * Used to permanently unban a banned player, with no reason.
     *
     * @param player The name of the player to unban.
     * @param op     The name of the player who executes the unban.
     * @return       The resulting unban {@link Event}.
     */
    public Event unbanPlayer(final String player,final String op) {
        return this.storage.saveEvent(player,op,Event.EventType.UNBAN,"",0);
    }

    /**
     * Ban a player.
     *
     * Used to permanently ban a player, with a reason.
     *
     * @param player The name of the player to kick and ban.
     * @param op     The name of the player who executes the ban.
     * @param reason The reason for this ban.
     * @return       The resulting ban {@link Event}.
     */
    public Event banPlayer(final String player,final String op,final String reason) {
        return this.storage.saveEvent(player, op, Event.EventType.BAN, reason, 0);
    }

    /**
     * Unban a player.
     *
     * Used to permanently unban a banned player, with a reason.
     *
     * @param player The name of the player to unban.
     * @param op     The name of the player who executes the unban.
     * @param reason The reason for this unban.
     * @return       The resulting unban {@link Event}.
     */
    public Event unbanPlayer(final String player,final String op,final String reason) {
        return this.storage.saveEvent(player,op,Event.EventType.UNBAN,reason,0);
    }

    /**
     * Temporarily ban a player.
     *
     * Used to temporary ban a player.
     *
     * @param player  The name of the player to ban.
     * @param op      The name of the player who executes the ban.
     * @param expires Length of ban in {@link #parseTime(String)} format.
     * @return        The resulting ban {@link Event}.
     */
    public Event tempBanPlayer(final String player,final String op,final String expires) {
        return this.storage.saveEvent(player, op, Event.EventType.BAN, "", Mjolnir.parseTime(expires));
    }

    /**
     * Temporarily unban a player.
     *
     * Used to temporarily unban a banned player, with no reason.
     *
     * @param player  The name of the player to be unbanned.
     * @param op      The name of the player who executes the unban.
     * @param expires Length of ban in {@link #parseTime(String)} format.
     * @return        The resulting unban {@link Event}.
     */
    public Event tempUnbanPlayer(final String player,final String op,final String expires) {
        return this.storage.saveEvent(player,op,Event.EventType.UNBAN,"",Mjolnir.parseTime(expires));
    }

    /**
     * Temporarily ban a player.
     *
     * Used to temporarily ban a player, with a reason.
     *
     * @param player  The name of the player to ban.
     * @param op      The name of the player who executes the ban.
     * @param reason  The reason for this ban.
     * @param expires Length of ban in {@link #parseTime(String)} format.
     * @return        The resulting ban {@link Event}.
     */
    public Event tempBanPlayer(final String player,final String op,final String reason,final String expires) {
        return this.storage.saveEvent(player, op, Event.EventType.BAN, reason, Mjolnir.parseTime(expires));
    }

    /**
     * Temporarily unban a player.
     *
     * Used to temporarily unban a player, with a reason.
     *
     * @param player  The name of the player to unban.
     * @param op      The name of the player who executes the unban.
     * @param reason  The reason for this unban.
     * @param expires Length of unban in {@link #parseTime(String)} format.
     * @return        The resulting unban {@link Event}.
     */
    public Event tempUnbanPlayer(final String player,final String op,final String reason,final String expires) {
        return this.storage.saveEvent(player,op,Event.EventType.UNBAN,reason,Mjolnir.parseTime(expires));
    }

    /**
     * Parse a time period string.
     *
     * Parses a string looking for the following time notations:
     *   1y, where 1 can be any number of years.
     *   1w, where 1 can be any number of weeks.
     *   1d, where 1 can be any number of days.
     *   1h, where 1 can be any number of hours.
     *   1m, where 1 can be any number of minutes.
     *   1s, where 1 can be any number of seconds.
     * It then returns the current UNIX time plus the specified amount of time.
     * <p>
     * If the given time string sums to 0, then 0 is returned.
     *
     * @param time A length of time in the supported format.
     * @return     A UNIX timestamp somewhere in the future, or 0.
     */
    public static int parseTime(final String time) {
        int sum = 0;
        String buffer = "";

        for (int i = 0; i < time.length(); i++) {
            final char c = time.charAt(i);

            if (Character.isDigit(c)) {
                buffer += c;
            } else {
                int amount = Integer.parseInt(buffer);

                switch (String.valueOf(c)) {
                    case "s": sum += amount; break;
                    case "m": sum += 60 * amount; break;
                    case "h": sum += 60 * 60 * amount; break;
                    case "d": sum += 60 * 60 * 24 * amount; break;
                    case "w": sum += 60 * 60 * 24 * 7 * amount; break;
                    case "y": sum += 60 * 60 * 24 * 365 * amount; break;
                }

                buffer = "";
            }
        }

        if (sum == 0) {
            return 0;
        }

        return ((int) (System.currentTimeMillis() / 1000L)) + sum;
    }

    /**
     * Pre-process an event.
     *
     * Invoked before the given {@link Event} is saved in storage. Fires off a
     * {@link NewEventEvent} to let other plugins know a new {@link Event} is about to
     * be created, and lets them cancel it.
     *
     * @param event The {@link Event} to process.
     */
    public NewEventEvent preProcess(final Event event) {
        NewEventEvent newEventEvent = new NewEventEvent(event);

        this.getServer().getPluginManager().callEvent(newEventEvent);

        return newEventEvent;
    }

    /**
     * Post-process an event.
     *
     * Invoked after the given {@link Event} has been saved in storage. Kicks
     * online players when they are banned.
     *
     * @param event The {@link Event} to process.
     */
    public void postProcess(final Event event) {
        Player player = this.getServer().getPlayerExact(event.getPlayer());

        if (player != null) {
            player.kickPlayer(this.buildKickMessage(event));
        }
    }

    /**
     * Check if a player is banned.
     *
     * Checks if a player is banned in Mjölnir, and if not also queries external
     * plugins using the {@link IsBannedEvent} event via {@link #isBannedExternally(String)}.
     *
     * @param player The name of the player to check.
     * @return       Whether or not the given player is banned.
     */
    public boolean isBanned(final String player) {
        return this.isBannedLocally(player) || this.isBannedExternally(player);
    }

    /**
     * Check if a player is banned locally.
     *
     * Fetches the given player's active {@link Event} from our database, and
     * checks its type.
     * <p>
     * If the player is banned, {@link #why(String)} can be invoked to get the
     * {@link Event} dictating so.
     *
     * @param player The name of the player to check.
     * @return       Whether or not the given player is banned locally.
     */
    @SuppressWarnings("WeakerAccess")
    public boolean isBannedLocally(final String player) {
        final Event event = this.getActiveEvent(player);

        if (event == null || event.getType() == Event.EventType.UNBAN) {
            return false;
        }

        this.why(player,event);

        return true;
    }

    /**
     * Check if a player is banned externally.
     *
     * Fires a {@link IsBannedEvent} to query external plugins for {@link Event}s,
     * then checks the {@link it.flaten.mjolnir.beans.Event.EventType}.
     * <p>
     * If the player is banned, {@link #why(String)} can be invoked to get the
     * {@link Event} dictating so.
     *
     * @param player The name of the player to check.
     * @return       Whether or not the given player is banned externally.
     */
    @SuppressWarnings("WeakerAccess")
    public boolean isBannedExternally(final String player) {
        final Event event = this.getExternalEvent(player);

        if (event == null || event.getType() == Event.EventType.UNBAN) {
            return false;
        }

        this.why(player,event);

        return true;
    }

    /**
     * Set which {@link Event} dictated the given player's fate.
     *
     * Used to let others know why {@link #isBanned(String)} returned what it did.
     * Memory is freed if the {@link Event} is null.
     *
     * @param player The name of the player this {@link Event} belongs to
     * @param event  The {@link Event}.
     */
    public void why(final String player,final Event event) {
        if (event == null) {
            this.whyMap.remove(player);
            return;
        }

        this.whyMap.put(player, event);
    }

    /**
     * Get the {@link Event} that dictated the given player's fate.
     *
     * @param player The name of the player whose {@link Event} to fetch.
     * @return       The {@link Event}.
     */
    public Event why(final String player) {
        return this.whyMap.get(player);
    }

    /**
     * Get the name history for a given UUID.
     *
     * @param uuid UUID to look up.
     * @return     A {@link Map} with timestamp as keys and names as values.
     */
    public Map<Integer, String> getNameHistory(UUID uuid) {
        if (this.nameHistoryCache.containsKey(uuid))
            return this.nameHistoryCache.get(uuid);

        HashMap<Integer, String> history = new HashMap<>();

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL("https://api.mojang.com/user/profiles/" + uuid + "/names").openConnection();
            connection.connect();

            JsonObject root = new JsonParser().parse(new InputStreamReader((InputStream) connection.getContent())).getAsJsonObject();

            for (JsonElement entry : root.getAsJsonArray()) {
                JsonObject object = entry.getAsJsonObject();

                history.put(
                    object.get("changedToAt") == null ? 0 : object.get("changedToAt").getAsInt(),
                    object.get("name").getAsString()
                );
            }
        } catch (IOException exception) {
            this.getLogger().warning("Failed to fetch name history!");

            exception.printStackTrace();
        }

        this.nameHistoryCache.put(uuid, history);

        return history;
    }

    /**
     * Checks if the given UUID had the given name at the given time.
     *
     * @param uuid      UUID to compare.
     * @param name      Name to compare.
     * @param timestamp Timestamp to compare.
     * @return          True if the values match. False otherwise.
     */
    public boolean hadNameAtTime(UUID uuid, String name, int timestamp) {
        Map<Integer, String> history = this.getNameHistory(uuid);

        for (int ts : history.keySet()) {
            if (!history.get(ts).equals(name))
                continue;

            int next = (int) (System.currentTimeMillis() / 1000L);
            for (int ts2 : history.keySet()) {
                if (ts < ts2 && ts2 < next) {
                    next = ts2;
                    break;
                }
            }

            if (ts < timestamp && timestamp < next) {
                return true;
            }
        }

        return false;
    }
}
