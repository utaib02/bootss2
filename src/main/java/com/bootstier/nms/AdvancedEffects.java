package com.bootstier.nms;

import com.bootstier.BootsTierPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Advanced visual effects using entities and animations.
 * Also manages temporary animated visuals for cleanup.
 */
public class AdvancedEffects {

    private final BootsTierPlugin plugin;

    // Track active particle/entity visuals (like orbiting items, displays, etc.)
    private final Map<UUID, TrackedEffect> activeEffects = new HashMap<>();

    public AdvancedEffects(final BootsTierPlugin plugin) {
        this.plugin = plugin;
    }

    /* --------------------------------------------------
       BASIC EFFECT HELPERS
    -------------------------------------------------- */

    public void createCirclingItemEntity(final Player player, final Material material, final double radius, final int durationTicks) {
        final Item itemEntity = player.getWorld().dropItem(player.getLocation(), new ItemStack(material));
        itemEntity.setGravity(false);
        itemEntity.setPickupDelay(Integer.MAX_VALUE);
        itemEntity.setInvulnerable(true);

        // Track and animate
        TrackedEffect effect = new TrackedEffect(itemEntity, System.currentTimeMillis() + (durationTicks * 50L));
        activeEffects.put(itemEntity.getUniqueId(), effect);

        new BukkitRunnable() {
            private int ticks = 0;
            private double angle = 0;

            @Override
            public void run() {
                if (ticks >= durationTicks || itemEntity.isDead()) {
                    itemEntity.remove();
                    activeEffects.remove(itemEntity.getUniqueId());
                    cancel();
                    return;
                }

                angle += 0.2;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                Location newLoc = player.getLocation().add(x, 1.5, z);
                itemEntity.teleport(newLoc);

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public ArmorStand createFloatingDisplay(final Location location, final String displayName, final int durationTicks) {
        final ArmorStand armorStand = location.getWorld().spawn(location, ArmorStand.class);
        armorStand.setVisible(false);
        armorStand.setGravity(false);
        armorStand.setInvulnerable(true);
        armorStand.setCustomName(displayName);
        armorStand.setCustomNameVisible(true);
        armorStand.setMarker(true);

        // Track and cleanup automatically
        activeEffects.put(armorStand.getUniqueId(), new TrackedEffect(armorStand, System.currentTimeMillis() + (durationTicks * 50L)));

        if (durationTicks > 0) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    armorStand.remove();
                    activeEffects.remove(armorStand.getUniqueId());
                }
            }.runTaskLater(plugin, durationTicks);
        }
        return armorStand;
    }

