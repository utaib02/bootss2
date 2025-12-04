package com.bootstier.listeners;

import com.bootstier.BootsTierPlugin;
import com.bootstier.player.PlayerData;
import com.bootstier.utils.MessageUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import com.bootstier.utils.ItemUtils;

/**
 * Handles player join events
 */
public class PlayerJoinListener implements Listener {

    private final BootsTierPlugin plugin;

    public PlayerJoinListener(final BootsTierPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final org.bukkit.entity.Player player = event.getPlayer();
        final PlayerData data = this.plugin.getPlayerManager().getPlayerData(player);
        
        // Remove any existing boots that aren't ability boots
        final ItemStack existingBoots = player.getInventory().getBoots();
        if (existingBoots != null && existingBoots.getType() == Material.DIAMOND_BOOTS) {
            if (!ItemUtils.hasCustomLore(existingBoots, "Tier:")) {
                player.getInventory().setBoots(null);
            }
        }
        
        // Update login time
        data.setLastLogin(System.currentTimeMillis());
        
        // Give boots if first join and config allows
        if (data.isFirstJoin() && this.plugin.getConfigManager().isGiveBootsOnJoin()) {
            this.plugin.getPlayerManager().createNewPlayer(player);
            
            // Give boots item
            this.plugin.getBootsManager().giveBoots(player, data.getBootsData().getBootType());
            
            MessageUtils.sendMessage(player, "&a&lWelcome! You have been given " + 
                data.getBootsData().getBootType().getColoredName() + "&a!");
            MessageUtils.sendMessage(player, "&7Use &e/boots info&7 to learn about your boots!");
            
            data.setFirstJoin(false);
        } else if (data.getBootsData() != null) {
            // Update existing boots item
            this.plugin.getBootsManager().giveBoots(player, data.getBootsData().getBootType());
        } else if (data.getBootsData() == null) {
            // Player has no boots data, create new
            this.plugin.getPlayerManager().createNewPlayer(player);
            this.plugin.getBootsManager().giveBoots(player, data.getBootsData().getBootType());
            
            MessageUtils.sendMessage(player, "&a&lYou have been given " + 
                data.getBootsData().getBootType().getColoredName() + "&a!");
        }
        
        this.plugin.getPlayerManager().savePlayerData(data);
    }
}
