package org.xenocraft.collectopaedia.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.xenocraft.collectopaedia.Collectopaedia;

public class PlayerJoinLeaveListener implements Listener {
    private final Collectopaedia collectopaedia;

    public PlayerJoinLeaveListener(Collectopaedia collectopaedia) {
        this.collectopaedia = collectopaedia;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        Bukkit.getScheduler().runTaskAsynchronously(collectopaedia, () -> {
            if (!collectopaedia.playerFileExists(p)) {
                collectopaedia.createPlayerFile(p);
                collectopaedia.updatePlayerFile(p);
            } else {
                collectopaedia.updatePlayerFile(p);
            }
        });
    }
}
