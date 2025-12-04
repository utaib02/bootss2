package com.bootstier.listeners;

import com.bootstier.BootsTierPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Handles block events for ritual mechanics
 */
public class BlockListener implements Listener {

    private final BootsTierPlugin plugin;

    public BlockListener(final BootsTierPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(final BlockBreakEvent event) {
        // Detect pedestal beacon break attempt
        if (event.getBlock().getType() != Material.BEACON) return;

        final Location broken = event.getBlock().getLocation();
        final Location pedestal = plugin.getPedestalManager().getPedestalLocation();

        if (pedestal == null) return;
        if (!pedestal.getWorld().equals(broken.getWorld())) return;
        if (pedestal.distance(broken) > 2.5) return;

        // Cancel break and apply beacon "damage"
        event.setCancelled(true);
        final Player attacker = event.getPlayer();
        plugin.getRitualManager().damageBeaconAt(broken, attacker);
    }

    @EventHandler
    public void onBlockPlace(final BlockPlaceEvent event) {
        // Prevent placing special custom items as blocks
        if (plugin.getCustomItemManager().isRerollItem(event.getItemInHand())
                || plugin.getCustomItemManager().isTierBoxItem(event.getItemInHand())
                || plugin.getCustomItemManager().isRepairBoxItem(event.getItemInHand())
                || plugin.getCustomItemManager().isBootShardItem(event.getItemInHand())) {
            event.setCancelled(true);
        }
    }
}
