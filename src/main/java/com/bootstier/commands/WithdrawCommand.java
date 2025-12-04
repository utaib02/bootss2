package com.bootstier.commands;

import com.bootstier.BootsTierPlugin;
import com.bootstier.player.PlayerData;
import com.bootstier.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Boot shard withdrawal command handler
 */
public class WithdrawCommand implements CommandExecutor {

    private final BootsTierPlugin plugin;

    public WithdrawCommand(final BootsTierPlugin plugin) {
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

        if (data.areBootsBroken()) {
            MessageUtils.sendMessage(player, "&cYour boots are broken! You cannot withdraw boot shards.");
            return true;
        }

        int amount = 1; // Default amount

        if (args.length > 0) {
            try {
                amount = Integer.parseInt(args[0]);
            } catch (final NumberFormatException e) {
                MessageUtils.sendMessage(player, "&cInvalid amount! Please enter a number.");
                return true;
            }
        }

        if (amount <= 0) {
            MessageUtils.sendMessage(player, "&cAmount must be greater than 0!");
            return true;
        }

        if (amount > data.getLives()) {
            MessageUtils.sendMessage(player, "&cYou don't have enough lives! You have &e" + data.getLives() + "&c lives.");
            return true;
        }

        // Prevent withdrawing to 0 lives (boot breaking scenario)
        if (data.getLives() - amount < 1) {
            MessageUtils.sendMessage(player, "&c&lCannot withdraw! &7You must keep at least 1 life to maintain your boots!");
            return true;
        }

        // Check inventory space
        if (player.getInventory().firstEmpty() == -1) {
            MessageUtils.sendMessage(player, "&cYour inventory is full!");
            return true;
        }

        this.plugin.getLivesManager().withdrawBootShards(player, amount);
        return true;
    }
}
