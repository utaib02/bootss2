package com.bootstier.lives;

import com.bootstier.BootsTierPlugin;
import com.bootstier.items.BootShardManager;
import com.bootstier.player.PlayerData;
import com.bootstier.utils.ItemUtils;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * Manages lives and boot shard system
 */
public class LivesManager {

    private final BootsTierPlugin plugin;
    @Getter
    private final BootShardManager bootShardManager;

    public LivesManager(final BootsTierPlugin plugin) {
        this.plugin = plugin;
        this.bootShardManager = new BootShardManager(plugin);
    }

    /* -----------------------------------------------
       PLAYER DEATH / KILL HANDLING
    ----------------------------------------------- */

    public void handlePlayerDeath(final Player player) {
        final PlayerData data = this.plugin.getPlayerManager().getPlayerData(player);

        if (data.hasLives()) {
            // Drop boot shard (using BootShardManager)
            this.bootShardManager.dropBootShardOnDeath(player);

            // Decrement lives
            data.decrementLives();

            // Handle boots breaking
            if (!data.hasLives()) {
                data.breakBoots();
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Your boots have broken! "
                        + ChatColor.GRAY + "Find a Repair Box to fix them.");
            }

            // Tier downgrade if Tier II
            if (data.getBootsData().getTier().getLevel() == 2) {
                this.plugin.getBootsManager().downgradeTier(player);
                dropTierBox(player);
            }

            // Update boots enchantments + save
            this.plugin.getBootsManager().updateBootsEnchantments(player, data.getLives());
            this.plugin.getPlayerManager().savePlayerData(data);
        }
    }

    public void handlePlayerKill(final Player killer, final Player victim) {
        final PlayerData killerData = this.plugin.getPlayerManager().getPlayerData(killer);
        final PlayerData victimData = this.plugin.getPlayerManager().getPlayerData(victim);

        // Only gain lives if victim had any
        if (victimData.hasLives()) {
            killerData.incrementLives(this.plugin.getConfigManager().getMaxLives());
            playerMsg(killer, ChatColor.GREEN + "" + ChatColor.BOLD + "You gained a life! " +
                    ChatColor.YELLOW + "Total: " + killerData.getLives());
            this.plugin.getPlayerManager().savePlayerData(killerData);
        }
    }

    /* -----------------------------------------------
       ITEM CREATION & DROPS
    ----------------------------------------------- */

    private void dropTierBox(final Player player) {
        player.getWorld().dropItemNaturally(player.getLocation(), this.plugin.getCustomItemManager().createTierBoxItem());
    }

    public ItemStack createBootShardItem() {
        return this.bootShardManager.createBootShard();
    }

    /* -----------------------------------------------
       BOOT SHARD CONSUMPTION (Legacy - BootShardManager handles this now)
    ----------------------------------------------- */

    public void consumeBootShard(final Player player, final ItemStack item) {
        this.bootShardManager.consumeBootShard(player, item);
    }

    /* -----------------------------------------------
       BOOT SHARD WITHDRAWAL
    ----------------------------------------------- */

    public void withdrawBootShards(final Player player, final int amount) {
        this.bootShardManager.withdrawBootShards(player, amount);
    }

    /* -----------------------------------------------
       UTIL
    ----------------------------------------------- */

    private void playerMsg(Player p, String msg) {
        p.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
    }
}
