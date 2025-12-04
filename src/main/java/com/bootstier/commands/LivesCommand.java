package com.bootstier.commands;

import com.bootstier.BootsTierPlugin;
import com.bootstier.player.PlayerData;
import com.bootstier.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Lives checking command handler
 */
public class LivesCommand implements CommandExecutor {

    private final BootsTierPlugin plugin;

    public LivesCommand(final BootsTierPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        final Player player = (Player) sender;
        final PlayerData data = this.plugin.getPlayerManager().getPlayerData(player);
        
        final int lives = data.getLives();
        final int maxLives = this.plugin.getConfigManager().getMaxLives();
        final boolean broken = data.areBootsBroken();
        
        MessageUtils.sendMessage(player, "&6&l=== Your Lives ===");
        MessageUtils.sendMessage(player, "&7Current Lives: &e" + lives + "&7/&e" + maxLives);
        
        if (broken) {
            MessageUtils.sendMessage(player, "&c&lYour boots are BROKEN!");
            MessageUtils.sendMessage(player, "&7Use a Repair Box to fix them when the pedestal is active.");
        } else {
            MessageUtils.sendMessage(player, "&a&lYour boots are active!");
            
            // Show lives-based enchantment status
            if (lives >= 4) {
                MessageUtils.sendMessage(player, "&7All enchantments active");
            } else if (lives >= 3) {
                MessageUtils.sendMessage(player, "&7Missing: Soul Speed");
            } else if (lives >= 2) {
                MessageUtils.sendMessage(player, "&7Missing: Soul Speed, Depth Strider");
            } else if (lives >= 1) {
                MessageUtils.sendMessage(player, "&7Missing: Soul Speed, Depth Strider, Feather Falling");
            }
        }
        
        return true;
    }
}
