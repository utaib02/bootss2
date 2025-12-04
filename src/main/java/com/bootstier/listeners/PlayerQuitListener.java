package com.bootstier.listeners;

import com.bootstier.BootsTierPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles player logout cleanup
 */
public class PlayerQuitListener implements Listener {

    private final BootsTierPlugin plugin;

    public PlayerQuitListener(final BootsTierPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        final var player = event.getPlayer();
        final var playerId = player.getUniqueId();

        this.plugin.getUnifiedDisplayManager().cleanupPlayer(playerId);
        
        this.plugin.getRitualManager().cleanupPlayer(player);
        
        // Save player data
        final var data = this.plugin.getPlayerManager().getPlayerData(player);
        this.plugin.getPlayerManager().savePlayerData(data);
    }
}
