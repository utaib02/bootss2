package com.bootstier.commands;

import com.bootstier.BootsTierPlugin;
import com.bootstier.boots.BootType;
import com.bootstier.boots.BootsTier;
import com.bootstier.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Admin command for quickly setting player boot types and tiers
 */
public class BootSetCommand implements CommandExecutor {

    private final BootsTierPlugin plugin;

    public BootSetCommand(final BootsTierPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (!sender.hasPermission("boot.admin.set")) {
            MessageUtils.sendMessage((Player) sender, "§c✦ §lInsufficient permissions!");
            return true;
        }

        if (args.length != 3) {
            MessageUtils.sendMessage((Player) sender, "§c✦ §lUsage: §7/bootset <player> <type> <tier>");
            this.showAvailableTypes(sender);
            return true;
        }

        final String targetName = args[0];
        final String typeName = args[1];
        final String tierName = args[2];

        final Player target = this.plugin.getServer().getPlayer(targetName);
        if (target == null) {
            MessageUtils.sendMessage((Player) sender, "§c✦ §lPlayer not found: §7" + targetName);
            return true;
        }

        // Parse boot type
        BootType bootType;
        try {
            bootType = BootType.valueOf(typeName.toUpperCase());
        } catch (final IllegalArgumentException e) {
            MessageUtils.sendMessage((Player) sender, "§c✦ §lInvalid boot type: §7" + typeName);
            this.showAvailableTypes(sender);
            return true;
        }

        // Parse tier
        BootsTier tier;
        try {
            final int tierLevel = Integer.parseInt(tierName);
            tier = BootsTier.fromLevel(tierLevel);
        } catch (final NumberFormatException e) {
            MessageUtils.sendMessage((Player) sender, "§c✦ §lInvalid tier: §7" + tierName + " §7(use 1 or 2)");
            return true;
        }

        // Apply boots
        this.plugin.getBootsManager().giveBoots(target, bootType);
        
        // Set tier if Tier 2
        if (tier == BootsTier.TIER_2) {
            this.plugin.getBootsManager().upgradeTier(target);
        }

        this.plugin.getUnifiedDisplayManager().refreshPlayerDisplays(target);

        // Premium effects
        target.getWorld().spawnParticle(org.bukkit.Particle.FIREWORK, target.getLocation(), 
            20, 1, 1, 1, 0.1);
        target.playSound(target.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);

        // Messages
        MessageUtils.sendMessage((Player) sender, "§a✦ §lGranted §e" + target.getName() + " §a" + 
            bootType.getStyledName() + " " + tier.getRoman() + "§a!");
        MessageUtils.sendMessage(target, "§a✦ §lYou received " + bootType.getStyledName() + " " + 
            tier.getRoman() + "§a from an administrator!");

        return true;
    }

    private void showAvailableTypes(final CommandSender sender) {
        MessageUtils.sendMessage((Player) sender, "§6✦ §lAvailable Boot Types:");
        final StringBuilder types = new StringBuilder("§7");
        for (final BootType type : BootType.values()) {
            types.append(type.name().toLowerCase()).append(", ");
        }
        // Remove last comma and space
        if (types.length() > 2) {
            types.setLength(types.length() - 2);
        }
        MessageUtils.sendMessage((Player) sender, types.toString());
        MessageUtils.sendMessage((Player) sender, "§6✦ §lAvailable Tiers: §71, 2");
    }
}
