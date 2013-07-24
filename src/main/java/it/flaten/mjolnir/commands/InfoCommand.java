package it.flaten.mjolnir.commands;

import it.flaten.mjolnir.Mjolnir;
import it.flaten.mjolnir.beans.Event;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.text.SimpleDateFormat;
import java.util.List;

public class InfoCommand implements CommandExecutor {
    private Mjolnir plugin;

    public InfoCommand(Mjolnir plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
        if (args.length == 0) {
            return false;
        }

        OfflinePlayer player = this.plugin.getServer().getOfflinePlayer(args[0]);

        if (player == null) {
            sender.sendMessage(ChatColor.RED + "Unknown player.");
            return true;
        }

        List<Event> events = this.plugin.getEventHistory(player.getName());

        if (events == null || events.size() == 0) {
            sender.sendMessage(ChatColor.GRAY + "No history.");
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + " ==== History for " + player.getName() + " ====");

        int now = (int) (System.currentTimeMillis() / 1000L);

        SimpleDateFormat dateFormat = new SimpleDateFormat(
            this.plugin.getConfig().getString("info.timestamp.format")
        );

        for (Event event : events) {
            sender.sendMessage(
                ChatColor.GRAY +
                "[" + dateFormat.format(event.getTime() * 1000L) + "] " +
                this.plugin.buildBroadcastMessage(event) +
                (event.getExpires() > 0 && event.getExpires() <= now ? ChatColor.RED + " (expired)" : "")
            );
        }

        return true;
    }
}
