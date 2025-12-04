package com.bootstier.listeners;

import com.bootstier.BootsTierPlugin;
import com.bootstier.utils.ItemUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

/**
 * Prevents boots from being removed or moved
 */
public class InventoryListener implements Listener {

    private final BootsTierPlugin plugin;

    public InventoryListener(final BootsTierPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        final Player player = (Player) event.getWhoClicked();
        
        final ItemStack boots = player.getInventory().getBoots();
        if (boots != null && ItemUtils.isValidBootsItem(boots)) {
            // Block ANY interaction with boots slot
            if (event.getSlot() == 39 || event.getSlotType() == InventoryType.SlotType.ARMOR) {
                event.setCancelled(true);
                return;
            }
            
            // Block dragging boots anywhere
            final ItemStack cursor = event.getCursor();
            if (cursor != null && ItemUtils.isValidBootsItem(cursor)) {
                event.setCancelled(true);
                return;
            }
            
            // Block shift-click movement
            if (event.isShiftClick() && ItemUtils.isValidBootsItem(event.getCurrentItem())) {
                event.setCancelled(true);
                return;
            }
        }

        // Prevent clicking on boots item or swapping it
        final ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem != null && ItemUtils.isValidBootsItem(clickedItem)) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onInventoryDrag(final InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        for (ItemStack item : event.getNewItems().values()) {
            if (item != null && ItemUtils.isValidBootsItem(item)) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
