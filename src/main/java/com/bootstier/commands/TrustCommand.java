package com.bootstier.commands;

import com.bootstier.BootsTierPlugin;
import com.bootstier.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Trust/untrust command handler
 */
public class TrustCommand implements CommandExecutor {

    private final BootsTierPlugin plugin;

    public TrustCommand(final BootsTierPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        final Player player = (Player) sender;

        if (args.length == 0) {
            this.showTrustHelp(player);
            return true;
        }

        final String targetName = args[0];
        final Player target = this.plugin.getServer().getPlayer(targetName);
        
        if (target == null) {
            MessageUtils.sendMessage(player, "&cPlayer not found!");
            return true;
        }

        if (target.equals(player)) {
            MessageUtils.sendMessage(player, "&cYou cannot trust/untrust yourself!");
            return true;
        }

        final String commandName = command.getName().toLowerCase();
        
        if ("trust".equals(commandName)) {
            this.trustPlayer(player, target);
        } else if ("untrust".equals(commandName)) {
            this.untrustPlayer(player, target);
        }

        return true;
    }

    private void showTrustHelp(final Player player) {
        MessageUtils.sendMessage(player, "&6&l=== Trust Commands ===");
        MessageUtils.sendMessage(player, "&e/trust <player> &7- Trust a player");
        MessageUtils.sendMessage(player, "&e/untrust <player> &7- Untrust a player");
        MessageUtils.sendMessage(player, "&7Trusted players are not affected by your boot abilities!");
        
        final int trustedCount = this.plugin.getTrustManager().getTrustedCount(player);
        final int maxTrusted = this.plugin.getConfigManager().getMaxTrusted();
        MessageUtils.sendMessage(player, "&7Trusted players: &e" + trustedCount + "&7/&e" + maxTrusted);
    }

    private void trustPlayer(final Player player, final Player target) {
        if (this.plugin.getTrustManager().isTrusted(player, target)) {
            MessageUtils.sendMessage(player, "&cYou already trust " + target.getName() + "!");
            return;
        }

        if (!this.plugin.getTrustManager().canTrustMore(player)) {
            MessageUtils.sendMessage(player, "&cYou have reached the maximum number of trusted players!");
            return;
        }

        if (this.plugin.getTrustManager().addTrust(player, target)) {
            MessageUtils.sendMessage(player, "&aYou now trust &e" + target.getName() + "&a!");
            

        }
    }

    private void untrustPlayer(final Player player, final Player target) {
        if (!this.plugin.getTrustManager().isTrusted(player, target)) {
            MessageUtils.sendMessage(player, "&cYou don't trust " + target.getName() + "!");
            return;
        }

        if (this.plugin.getTrustManager().removeTrust(player, target)) {
            MessageUtils.sendMessage(player, "&cYou no longer trust &e" + target.getName() + "&c!");

        }
    }
}
