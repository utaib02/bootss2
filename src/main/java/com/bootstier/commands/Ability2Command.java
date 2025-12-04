package com.bootstier.commands;

import com.bootstier.BootsTierPlugin;
import com.bootstier.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command handler for /ability2
 */
public class Ability2Command implements CommandExecutor {

    private final BootsTierPlugin plugin;

    public Ability2Command(final BootsTierPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        final Player player = (Player) sender;
        
        if (!this.plugin.getAbilityManager().activateAbility(player, 2)) {
            MessageUtils.sendMessage(player, "&cAbility activation failed!");
        }
        
        return true;
    }
}
