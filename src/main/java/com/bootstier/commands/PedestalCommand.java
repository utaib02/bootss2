package com.bootstier.commands;

import com.bootstier.BootsTierPlugin;
import com.bootstier.utils.MessageUtils;
import com.bootstier.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Pedestal management command handler
 */
public class PedestalCommand implements CommandExecutor {

    private final BootsTierPlugin plugin;

    public PedestalCommand(final BootsTierPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        final Player player = (Player) sender;

        if (!player.hasPermission("boots.admin")) {
            MessageUtils.sendMessage(player, "&cYou don't have permission to use this command!");
            return true;
        }

        if (args.length == 0) {
            this.showPedestalHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "set":
                this.setPedestalLocation(player);
                break;
            case "activate":
                this.activatePedestal(player);
                break;
            case "deactivate":
                this.deactivatePedestal(player);
                break;
            case "status":
                this.showPedestalStatus(player);
                break;
            default:
                this.showPedestalHelp(player);
                break;
        }

        return true;
    }

    private void showPedestalHelp(final Player player) {
        MessageUtils.sendMessage(player, "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        MessageUtils.sendMessage(player, "§5§l✦ PEDESTAL COMMANDS ✦");
        MessageUtils.sendMessage(player, "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        MessageUtils.sendMessage(player, "§e/pedestal set §7- §fAnchor the sacred location");
        MessageUtils.sendMessage(player, "§e/pedestal activate §7- §aAwaken the pedestal's power");
        MessageUtils.sendMessage(player, "§e/pedestal deactivate §7- §cSeal the pedestal's energy");
        MessageUtils.sendMessage(player, "§e/pedestal status §7- §bView current state");
        MessageUtils.sendMessage(player, "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }

    private void setPedestalLocation(final Player player) {
        this.plugin.getPedestalManager().setPedestalLocation(player.getLocation());
        
        // Premium effects
        player.getWorld().spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, player.getLocation(), 20, 1, 1, 1, 0.1);
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.2f);
        
        MessageUtils.sendMessage(player, "§5✦ §lPedestal anchored! §7The sacred ground has been marked at your location.");
    }

    private void activatePedestal(final Player player) {
        this.plugin.getPedestalManager().setActive(true);
        MessageUtils.sendMessage(player, "§a✦ §lPedestal awakened! §7The ancient power flows once more.");
        
        // Premium broadcast to all players
        for (final Player onlinePlayer : this.plugin.getServer().getOnlinePlayers()) {
            if (!onlinePlayer.equals(player)) {
                MessageUtils.sendMessage(onlinePlayer, "§a§l⚡ The Pedestal of Restoration awakens! §7Rituals may now commence!");
            }
        }
    }

    private void deactivatePedestal(final Player player) {
        this.plugin.getPedestalManager().setActive(false);
        MessageUtils.sendMessage(player, "§c✦ §lPedestal sealed! §7The ancient power slumbers once more.");
        
        // Premium broadcast to all players
        for (final Player onlinePlayer : this.plugin.getServer().getOnlinePlayers()) {
            if (!onlinePlayer.equals(player)) {
                MessageUtils.sendMessage(onlinePlayer, "§c§l⚡ The Pedestal of Restoration slumbers! §7No more rituals may be performed.");
            }
        }
    }

    private void showPedestalStatus(final Player player) {
        final boolean active = this.plugin.getPedestalManager().isActive();
        final org.bukkit.Location location = this.plugin.getPedestalManager().getPedestalLocation();
        
        MessageUtils.sendMessage(player, "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        MessageUtils.sendMessage(player, "§5§l✦ PEDESTAL STATUS ✦");
        MessageUtils.sendMessage(player, "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        MessageUtils.sendMessage(player, "§7State: " + (active ? "§a§lAWAKE §7⚡" : "§c§lSLUMBERING §7💤"));
        
        if (location != null) {
            MessageUtils.sendMessage(player, "§7Anchor: §e" + location.getWorld().getName() + 
                " §7(§e" + location.getBlockX() + "§7, §e" + location.getBlockY() + "§7, §e" + location.getBlockZ() + "§7)");
        } else {
            MessageUtils.sendMessage(player, "§7Anchor: §c§lUNSET");
        }
        
        MessageUtils.sendMessage(player, "§7Rituals: " + (active ? "§a§lAVAILABLE" : "§c§lFORBIDDEN"));
        MessageUtils.sendMessage(player, "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }
}
