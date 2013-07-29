package it.flaten.mjolnir.tasks;

import it.flaten.mjolnir.Mjolnir;

public class PlayerTask implements Runnable {
    private final Mjolnir plugin;
    private final String player;

    public PlayerTask(final Mjolnir plugin,final String player) {
        this.plugin = plugin;
        this.player = player;
    }

    @Override
    public void run() {
        if (this.plugin.isBanned(this.player)) {
            this.plugin.getServer().getPlayerExact(this.player).kickPlayer(this.plugin.buildKickMessage(this.plugin.why(this.player)));
        }
    }
}
