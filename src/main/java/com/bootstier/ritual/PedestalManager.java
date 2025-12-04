package com.bootstier.ritual;

import com.bootstier.BootsTierPlugin;
import com.bootstier.utils.MessageUtils;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class PedestalManager {

    private final BootsTierPlugin plugin;
    private Location pedestalLocation;
    private boolean active;
    private BukkitTask particleLoop;

    public PedestalManager(BootsTierPlugin plugin) {
        this.plugin = plugin;
        this.pedestalLocation = plugin.getConfigManager().getPedestalLocation();
        this.active = plugin.getConfigManager().isPedestalActive();
    }

    public Location getPedestalLocation() { return pedestalLocation; }
    public boolean isActive() { return active; }

    /* ---------------- COMMAND HANDLER ---------------- */

    public void handleCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cOnly players can use pedestal commands.");
            return;
        }

        switch (args.length > 0 ? args[0].toLowerCase() : "") {
            case "set" -> setPedestalLocation(p.getLocation());
            case "activate" -> setActive(true);
            case "deactivate" -> setActive(false);
            case "status" -> sendStatus(p);
            default -> p.sendMessage("§d/pedestal §fset|activate|deactivate|status");
        }
    }

    private void sendStatus(Player p) {
        p.sendMessage("§5§lPedestal Status:");
        p.sendMessage("§7Active: " + (active ? "§aYes" : "§cNo"));
        if (pedestalLocation == null)
            p.sendMessage("§7Location: §cNot Set");
        else
            p.sendMessage("§7Location: §e" + pedestalLocation.getBlockX() + ", "
                    + pedestalLocation.getBlockY() + ", " + pedestalLocation.getBlockZ());
    }

    /* ---------------- CORE LOGIC ---------------- */

    public void setPedestalLocation(Location loc) {
        this.pedestalLocation = loc.clone().getBlock().getLocation().add(0.5, 0, 0.5);
        plugin.getConfigManager().setPedestalLocation(this.pedestalLocation);
        MessageUtils.sendMessage(Bukkit.getOnlinePlayers(), "§d✦ Pedestal location set.");
    }

    public void setActive(boolean state) {
        if (pedestalLocation == null) {
            MessageUtils.broadcast("§cSet coordinates for the sacred place first!");
            return;
        }

        this.active = state;
        plugin.getConfigManager().setPedestalActive(state);

        if (state) {
            pedestalLocation.getBlock().setType(Material.BEACON);
            startParticleLoop();
            MessageUtils.broadcast("§a✦ Pedestal activated.");
        } else {
            stopParticleLoop();
            if (pedestalLocation.getBlock().getType() == Material.BEACON)
                pedestalLocation.getBlock().setType(Material.AIR);
            MessageUtils.broadcast("§c✦ Pedestal deactivated.");
        }
    }

    private void startParticleLoop() {
        stopParticleLoop();

        particleLoop = new BukkitRunnable() {
            double rotation = 0;

            @Override
            public void run() {
                if (!active || pedestalLocation == null) {
                    cancel();
                    return;
                }

                World w = pedestalLocation.getWorld();
                if (w == null) return;

                Location center = pedestalLocation.clone().add(0.5, 1.3, 0.5);

                rotation += 0.08;
                double radius = 2.2;  // reduced slightly to sit cleaner

                for (int i = 0; i < 24; i++) {
                    double angle = (i * (Math.PI * 2 / 24)) + rotation;

                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;

                    Location particleLoc = center.clone().add(x, 0, z);

                    w.spawnParticle(
                            Particle.END_ROD,
                            particleLoc,
                            1,
                            0, 0, 0,
                            0
                    );
                }
            }
        }.runTaskTimer(plugin, 0L, 3L);
    }

    private void stopParticleLoop() {
        if (particleLoop != null) particleLoop.cancel();
        particleLoop = null;
    }

    public boolean isAtPedestal(Location loc) {
        return pedestalLocation != null
                && pedestalLocation.getWorld().equals(loc.getWorld())
                && pedestalLocation.distance(loc) <= 3;
    }

    /* ---------------- REPAIR BOX USE ---------------- */

    public void handleRepairBoxUse(Player player, ItemStack item) {
        if (pedestalLocation == null) {
            player.sendMessage(ChatColor.RED + "Set coordinates for the sacred place first!");
            return;
        }

        if (!active) {
            player.sendMessage(ChatColor.RED + "Pedestal is not active!");
            return;
        }

        plugin.getRitualManager().startRitual(player);
    }
}
