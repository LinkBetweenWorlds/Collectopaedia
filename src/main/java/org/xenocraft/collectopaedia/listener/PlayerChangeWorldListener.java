package org.xenocraft.collectopaedia.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.xenocraft.collectopaedia.Collectopaedia;

import java.util.Objects;

public class PlayerChangeWorldListener implements Listener {
    private final Collectopaedia collectopaedia;

    public PlayerChangeWorldListener(Collectopaedia collectopaedia){
        this.collectopaedia = collectopaedia;
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // Check if the teleportation involves a change in the player's world
        if (!event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            System.out.println(Objects.requireNonNull(event.getTo().getWorld()).getName());
            collectopaedia.addArea(event.getPlayer(), Objects.requireNonNull(event.getTo().getWorld()).getName());
        }
    }
}
