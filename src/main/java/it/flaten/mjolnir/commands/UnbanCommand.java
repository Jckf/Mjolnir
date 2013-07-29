package it.flaten.mjolnir.commands;

import it.flaten.mjolnir.Mjolnir;
import it.flaten.mjolnir.beans.Event;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class UnbanCommand implements CommandExecutor {
    private final Mjolnir plugin;

    public UnbanCommand(final Mjolnir plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender,final Command command,final String label,final String[] args) {
        if (args.length == 0) {
            return false;
        }

        final OfflinePlayer player = this.plugin.getServer().getOfflinePlayer(args[0]);

        if (player == null) {
            sender.sendMessage(ChatColor.RED + "Unknown player.");
            return true;
        }

        if (!this.plugin.isBanned(player.getName())) {
            sender.sendMessage(ChatColor.RED + player.getName() + " is not banned. Cannot unban.");
            return true;
        }

        // Unban, no reason.
        if (args.length == 1) {
            Event event = this.plugin.unbanPlayer(
                player.getName(),
                sender.getName()
            );

            if (event != null) {
                this.plugin.broadcast(event);
            }

            return true;
        }

        // Unban.
        Event event = this.plugin.unbanPlayer(
            player.getName(),
            sender.getName(),
            StringUtils.join(args," ",1,args.length)
        );

        if (event != null) {
            this.plugin.broadcast(event);
        }

        return true;
    }
}
