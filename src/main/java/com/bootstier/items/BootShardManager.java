package com.bootstier.items;

import com.bootstier.BootsTierPlugin;
import com.bootstier.player.PlayerData;
import com.bootstier.utils.MessageUtils;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * Manages Boot Shards - withdrawable and consumable lives system
 */
public class BootShardManager {

    private final BootsTierPlugin plugin;

    public BootShardManager(final BootsTierPlugin plugin) {
        this.plugin = plugin;
    }

    public ItemStack createBootShard() {
        final ItemStack shard = new ItemStack(Material.ECHO_SHARD);
        final ItemMeta meta = shard.getItemMeta();
        
        if (meta != null) {
            // Premium boot shard design
            meta.setDisplayName("§6§l✦ Boot Shard ✦");
            meta.setLore(Arrays.asList(
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§7A crystallized fragment of §6ancient power",
                "§7Right-click to §aconsume §7and gain a life",
                "§7Can be §etransferred §7to other players",
                "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "§6§lLIFE ESSENCE",
                "§8\"Power flows through crystallized time\""
            ));
            
            // Add enchantment glint
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            
            meta.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(this.plugin, "bootshard"), 
                org.bukkit.persistence.PersistentDataType.BOOLEAN, 
                true
            );
            
            shard.setItemMeta(meta);
        }
        
        return shard;
    }

    public boolean isBootShard(final ItemStack item) {
        if (item == null || item.getType() != Material.ECHO_SHARD) {
            return false;
        }
        
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        return meta.getPersistentDataContainer().has(
            new org.bukkit.NamespacedKey(this.plugin, "bootshard"), 
            org.bukkit.persistence.PersistentDataType.BOOLEAN
        );
    }

    public boolean consumeBootShard(final Player player, final ItemStack item) {
        if (!this.isBootShard(item)) {
            return false;
        }
        
        final PlayerData data = this.plugin.getPlayerManager().getPlayerData(player);
        
        // Check if boots are broken
        if (data.areBootsBroken()) {
            MessageUtils.sendMessage(player, "§c✦ §lRepair your boots at the pedestal first!");
            return false;
        }
        
        // Check if at max lives
        if (data.getLives() >= this.plugin.getConfigManager().getMaxLives()) {
            MessageUtils.sendMessage(player, "§c✦ §lYou already have maximum lives!");
            return false;
        }
        
        // Consume the shard
        item.setAmount(item.getAmount() - 1);
        
        // Add life
        data.incrementLives(this.plugin.getConfigManager().getMaxLives());
        
        // Update enchantments based on new life count
        this.plugin.getBootsManager().updateBootsEnchantments(player, data.getLives());
        
        // Premium consumption effects
        player.getWorld().spawnParticle(org.bukkit.Particle.HEART, player.getLocation().add(0, 2, 0), 
            10, 0.5, 0.5, 0.5, 0.1);
        player.getWorld().spawnParticle(org.bukkit.Particle.ENCHANTED_HIT, player.getLocation().add(0, 1, 0), 
            15, 0.5, 0.5, 0.5, 0.1);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        
        MessageUtils.sendMessage(player, "§a✦ §lBoot Shard consumed! §7Lives: §e" + data.getLives() + 
            "§7/§e" + this.plugin.getConfigManager().getMaxLives());
        
        this.plugin.getPlayerManager().savePlayerData(data);
        return true;
    }

    public void withdrawBootShards(final Player player, final int amount) {
        final PlayerData data = this.plugin.getPlayerManager().getPlayerData(player);
        
        // Prevent withdrawing to 0 lives (boot breaking scenario)
        if (data.getLives() - amount < 1) {
            MessageUtils.sendMessage(player, "§c✦ §lCannot withdraw! §7You must keep at least 1 life to maintain your boots!");
            return;
        }
        
        if (data.getLives() < amount) {
            MessageUtils.sendMessage(player, "§c✦ §lInsufficient lives! §7You have §e" + data.getLives() + "§7 lives.");
            return;
        }
        
        // Check inventory space
        final int slotsNeeded = (int) Math.ceil((double) amount / 64);
        int emptySlots = 0;
        for (final ItemStack slot : player.getInventory().getStorageContents()) {
            if (slot == null || slot.getType() == Material.AIR) {
                emptySlots++;
            }
        }
        
        if (emptySlots < slotsNeeded) {
            MessageUtils.sendMessage(player, "§c✦ §lInsufficient inventory space! §7Need " + slotsNeeded + " empty slots.");
            return;
        }
        
        // Remove lives
        data.setLives(data.getLives() - amount, this.plugin.getConfigManager().getMaxLives());
        
        int remaining = amount;
        while (remaining > 0) {
            final int stackSize = Math.min(remaining, 64);
            final ItemStack shard = this.createBootShard();
            shard.setAmount(stackSize);
            player.getInventory().addItem(shard);
            remaining -= stackSize;
        }
        
        // Update enchantments based on new life count
        this.plugin.getBootsManager().updateBootsEnchantments(player, data.getLives());
        
        // Premium withdrawal effects
        player.getWorld().spawnParticle(org.bukkit.Particle.ENCHANTED_HIT, player.getLocation().add(0, 1, 0), 
            amount * 2, 0.5, 0.5, 0.5, 0.1);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.8f);
        
        MessageUtils.sendMessage(player, "§a✦ §lWithdrew §e" + amount + "§a Boot Shards! §7Remaining lives: §e" + 
            data.getLives() + "§7/§e" + this.plugin.getConfigManager().getMaxLives());
        
        this.plugin.getPlayerManager().savePlayerData(data);
    }

    public void dropBootShardOnDeath(final Player player) {
        final PlayerData data = this.plugin.getPlayerManager().getPlayerData(player);
        
        if (data.getLives() > 0) {
            final ItemStack shard = this.createBootShard();
            player.getWorld().dropItemNaturally(player.getLocation(), shard);
            
            // Premium death drop effects
            player.getWorld().spawnParticle(org.bukkit.Particle.SOUL, player.getLocation().add(0, 1, 0), 
                10, 0.5, 0.5, 0.5, 0.1);
        }
    }

    /**
     * Registers listener so players can right-click to consume Boot Shards.
     */
    public void registerShardConsumeListener() {
        plugin.getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onShardUse(org.bukkit.event.player.PlayerInteractEvent e) {
                if (e.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
                org.bukkit.entity.Player player = e.getPlayer();
                org.bukkit.inventory.ItemStack item = e.getItem();
                if (item == null || !isBootShard(item)) return;

                e.setCancelled(true);
                consumeBootShard(player, item);
            }
        }, plugin);
    }

}