    public void createExpandingRing(final Location center, final double maxRadius, final int steps, final Material blockType) {
        new BukkitRunnable() {
            private int currentStep = 0;
            @Override
            public void run() {
                if (currentStep >= steps) {
                    cancel();
                    return;
                }
                double radius = (maxRadius / steps) * (currentStep + 1);
                for (double angle = 0; angle < 2 * Math.PI; angle += 0.1) {
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location particleLoc = center.clone().add(x, 0.5, z);
                    center.getWorld().spawnParticle(
                        org.bukkit.Particle.BLOCK_CRUMBLE,
                        particleLoc,
                        3, 0.1, 0.1, 0.1, 0.1, blockType.createBlockData());
                }
                currentStep++;
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    public void createTrailEffect(final Location start, final Location end, final org.bukkit.Particle particle, final int steps) {
        final Vector direction = end.toVector().subtract(start.toVector()).normalize();
        final double distance = start.distance(end);
        final double stepSize = distance / steps;

        new BukkitRunnable() {
            private int currentStep = 0;
            @Override
            public void run() {
                if (currentStep >= steps) {
                    cancel();
                    return;
                }
                Location particleLoc = start.clone().add(direction.clone().multiply(stepSize * currentStep));
                start.getWorld().spawnParticle(particle, particleLoc, 1, 0, 0, 0, 0);
                currentStep++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void createSpiralEffect(final Location center, final double radius, final double height, final org.bukkit.Particle particle, final int durationTicks) {
        new BukkitRunnable() {
            private int ticks = 0;
            private double angle = 0;

            @Override
            public void run() {
                if (ticks >= durationTicks) {
                    cancel();
                    return;
                }
                angle += 0.3;
                double currentHeight = (height / durationTicks) * ticks;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                Location particleLoc = center.clone().add(x, currentHeight, z);
                center.getWorld().spawnParticle(particle, particleLoc, 1, 0, 0, 0, 0);
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public List<Entity> createEntityRing(final Location center, final double radius, final int entityCount, final Material itemType) {
        final List<Entity> entities = new ArrayList<>();

        for (int i = 0; i < entityCount; i++) {
            double angle = (2 * Math.PI * i) / entityCount;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Location entityLoc = center.clone().add(x, 0.5, z);
            Item item = center.getWorld().dropItem(entityLoc, new ItemStack(itemType));
            item.setGravity(false);
            item.setPickupDelay(Integer.MAX_VALUE);
            item.setInvulnerable(true);
            entities.add(item);
            activeEffects.put(item.getUniqueId(), new TrackedEffect(item, System.currentTimeMillis() + 60000L)); // 1 minute lifetime
        }

        return entities;
    }

    public void removeEntities(final List<Entity> entities) {
        for (final Entity entity : entities) {
            if (entity != null && !entity.isDead()) {
                entity.remove();
                activeEffects.remove(entity.getUniqueId());
            }
        }
    }

    /* --------------------------------------------------
       NEW METHOD FOR PLUGIN SCHEDULER LOOP
    -------------------------------------------------- */

    /**
     * Called every few ticks by BootsTierPlugin to clean up
     * expired or invalid effects.
     */
    public void updateActiveEffects() {
        long now = System.currentTimeMillis();

        Iterator<Map.Entry<UUID, TrackedEffect>> it = activeEffects.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, TrackedEffect> entry = it.next();
            Entity entity = entry.getValue().entity();
            long expiry = entry.getValue().expiry();

            if (entity == null || entity.isDead() || expiry < now) {
                if (entity != null && !entity.isDead()) entity.remove();
                it.remove();
            }
        }
    }


    /* --------------------------------------------------
       LIGHTNING EFFECTS
    -------------------------------------------------- */

    /**
     * Creates a lightning strike effect at location (visual only)
     */
    public void createLightningEffect(final Location location) {
        location.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, location, 30, 0.5, 2, 0.5, 0.2);
        location.getWorld().spawnParticle(Particle.FLASH, location, 1, 0, 0, 0, 0);
        
        // Upward bolt animation
        new BukkitRunnable() {
            private double y = 0;
            
            @Override
            public void run() {
                if (y >= 3) {
                    cancel();
                    return;
                }
                
                final Location boltLoc = location.clone().add(0, y, 0);
                location.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, boltLoc, 5, 0.2, 0.2, 0.2, 0.1);
                y += 0.3;
            }
        }.runTaskTimer(this.plugin, 0L, 1L);
    }

    /* --------------------------------------------------
       FIRE EFFECTS
    -------------------------------------------------- */

    /**
     * Creates expanding fire rings for Fire Boots Tier 1
     */
    public void createFireRings(final Location center, final int ringCount, final double maxRadius) {
        new BukkitRunnable() {
            private int currentRing = 0;
            private double radius = 0.5;
            
            @Override
            public void run() {
                if (currentRing >= ringCount) {
                    cancel();
                    return;
                }
                
                // Draw fire ring at current radius
                final int points = (int) (radius * 20);
                for (int i = 0; i < points; i++) {
                    final double angle = 2 * Math.PI * i / points;
                    final double x = center.getX() + Math.cos(angle) * radius;
                    final double z = center.getZ() + Math.sin(angle) * radius;
                    final Location ringLoc = new Location(center.getWorld(), x, center.getY(), z);
                    
                    center.getWorld().spawnParticle(Particle.FLAME, ringLoc, 3, 0.1, 0.1, 0.1, 0.02);
                    center.getWorld().spawnParticle(Particle.SMOKE, ringLoc, 1, 0.1, 0.1, 0.1, 0.01);
                }
                
                radius += 1.5;
                if (radius >= maxRadius) {
                    currentRing++;
                    radius = 0.5;
                }
            }
        }.runTaskTimer(this.plugin, 0L, 2L);
    }

    /* --------------------------------------------------
       ICE EFFECTS
    -------------------------------------------------- */

    /**
     * Creates ice circle boundary effect for Frost Boots Tier 2
     */
    public void createIceCircle(final Location center, final double radius, final int durationTicks) {
        new BukkitRunnable() {
            private int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= durationTicks) {
                    cancel();
                    return;
                }
                
                // Rotating ice particle ring
                final int points = 32;
                final double rotation = ticks * 0.05;
                for (int i = 0; i < points; i++) {
                    final double angle = (2 * Math.PI * i / points) + rotation;
                    final double x = center.getX() + Math.cos(angle) * radius;
                    final double z = center.getZ() + Math.sin(angle) * radius;
                    final Location ringLoc = new Location(center.getWorld(), x, center.getY() + 0.1, z);
                    
                    center.getWorld().spawnParticle(Particle.SNOWFLAKE, ringLoc, 2, 0.1, 0.1, 0.1, 0);
                    
                    // Occasional ice spikes
                    if (ticks % 20 == 0 && i % 4 == 0) {
                        center.getWorld().spawnParticle(Particle.BLOCK, ringLoc.clone().add(0, 0.5, 0), 
                            5, 0.1, 0.5, 0.1, 0, Material.PACKED_ICE.createBlockData());
                    }
                }
                
                ticks++;
            }
        }.runTaskTimer(this.plugin, 0L, 2L);
    }

    /* --------------------------------------------------
       WIND/TORNADO EFFECTS
    -------------------------------------------------- */

    /**
     * Creates tornado effect at location for Wind Boots Tier 2
     */
    public void createTornado(final Location center, final int durationTicks) {
        new BukkitRunnable() {
            private int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= durationTicks) {
                    cancel();
                    return;
                }
                
                // Spiral wind effect
                final double height = 5.0;
                final int spirals = 3;
                final int pointsPerSpiral = 20;
                
                for (int spiral = 0; spiral < spirals; spiral++) {
                    for (int i = 0; i < pointsPerSpiral; i++) {
                        final double progress = (double) i / pointsPerSpiral;
                        final double angle = (2 * Math.PI * spiral / spirals) + (progress * 2 * Math.PI) + (ticks * 0.1);
                        final double radius = 2.0 * (1 - progress);
                        final double y = center.getY() + (height * progress);
                        
                        final double x = center.getX() + Math.cos(angle) * radius;
                        final double z = center.getZ() + Math.sin(angle) * radius;
                        final Location spiralLoc = new Location(center.getWorld(), x, y, z);
                        
                        center.getWorld().spawnParticle(Particle.CLOUD, spiralLoc, 1, 0, 0, 0, 0);
                        center.getWorld().spawnParticle(Particle.SWEEP_ATTACK, spiralLoc, 1, 0, 0, 0, 0);
                    }
                }
                
                ticks++;
            }
        }.runTaskTimer(this.plugin, 0L, 1L);
    }

    /* --------------------------------------------------
       SHADOW/DARKNESS EFFECTS
    -------------------------------------------------- */

    /**
     * Creates shadow aura around player for Ward Boots
     */
    public void createShadowAura(final Player player, final int durationTicks) {
        new BukkitRunnable() {
            private int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= durationTicks || !player.isOnline()) {
                    cancel();
                    return;
                }
                
                final Location loc = player.getLocation();
                
                // Shadow particles orbiting player
                for (double angle = 0; angle < 2 * Math.PI; angle += 0.4) {
                    final double radius = 1.5 + Math.sin(ticks * 0.1) * 0.5;
                    final double x = Math.cos(angle + ticks * 0.05) * radius;
                    final double z = Math.sin(angle + ticks * 0.05) * radius;
                    final Location particleLoc = loc.clone().add(x, 1, z);
                    player.getWorld().spawnParticle(Particle.SMOKE, particleLoc, 1, 0, 0, 0, 0);
                }
                
                // Dark portal effect at feet
                player.getWorld().spawnParticle(Particle.PORTAL, loc.clone().add(0, 0.1, 0), 
                    3, 0.3, 0.1, 0.3, 0);
                
                ticks++;
            }
        }.runTaskTimer(this.plugin, 0L, 2L);
    }

