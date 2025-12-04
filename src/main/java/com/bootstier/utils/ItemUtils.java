package com.bootstier.utils;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Utility class for item operations
 */
public class ItemUtils {

    public static boolean hasCustomName(final ItemStack item, final String name) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        final ItemMeta meta = item.getItemMeta();
        return meta.hasDisplayName() && name.equals(meta.getDisplayName());
    }

    public static boolean hasCustomLore(final ItemStack item, final String loreText) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        final ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) {
            return false;
        }
        
        for (final String line : meta.getLore()) {
            if (line.contains(loreText)) {
                return true;
            }
        }
        
        return false;
    }

    public static void addGlow(final ItemMeta meta) {
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    }

    public static boolean isValidBootsItem(final ItemStack item) {
        return item != null && 
               item.getType() == org.bukkit.Material.DIAMOND_BOOTS &&
               hasCustomLore(item, "Tier:");
    }
}
