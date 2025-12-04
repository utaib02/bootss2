package com.bootstier.boots.abilities.impl;

import com.bootstier.BootsTierPlugin;
import com.bootstier.boots.abilities.BootAbility;
import com.bootstier.utils.LocationUtils;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class AstralAbility implements BootAbility {

    private final BootsTierPlugin plugin;
    private final Random random = new Random();

    // Player → Marker ArmorStand
    private final Map<UUID, ArmorStand> rewindMarkers = new HashMap<>();

    // COOLDOWN MAPS (NOW REAL — COMPATIBLE WITH ACTION BAR)
    private final Map<UUID, Long> tier1Cooldown = new HashMap<>();
    private final Map<UUID, Long> tier2Cooldown = new HashMap<>();

    // REAL COOLDOWNS (MATCH ACTIONBAR MANAGER)
    private static final long TIER1_CD = 90_000L;  // 1m30s
    private static final long TIER2_CD = 120_000L; // 2m

    public AstralAbility(BootsTierPlugin plugin) {
        this.plugin = plugin;
    }

    /* ==========================================================
       COOLDOWN HELPERS + DRAGON EGG MULTIPLIER
       ========================================================== */

    private boolean hasDragonEgg(Player p) {
        for (ItemStack item : p.getInventory().getContents()) {
            if (item != null && item.getType() == Material.DRAGON_EGG)
                return true;
        }
        return false;
    }

    private boolean isOnCooldown(Map<UUID, Long> map, UUID id) {
        return map.getOrDefault(id, 0L) > System.currentTimeMillis();
    }

    private long getRemaining(Map<UUID, Long> map, UUID id) {
        return Math.max(0, map.getOrDefault(id, 0L) - System.currentTimeMillis());
    }

    private void setCooldown(Map<UUID, Long> map, Player p, long baseCD) {
        boolean egg = hasDragonEgg(p);
        long effective = egg ? baseCD / 2 : baseCD;
        map.put(p.getUniqueId(), System.currentTimeMillis() + effective);
    }

    /* ==========================================================
       TIER 1 — REWIND
       ========================================================== */

    @Override
    public boolean executeTier1(Player player) {

        UUID id = player.getUniqueId();

        // SECOND ACTIVATION — TELEPORT BACK
        if (player.hasMetadata("astral_rewind_loc")) {

            Location rewindLoc = (Location) player.getMetadata("astral_rewind_loc").get(0).value();
            Location current = player.getLocation();

            player.teleport(rewindLoc);

            player.removeMetadata("astral_rewind_loc", plugin);

            // Remove marker
            ArmorStand marker = rewindMarkers.remove(id);
            if (marker != null && !marker.isDead()) marker.remove();

            spawnTeleportTrail(current, rewindLoc);

            player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_ATTACK_IMPACT, 1f, 1.2f);

            // REAL COOLDOWN
            setCooldown(tier1Cooldown, player, TIER1_CD);

            return true;
        }

        // FIRST ACTIVATION — Place Marker

        if (isOnCooldown(tier1Cooldown, id)) {
            long left = getRemaining(tier1Cooldown, id) / 1000;
            player.sendMessage("§cRewind on cooldown: §e" + left + "s");
            return false;
        }

        Location loc = player.getLocation().clone();
        player.setMetadata("astral_rewind_loc", new FixedMetadataValue(plugin, loc));

        // MARKER (visual)
        ArmorStand marker = loc.getWorld().spawn(loc, ArmorStand.class, a -> {
            a.setVisible(false);
            a.setGravity(false);
            a.setSmall(true);
            a.setInvulnerable(true);
            a.setCustomName("§5✦ Rewind Point");
            a.setCustomNameVisible(true);
        });
        rewindMarkers.put(id, marker);

        // Auto-remove after 20 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.hasMetadata("astral_rewind_loc")) {
                    player.removeMetadata("astral_rewind_loc", plugin);

                    ArmorStand m = rewindMarkers.remove(id);
                    if (m != null && !m.isDead()) m.remove();
                }
            }
        }.runTaskLater(plugin, 20 * 20);

        // Particle animation
        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (!player.hasMetadata("astral_rewind_loc") || marker.isDead()) {
                    cancel();
                    return;
                }

                Location ml = marker.getLocation().clone().add(0, 1, 0);

                ml.getWorld().spawnParticle(Particle.END_ROD, ml, 4, 0.2, 0.2, 0.2, 0.01);
                ml.getWorld().spawnParticle(Particle.PORTAL, ml.clone().add(0, -0.5, 0), 8, 0.4, 0.4, 0.4, 0.05);

                double angle = t * 0.25;
                double x = Math.cos(angle) * 0.7;
                double z = Math.sin(angle) * 0.7;

                ml.getWorld().spawnParticle(Particle.WITCH, ml.clone().add(x, 0.2, z), 1, 0, 0, 0, 0);

                t++;
            }
        }.runTaskTimer(plugin, 0, 4);

        return false; // IMPORTANT → FIRST CLICK DOES NOT TRIGGER COOLDOWN
    }

    /* ==========================================================
       TIER 2 — ASTRAL DISABLE (15s)
       ========================================================== */

    @Override
    public boolean executeTier2(Player player) {

        UUID id = player.getUniqueId();

        if (isOnCooldown(tier2Cooldown, id)) {
            long left = getRemaining(tier2Cooldown, id) / 1000;
            player.sendMessage("§cDisable on cooldown: §e" + left + "s");
            return false;
        }

        // Start REAL cooldown
        setCooldown(tier2Cooldown, player, TIER2_CD);

        player.setMetadata("astral_disable_active", new FixedMetadataValue(plugin, true));

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {

                if (!player.isOnline()) {
                    cleanupDisable(player);
                    cancel();
                    return;
                }

                if (!player.hasMetadata("astral_disable_active")) {
                    cleanupDisable(player);
                    cancel();
                    return;
                }

                if (ticks >= 20 * 15) {
                    cleanupDisable(player);
                    cancel();
                    return;
                }

                // Disable ability effect logic
                List<Player> near = LocationUtils.getPlayersInRadius(player.getLocation(), 5);
                for (Player p : near) {
                    if (!plugin.getTrustManager().isTrusted(player, p)) {
                        p.setMetadata("boots_disabled", new FixedMetadataValue(plugin, true));

                        Location tLoc = p.getLocation().clone().add(0, 1, 0);
                        tLoc.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, tLoc, 8, 0.4, 0.4, 0.4, 0.01);
                        tLoc.getWorld().spawnParticle(Particle.CRIT, tLoc, 5, 0.3, 0.3, 0.3, 0.02);
                    }
                }

                // Caster ring
                Location c = player.getLocation().clone().add(0, 1.1, 0);

                double radius = 5;
                for (double a = 0; a < Math.PI * 2; a += Math.PI / 12) {
                    double x = Math.cos(a) * radius;
                    double z = Math.sin(a) * radius;
                    c.getWorld().spawnParticle(Particle.PORTAL, c.clone().add(x, 0, z), 1, 0, 0, 0, 0);
                }

                c.getWorld().spawnParticle(Particle.END_ROD, c, 10, 0.6, 0.6, 0.6, 0.02);
                c.getWorld().spawnParticle(Particle.WITCH, c, 4, 0.3, 0.3, 0.3, 0.01);

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);

        return true;
    }

    private void cleanupDisable(Player p) {
        p.removeMetadata("astral_disable_active", plugin);

        for (Player pl : plugin.getServer().getOnlinePlayers()) {
            pl.removeMetadata("boots_disabled", plugin);
        }
    }

    /* ==========================================================
       PASSIVES
       ========================================================== */
