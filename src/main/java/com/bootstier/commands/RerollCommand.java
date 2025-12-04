package com.bootstier.commands;

import com.bootstier.BootsTierPlugin;
import com.bootstier.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Admin command for force-rerolling player boots
 */
public class RerollCommand implements CommandExecutor {

    private final BootsTierPlugin plugin;

    public RerollCommand(final BootsTierPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        final Player executor = (Player) sender;

        if (!executor.hasPermission("boot.admin.reroll")) {
            MessageUtils.sendMessage(executor, "§c✦ §lInsufficient permissions!");
            return true;
        }

        Player target = executor;
        if (args.length > 0) {
            target = this.plugin.getServer().getPlayer(args[0]);
            if (target == null) {
                MessageUtils.sendMessage(executor, "§c✦ §lPlayer not found: §7" + args[0]);
                return true;
            }
        }

        // Force reroll (preserves Tier 2)
        this.plugin.getPlayerManager().rerollPlayerBoots(target);
        
        // Update boots item
        final com.bootstier.player.PlayerData data = this.plugin.getPlayerManager().getPlayerData(target);
        this.plugin.getBootsManager().giveBoots(target, data.getBootsData().getBootType());

        // Premium reroll effects
        target.getWorld().spawnParticle(org.bukkit.Particle.ENCHANTED_HIT, target.getLocation(), 
            30, 1, 1, 1, 0.1);
        target.getWorld().spawnParticle(org.bukkit.Particle.FIREWORK, target.getLocation(), 
            15, 0.5, 0.5, 0.5, 0.1);
        target.playSound(target.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.8f);

        // Messages
        if (target.equals(executor)) {
            MessageUtils.sendMessage(executor, "§a✦ §lForce rerolled your boots to " + 
                data.getBootsData().getBootType().getStyledName() + "§a!");
        } else {
            MessageUtils.sendMessage(executor, "§a✦ §lForce rerolled §e" + target.getName() + "§a's boots to " + 
                data.getBootsData().getBootType().getStyledName() + "§a!");
            MessageUtils.sendMessage(target, "§a✦ §lYour boots were rerolled to " + 
                data.getBootsData().getBootType().getStyledName() + "§a by an administrator!");
        }

        return true;
    }
}
