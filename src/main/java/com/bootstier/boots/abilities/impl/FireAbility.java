package com.bootstier.boots.abilities.impl;

import com.bootstier.BootsTierPlugin;
import com.bootstier.boots.abilities.BootAbility;
import com.bootstier.utils.LocationUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class FireAbility implements BootAbility {

    private final BootsTierPlugin plugin;
    private final Random random;

    // Per-player cooldowns (store "ready at" timestamps)
    private final Map<UUID, Long> tier1Cooldown = new HashMap<>();
    private final Map<UUID, Long> tier2Cooldown = new HashMap<>();

    // Base cooldowns (must match ActionBarManager)
    private static final long TIER1_CD_MS = 60_000L; // 60s – Fire Rings
    private static final long TIER2_CD_MS = 75_000L; // 75s – Flame Dash

    public FireAbility(final BootsTierPlugin plugin) {
        this.plugin = plugin;
        this.random = new Random();
    }

    /* ==========================================================
       COOLDOWN HELPERS (using maps + real dragon egg modifier)
       ========================================================== */

    private boolean isOnCooldown(Map<UUID, Long> map, UUID id) {
        return map.getOrDefault(id, 0L) > System.currentTimeMillis();
    }

    private long getCooldownLeft(Map<UUID, Long> map, UUID id) {
        return Math.max(0L, map.getOrDefault(id, 0L) - System.currentTimeMillis());
    }

    private void setCooldown(Map<UUID, Long> map, UUID id, long cdMs) {
        map.put(id, System.currentTimeMillis() + cdMs);
    }

    private boolean hasDragonEgg(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.DRAGON_EGG) {
                return true;
            }
        }
        return false;
    }

    /* ==========================================================
       TIER 1 – FIRE RINGS
       3 quick rings, 5-block radius, removes water in ring
       ========================================================== */

    @Override
    public boolean executeTier1(final Player player) {
        final UUID id = player.getUniqueId();

        // Apply dragon egg modifier to actual cooldown (NOT just visual)
        final boolean egg = hasDragonEgg(player);
        final long effectiveCd = egg ? TIER1_CD_MS / 2L : TIER1_CD_MS;

        if (isOnCooldown(tier1Cooldown, id)) {
            long left = getCooldownLeft(tier1Cooldown, id) / 1000L;
            player.sendMessage(ChatColor.RED + "Fire Rings on cooldown: " +
                    ChatColor.YELLOW + left + "s");
            return false;
        }

        if (player.hasMetadata("fire_rings_active")) {
            player.sendMessage(ChatColor.RED + "Fire Rings are already active!");
            return false;
        }

        player.setMetadata("fire_rings_active", new FixedMetadataValue(plugin, true));

        Location center = player.getLocation().clone();

        // Activation Effects
        if (center.getWorld() != null) {
            center.getWorld().spawnParticle(
                    Particle.FLAME,
                    center.clone().add(0, 1, 0),
                    30, 0.5, 0.5, 0.5, 0.2
            );
            center.getWorld().playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.2f, 0.8f);
        }

        player.sendMessage(ChatColor.RED + "🔥 " + ChatColor.BOLD + "Fire Rings!");

        new BukkitRunnable() {
            int rings = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    player.removeMetadata("fire_rings_active", plugin);
                    cancel();
                    return;
                }

                if (rings >= 3) {
                    player.removeMetadata("fire_rings_active", plugin);
                    cancel();
                    return;
                }

                double radius = 5.0; // Always a 5-block ring
                createFireRing(player, center, radius);

                if (center.getWorld() != null) {
                    center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.3f);
                }

                rings++;
            }
        }.runTaskTimer(plugin, 0, 10);

        // REAL cooldown now respects dragon egg
        setCooldown(tier1Cooldown, id, effectiveCd);
        return true;
    }

    /* ==========================================================
       TIER 2 – FLAME DASH (MARK + WEAKNESS + GLOW)
       ========================================================== */

    @Override
    public boolean executeTier2(final Player player) {
        final UUID id = player.getUniqueId();

        final boolean egg = hasDragonEgg(player);
        final long effectiveCd = egg ? TIER2_CD_MS / 2L : TIER2_CD_MS;

        if (isOnCooldown(tier2Cooldown, id)) {
            long left = getCooldownLeft(tier2Cooldown, id) / 1000L;
            player.sendMessage(ChatColor.RED + "Flame Dash on cooldown: " +
                    ChatColor.YELLOW + left + "s");
            return false;
        }

        if (player.hasMetadata("fire_dash_active")) {
            player.sendMessage(ChatColor.RED + "Already dashing!");
            return false;
        }

        player.setMetadata("fire_dash_active", new FixedMetadataValue(plugin, true));

        // Short, controlled dash
        Vector dir = player.getLocation().getDirection().normalize().multiply(1.2);
        dir.setY(0.2);
        player.setVelocity(dir);

        List<Player> marked = new ArrayList<>();

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    player.removeMetadata("fire_dash_active", plugin);
                    cancel();
                    return;
                }

                // After 1 second → apply debuffs to all marked
                if (t >= 20) {
                    for (Player p : marked) {
                        if (!p.isOnline()) continue;

                        p.setMetadata("fire_marked", new FixedMetadataValue(
                                plugin, System.currentTimeMillis() + 10_000L
                        ));

                        p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 200, 0));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 200, 0));

                        if (p.getWorld() != null) {
                            p.getWorld().spawnParticle(
                                    Particle.FLAME,
                                    p.getLocation().add(0, 1, 0),
                                    10, 0.5, 0.5, 0.5, 0.1
                            );
                        }
                    }

                    player.removeMetadata("fire_dash_active", plugin);
                    cancel();
                    return;
                }

                // Mark nearby untrusted players
                for (Player near : LocationUtils.getPlayersInRadius(player.getLocation(), 3)) {
                    if (near.equals(player)) continue;
                    if (marked.contains(near)) continue;
                    if (plugin.getTrustManager().isTrusted(player, near)) continue;
                    marked.add(near);
                }

                // Dash trail
                if (player.getWorld() != null) {
                    player.getWorld().spawnParticle(
                            Particle.FLAME,
                            player.getLocation().add(0, 0.2, 0),
                            6, 0.2, 0.2, 0.2, 0.06
                    );
                }

                t++;
            }
        }.runTaskTimer(plugin, 0, 1);

        player.sendMessage(ChatColor.RED + "🔥 " + ChatColor.BOLD + "Flame Dash!");

        // REAL cooldown now respects dragon egg
        setCooldown(tier2Cooldown, id, effectiveCd);
        return true;
    }

    /* ==========================================================
       FIRE RING (POOF + FULL WATER CLEAR IN RING BAND)
       ========================================================== */

    private void createFireRing(Player caster, Location center, double radius) {
        if (center.getWorld() == null) return;

        // Damage / ignite untrusted in thin ring band
        for (LivingEntity e : center.getWorld().getNearbyEntities(center, radius + 1, 4, radius + 1)
                .stream()
                .filter(ent -> ent instanceof LivingEntity)
                .map(ent -> (LivingEntity) ent)
                .filter(ent -> !ent.equals(caster))
                .collect(Collectors.toList())) {

            double dist = e.getLocation().distance(center);
            if (dist <= radius && dist >= radius - 1) {

                if (e instanceof Player && plugin.getTrustManager().isTrusted(caster, (Player) e))
                    continue;

                e.damage(4.0, caster);
                e.setFireTicks(60);

                e.getWorld().spawnParticle(
                        Particle.LAVA,
                        e.getLocation().add(0, 1, 0),
                        8, 0.4, 0.4, 0.4, 0.1
                );
            }
        }

        // Remove water blocks in the ring band (y -1 → +2)
        for (double a = 0; a < Math.PI * 2; a += 0.15) {
            double x = Math.cos(a) * radius;
            double z = Math.sin(a) * radius;

            Location check = center.clone().add(x, 0, z);

            for (int y = -1; y <= 2; y++) {
                Location w = check.clone().add(0, y, 0);

                if (w.getBlock().getType() == Material.WATER) {
                    w.getBlock().setType(Material.AIR);
                    w.getBlock().setBlockData(
                            org.bukkit.Bukkit.createBlockData(Material.AIR)
                    );

                    w.getWorld().spawnParticle(
                            Particle.CLOUD,
                            w.clone().add(0.5, 0.5, 0.5),
                            5, 0.3, 0.3, 0.3, 0.05
                    );
                }
            }
        }

        // Poof ring particles (no block displays)
        for (double a = 0; a < Math.PI * 2; a += 0.07) {
            double x = Math.cos(a) * radius;
            double z = Math.sin(a) * radius;

            Location p = center.clone().add(x, 0.3, z);

            center.getWorld().spawnParticle(
                    Particle.FLAME,
                    p, 5, 0.15, 0.15, 0.15, 0.04
            );
            center.getWorld().spawnParticle(
                    Particle.LARGE_SMOKE,
                    p, 2, 0.1, 0.1, 0.1, 0.02
            );
        }
    }

    /* ==========================================================
       PASSIVES
       ========================================================== */

    @Override
    public void applyTier1Passives(Player player) {
        if (!player.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.FIRE_RESISTANCE,
                    100, 0, true, false
            ));
        }
    }

    @Override
    public void applyTier2Passives(Player player) {
        if (player.getLocation().getBlock().getType() == Material.MAGMA_BLOCK) {
            player.setFireTicks(0);
        }

        if (player.getFireTicks() > 0) {
            handleWaterDistinguish(player);
        }
    }

    /* ==========================================================
       OTHER LOGIC
       ========================================================== */

    public void handleAttackerIgnite(final Player attacker, final Player victim) {
        if (random.nextDouble() < 0.05) {
            attacker.setFireTicks(100);
            attacker.getWorld().spawnParticle(
                    Particle.FLAME,
                    attacker.getLocation().add(0, 1, 0),
                    8, 0.3, 0.3, 0.3, 0.1
            );
        }
    }

    public void handleMarkedPlayerHit(final Player victim) {
        if (victim.hasMetadata("fire_marked")) {
            long until = victim.getMetadata("fire_marked").get(0).asLong();
            if (System.currentTimeMillis() < until) {
                victim.setFireTicks(100);

                victim.getWorld().spawnParticle(
                        Particle.FLAME,
                        victim.getLocation().add(0, 1, 0),
                        10, 0.5, 0.5, 0.5, 0.1
                );
            } else {
                victim.removeMetadata("fire_marked", plugin);
            }
        }
    }

    public void handleLavaMovement(final Player player) {
        if (player.getLocation().getBlock().getType() == Material.LAVA) {
            player.setFireTicks(0);
            if (!player.hasPotionEffect(PotionEffectType.DOLPHINS_GRACE)) {
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.DOLPHINS_GRACE,
                        40, 0
                ));
            }
        }
    }

    private void handleWaterDistinguish(final Player player) {
        Location loc = player.getLocation();
        for (int x = -2; x <= 2; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -2; z <= 2; z++) {
                    Location c = loc.clone().add(x, y, z);
                    if (c.getBlock().getType() == Material.WATER) {
                        player.setFireTicks(0);
                        c.getWorld().spawnParticle(
                                Particle.CLOUD,
                                c.clone().add(0.5, 0.5, 0.5),
                                5, 0.3, 0.3, 0.3, 0.05
                        );
                        return;
                    }
                }
            }
        }
    }
}