/**
 * 10% chance to cancel any incoming damage.
 */
public boolean handleDamageCancellation(Player player) {
    double chance = 0.10; // 10%

    if (Math.random() <= chance) {
        player.getWorld().spawnParticle(
                Particle.END_ROD,
                player.getLocation().add(0, 1, 0),
                20, 0.4, 0.6, 0.4, 0.02
        );

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_ENDERMAN_TELEPORT,
                1f, 1.5f
        );
        return true;
    }

    return false;
}

    @Override
    public void applyTier1Passives(Player player) {}

    @Override
    public void applyTier2Passives(Player player) {}

    /* ==========================================================
       TELEPORT TRAIL
       ========================================================== */

    private void spawnTeleportTrail(Location from, Location to) {
        int steps = 25;
        double dx = (to.getX() - from.getX()) / steps;
        double dy = (to.getY() - from.getY()) / steps;
        double dz = (to.getZ() - from.getZ()) / steps;

        new BukkitRunnable() {
            int i = 0;

            @Override
            public void run() {
                if (i >= steps) {
                    cancel();
                    return;
                }

                Location trail = from.clone().add(dx * i, dy * i, dz * i);
                trail.getWorld().spawnParticle(Particle.END_ROD, trail, 2, 0.05, 0.05, 0.05, 0);

                i++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}
