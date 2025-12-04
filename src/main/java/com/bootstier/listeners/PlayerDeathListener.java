package com.bootstier.listeners;

import com.bootstier.BootsTierPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Handles player death events
 */
public class PlayerDeathListener implements Listener {

    private final BootsTierPlugin plugin;

    public PlayerDeathListener(final BootsTierPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(final PlayerDeathEvent event) {
        final Player player = event.getEntity();
        final Player killer = player.getKiller();
        
        // Handle death (lives, boot shards, etc.)
        this.plugin.getLivesManager().handlePlayerDeath(player);
        
        // Handle killer gaining lives
        if (killer != null && killer instanceof Player) {
            this.plugin.getLivesManager().handlePlayerKill(killer, player);
        }
        
        // Prevent boots from dropping
        event.getDrops().removeIf(item -> 
            item.getType() == org.bukkit.Material.DIAMOND_BOOTS && 
            this.plugin.getBootsManager().hasValidBoots(player));
    }
}
