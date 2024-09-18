package org.xenocraft.collectopaedia.listener;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.xenocraft.collectopaedia.Collectopaedia;

public class PlayerChangeWorldListener implements Listener {
    private final Collectopaedia collectopaedia;

    public PlayerChangeWorldListener(Collectopaedia collectopaedia) {
        this.collectopaedia = collectopaedia;
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // Check if the teleportation involves a change in the player's world
        var fromWorld = event.getFrom().getWorld();
        var toWorld = event.getTo() != null ? event.getTo().getWorld() : null;

        if (toWorld != null && fromWorld != null && !fromWorld.equals(toWorld)) {
            Bukkit.getScheduler().runTaskAsynchronously(collectopaedia, () -> {
                // Synchronize on the collectopaedia instance to ensure thread safety
                synchronized (collectopaedia) {
                    collectopaedia.addArea(event.getPlayer(), toWorld.getName());
                }
            });
        }
    }
}
