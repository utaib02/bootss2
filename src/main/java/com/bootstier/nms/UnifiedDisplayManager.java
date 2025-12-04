package com.bootstier.nms;

import com.bootstier.BootsTierPlugin;
import com.bootstier.boots.BootType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Unified Display Manager (Particle Only Version)
 * ----------------------------------------------------
 * - NO ItemDisplay
 * - NO BlockDisplay
 * - ONLY particles for all visuals
 * - Still handles tornado + pedestal orbit
 */
public class UnifiedDisplayManager {

    private final BootsTierPlugin plugin;

    // Track holograms / tornado marker entities
    private final Map<UUID, Entity> temporaryEntities = new HashMap<>();

    // Rotation angle for particle rings
    private double rotation = 0;

    public UnifiedDisplayManager(BootsTierPlugin plugin) {
        this.plugin = plugin;
        startTickLoop();
    }

    /* =====================================================
       UPDATE LOOP (ONLY USED FOR PEDESTAL ORBIT)
    ===================================================== */
    private void startTickLoop() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            rotation += 0.15;
            if (rotation > Math.PI * 2) rotation = 0;
        }, 0L, 1L);
    }

    /* =====================================================
       PLAYER PARTICLE RINGS (handled by ParticleManager now)
       So UnifiedDisplayManager does NOTHING here
    ===================================================== */

    public void refreshPlayerDisplays(Player player) {
        // Nothing needed (no more displays)
    }

    public void clearPlayerDisplays(Player player) {
        cleanupAll();
    }

    /* =====================================================
       PARTICLE ABILITY RINGS (replaces BlockDisplay)
    ===================================================== */

    public void createAbilityRing(Location center, double radius, int count, Material material, int durationTicks) {
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= durationTicks) {
                    cancel();
                    return;
                }

                for (int i = 0; i < count; i++) {
                    double ang = (2 * Math.PI * i) / count;
                    double x = Math.cos(ang) * radius;
                    double z = Math.sin(ang) * radius;

                    Location loc = center.clone().add(x, 0.1, z);

                    center.getWorld().spawnParticle(
                            Particle.BLOCK,
                            loc,
                            2, 0.05, 0.05, 0.05,
                            0,
                            material.createBlockData()
                    );
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    /* =====================================================
       EXPANDING RING EFFECT (pure particle)
    ===================================================== */

    public void animateExpandingRing(Location center, Material material, double maxRadius, int durationTicks) {
        int points = 18;

        new BukkitRunnable() {
            double radius = 0.5;

            @Override
            public void run() {
                if (radius >= maxRadius) {
                    cancel();
                    return;
                }

                for (int i = 0; i < points; i++) {
                    double ang = (2 * Math.PI * i) / points;
                    double x = Math.cos(ang) * radius;
                    double z = Math.sin(ang) * radius;

                    Location loc = center.clone().add(x, 0.1, z);

                    center.getWorld().spawnParticle(
                            Particle.BLOCK,
                            loc,
                            2, 0.05, 0.05, 0.05,
                            0,
                            material.createBlockData()
                    );
                }

                radius += 0.3;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    /* =====================================================
       TORNADO (already 100% particle-based)
    ===================================================== */

    public ArmorStand createTornado(Location center, int durationTicks) {
        ArmorStand marker = center.getWorld().spawn(center, ArmorStand.class);
        marker.setVisible(false);
        marker.setGravity(false);
        marker.setInvulnerable(true);
        marker.setMarker(true);

        UUID id = UUID.randomUUID();
        temporaryEntities.put(id, marker);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= durationTicks || marker.isDead()) {
                    marker.remove();
                    temporaryEntities.remove(id);
                    cancel();
                    return;
                }

                double height = 5.0;
                int layers = 3;
                int points = 20;

                for (int layer = 0; layer < layers; layer++) {
                    for (int i = 0; i < points; i++) {

                        double progress = i / (double) points;
                        double ang = (progress * 2 * Math.PI) + (ticks * 0.25) + (layer * 1.2);
                        double radius = 2.2 * (1 - progress);
                        double y = center.getY() + height * progress;

                        double x = center.getX() + Math.cos(ang) * radius;
                        double z = center.getZ() + Math.sin(ang) * radius;

                        Location loc = new Location(center.getWorld(), x, y, z);

                        center.getWorld().spawnParticle(Particle.CLOUD, loc, 2, 0.05, 0.05, 0.05, 0.01);

                        if (ticks % 5 == 0) {
                            center.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 1, 0, 0, 0, 0);
                        }
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return marker;
    }

    /* =====================================================
       PEDESTAL ORBIT (replaces block displays with particles)
    ===================================================== */

    public void createPedestalOrbit(Location center, int durationTicks) {
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= durationTicks) {
                    cancel();
                    return;
                }

                double radius = 3.0;
                int points = 12;
                rotation += 0.12;

                for (int i = 0; i < points; i++) {
                    double ang = rotation + ((2 * Math.PI * i) / points);
                    double x = Math.cos(ang) * radius;
                    double z = Math.sin(ang) * radius;

                    Location loc = center.clone().add(0.5 + x, 1.4, 0.5 + z);

                    center.getWorld().spawnParticle(Particle.END_ROD, loc, 1, 0, 0, 0, 0);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 3L);
    }

    /* =====================================================
       CLEANUP
    ===================================================== */

    public void cleanupAll() {
        temporaryEntities.values().forEach(entity -> {
            if (!entity.isDead()) entity.remove();
        });
        temporaryEntities.clear();
    }

    public void cleanupPlayer(UUID playerId) {
        // No player-based displays anymore
    }
}
