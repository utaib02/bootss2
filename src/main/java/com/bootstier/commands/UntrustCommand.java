package com.bootstier.commands;

import com.bootstier.BootsTierPlugin;
import com.bootstier.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Untrust command handler
 */
public class UntrustCommand implements CommandExecutor {

    private final BootsTierPlugin plugin;

    public UntrustCommand(final BootsTierPlugin plugin) {
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
            MessageUtils.sendMessage(player, "&cUsage: /untrust <player>");
            return true;
        }

        final String targetName = args[0];
        final Player target = this.plugin.getServer().getPlayer(targetName);
        
        if (target == null) {
            MessageUtils.sendMessage(player, "&cPlayer not found!");
            return true;
        }

        if (target.equals(player)) {
            MessageUtils.sendMessage(player, "&cYou cannot untrust yourself!");
            return true;
        }

        if (!this.plugin.getTrustManager().isTrusted(player, target)) {
            MessageUtils.sendMessage(player, "&cYou don't trust " + target.getName() + "!");
            return true;
        }

        if (this.plugin.getTrustManager().removeTrust(player, target)) {
            MessageUtils.sendMessage(player, this.plugin.getConfigManager().getMessage("trust-removed")
                .replace("{player}", target.getName()));
            MessageUtils.sendMessage(target, this.plugin.getConfigManager().getMessage("trust-revoked")
                .replace("{player}", player.getName()));
            
            if (this.plugin.getConfigManager().isBroadcastTrust()) {
                for (final Player onlinePlayer : this.plugin.getServer().getOnlinePlayers()) {
                    if (!onlinePlayer.equals(player) && !onlinePlayer.equals(target)) {
                        MessageUtils.sendMessage(onlinePlayer, "&7" + player.getName() + " untrusted " + target.getName());
                    }
                }
            }
        }

        return true;
    }
}