    /* --------------------------------------------------
       WATER/WHIRLPOOL EFFECTS
    -------------------------------------------------- */

    /**
     * Creates whirlpool effect for Water Boots Tier 1
     */
    public void createWhirlpool(final Location center, final double radius, final int durationTicks) {
        new BukkitRunnable() {
            private int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= durationTicks) {
                    cancel();
                    return;
                }
                
                // Spiral water particles
                final int spirals = 5;
                for (int spiral = 0; spiral < spirals; spiral++) {
                    final double spiralRadius = radius * (1 - ((double) spiral / spirals));
                    final double angle = (ticks * 0.2) + (2 * Math.PI * spiral / spirals);
                    
                    final double x = center.getX() + Math.cos(angle) * spiralRadius;
                    final double z = center.getZ() + Math.sin(angle) * spiralRadius;
                    final Location spiralLoc = new Location(center.getWorld(), x, center.getY(), z);
                    
                    center.getWorld().spawnParticle(Particle.DRIPPING_WATER, spiralLoc, 2, 0.1, 0.1, 0.1, 0);
                    center.getWorld().spawnParticle(Particle.BUBBLE_POP, spiralLoc, 1, 0.1, 0.1, 0.1, 0);
                }
                
                // Center vortex
                center.getWorld().spawnParticle(Particle.SPLASH, center, 5, 0.3, 0.3, 0.3, 0.1);
                
                ticks++;
            }
        }.runTaskTimer(this.plugin, 0L, 2L);
    }

    /* --------------------------------------------------
       ASTRAL EFFECTS
    -------------------------------------------------- */

    /**
     * Creates astral teleport trail effect for Astral Boots
     */
    public void createAstralTrail(final Location start, final Location end) {
        final Vector direction = end.toVector().subtract(start.toVector());
        final double distance = start.distance(end);
        final int steps = (int) (distance * 5);
        
        new BukkitRunnable() {
            private int currentStep = 0;
            
            @Override
            public void run() {
                if (currentStep >= steps) {
                    cancel();
                    return;
                }
                
                final double progress = (double) currentStep / steps;
                final Location particleLoc = start.clone().add(direction.clone().multiply(progress));
                
                start.getWorld().spawnParticle(Particle.PORTAL, particleLoc, 3, 0.1, 0.1, 0.1, 0.02);
                start.getWorld().spawnParticle(Particle.ENCHANT, particleLoc, 2, 0.1, 0.1, 0.1, 0.1);
                
                currentStep++;
            }
        }.runTaskTimer(this.plugin, 0L, 1L);
    }

    /* --------------------------------------------------
       HELPER RECORD
    -------------------------------------------------- */
    private record TrackedEffect(Entity entity, long expiry) { }
}
