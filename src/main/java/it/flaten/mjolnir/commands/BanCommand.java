package it.flaten.mjolnir.commands;

import it.flaten.mjolnir.Mjolnir;
import it.flaten.mjolnir.beans.Event;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class BanCommand implements CommandExecutor {
	private Mjolnir plugin;

	public BanCommand(Mjolnir plugin) {
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

		if (this.plugin.isBanned(player.getName())) {
			sender.sendMessage(ChatColor.RED + player.getName() + " is already banned.");
			return true;
		}

		// Ban, no reason.
		if (args.length == 1) {
			Event event = this.plugin.banPlayer(
				player.getName(),
				sender.getName()
			);

			this.plugin.broadcast(event);

			return true;
		}

		// Ban.
		Event event = this.plugin.banPlayer(
			player.getName(),
			sender.getName(),
			StringUtils.join(args," ",1,args.length)
		);

		this.plugin.broadcast(event);

		return true;
	}
}