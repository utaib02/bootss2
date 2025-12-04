package com.bootstier.boots.abilities.impl;

import com.bootstier.BootsTierPlugin;
import com.bootstier.boots.abilities.BootAbility;
import com.bootstier.boots.BootType;
import com.bootstier.player.PlayerData;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * FINAL FROST ABILITY — FULLY FIXED FOR DRAGON EGG + PROJECTILE BLOCKING
 */
public class FrostAbility implements BootAbility, Listener {

    private final BootsTierPlugin plugin;

    private final Map<UUID, Long> tier1Cooldown = new HashMap<>();
    private final Map<UUID, Long> tier2Cooldown = new HashMap<>();

    // Cage blocks tracked so players cannot break them
    private final Set<Location> activeCageBlocks = new HashSet<>();

    public FrostAbility(final BootsTierPlugin plugin) {
        this.plugin = plugin;
        this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
    }

    /* ================================================================
       COOLDOWN HELPERS (NOW USING REAL COOLDOWN FROM ACTION BAR MANAGER)
       ================================================================ */

    private long getRealCooldown(Player player, int tier) {
        boolean egg = player.getInventory().contains(Material.DRAGON_EGG);
        return plugin.getActionBarManager()
                .getTotalAbilityCooldown(BootType.FROST, tier, egg);
    }

    private boolean isOnCooldown(Map<UUID, Long> map, UUID id) {
        long now = System.currentTimeMillis();
        return map.containsKey(id) && map.get(id) > now;
    }

    private long getRemaining(Map<UUID, Long> map, UUID id) {
        return Math.max(0, map.getOrDefault(id, 0L) - System.currentTimeMillis());
    }

    private void applyCooldown(Map<UUID, Long> map, UUID id, long ms) {
        map.put(id, System.currentTimeMillis() + ms);
    }

    /* ================================================================
       ░░ TIER 1 – FROST SHIELD (PROJECTILE BLOCKING + DAMAGE REFLECT) ░░
       ================================================================ */
/**
 * Reflects a small percent of damage back to attacker if shield is active.
 */
public void handleDamageReflection(Player victim, Player attacker, double damage) {
    if (!victim.hasMetadata("frost_shield_active")) return;

    // reflect 20% damage back to attacker
    double reflected = damage * 0.20;

    attacker.damage(reflected, victim);

    victim.getWorld().spawnParticle(Particle.SNOWFLAKE, victim.getLocation(), 10, 0.3, 0.3, 0.3, 0.02);
    victim.getWorld().playSound(victim.getLocation(), Sound.BLOCK_GLASS_HIT, 1f, 1.4f);
}

    @Override
    public boolean executeTier1(Player player) {
        UUID id = player.getUniqueId();

        long cd = getRealCooldown(player, 1);

        if (isOnCooldown(tier1Cooldown, id)) {
            long s = getRemaining(tier1Cooldown, id) / 1000;
            player.sendMessage("§cFrost Shield cooldown: §b" + s + "s");
            return false;
        }

        if (player.hasMetadata("frost_shield_active")) {
            player.sendMessage("§cYour Frost Shield is already active!");
            return false;
        }

        player.setMetadata("frost_shield_active",
                new FixedMetadataValue(plugin, true));

        Location loc = player.getLocation();
        World w = loc.getWorld();
        if (w != null) {
            w.playSound(loc, Sound.BLOCK_GLASS_PLACE, 1.2f, 0.7f);
            w.spawnParticle(Particle.SNOWFLAKE, loc.add(0, 1.2, 0),
                    40, 0.5, 0.8, 0.5, 0.05);
        }

        List<BlockDisplay> blocks = createIceShield(player);

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cleanupShield(blocks, player);
                    cancel();
                    return;
                }

                if (t >= 100) {
                    cleanupShield(blocks, player);
                    cancel();
                    return;
                }

                updateIceShield(player, blocks, t);
                t++;
            }

        }.runTaskTimer(plugin, 0L, 1L);

        applyCooldown(tier1Cooldown, id, cd);

        player.sendMessage("§b❄ Frost Shield Active!");

        return true;
    }

    /**
     * FULLY SEALED — ONLY PACKED_ICE + BLUE_ICE
     */
    private List<BlockDisplay> createIceShield(Player player) {
        List<BlockDisplay> list = new ArrayList<>();
        World w = player.getWorld();
        if (w == null) return list;

        Location base = player.getLocation();
        Vector forward = base.getDirection().setY(0).normalize();
        if (forward.lengthSquared() == 0) forward = new Vector(0, 0, 1);

        Vector right = forward.clone().crossProduct(new Vector(0, 1, 0)).normalize();

        int segments = 9;
        double arc = Math.PI / 1.5;
        double radius = 2.2;

        for (int i = 0; i < segments; i++) {

            double t = (i / (segments - 1.0)) - 0.5;
            double angle = t * arc;

            double cos = Math.cos(angle);
            double sin = Math.sin(angle);

            Vector dir = forward.clone().multiply(cos).add(right.clone().multiply(sin));
            Location loc = base.clone().add(dir.multiply(radius)).add(0, 1.3, 0);

            Material mat = (i % 2 == 0 ? Material.PACKED_ICE : Material.BLUE_ICE);

            BlockDisplay bd = w.spawn(loc, BlockDisplay.class, d -> {
                d.setBlock(mat.createBlockData());
                d.setPersistent(false);
                d.setInvulnerable(true);
            });

            list.add(bd);
        }

        return list;
    }

    private void updateIceShield(Player player, List<BlockDisplay> blocks, int tick) {
        World w = player.getWorld();
        if (w == null) return;

        Location base = player.getLocation();
        Vector forward = base.getDirection().setY(0).normalize();
        if (forward.lengthSquared() == 0) forward = new Vector(0, 0, 1);

        Vector right = forward.clone().crossProduct(new Vector(0, 1, 0)).normalize();

        double arc = Math.PI / 1.5;
        double baseRadius = 2.2;

        for (int i = 0; i < blocks.size(); i++) {
            BlockDisplay bd = blocks.get(i);
            if (bd == null || bd.isDead()) continue;

            double t = (i / (double) (blocks.size() - 1)) - 0.5;
            double angle = t * arc;

            double r = baseRadius + 0.1 * Math.sin((tick + i * 4) * 0.18);

            double cos = Math.cos(angle);
            double sin = Math.sin(angle);

            Vector dir = forward.clone().multiply(cos).add(right.clone().multiply(sin));

            double y = 1.3 + 0.1 * Math.sin((tick + i * 6) * 0.15);

            Location target = base.clone().add(dir.multiply(r)).add(0, y, 0);
            bd.teleport(target);

            w.spawnParticle(Particle.SNOWFLAKE, target.clone().add(0, 0.1, 0),
                    1, 0.1, 0.1, 0.1, 0.01);
        }
    }

    private void cleanupShield(List<BlockDisplay> blocks, Player player) {
        player.removeMetadata("frost_shield_active", plugin);

        for (BlockDisplay bd : blocks) {
            if (bd != null && !bd.isDead()) {
                Location l = bd.getLocation();
                World w = l.getWorld();
                if (w != null)
                    w.spawnParticle(Particle.SNOWFLAKE, l, 8, 0.2, 0.2, 0.2, 0.02);

                bd.remove();
            }
        }
    }

    /* ================================================================
       PROJECTILE BLOCKING (SHIELD)
       ================================================================ */

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent e) {
        if (!(e.getEntity().getShooter() instanceof LivingEntity)) return;
        Projectile proj = e.getEntity();

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.hasMetadata("frost_shield_active")) continue;

            if (proj.getLocation().distance(p.getLocation()) <= 3.3) {
                proj.remove();

                // particles
                proj.getWorld().spawnParticle(Particle.SNOWFLAKE,
                        proj.getLocation(), 10, 0.3, 0.3, 0.3, 0.03);
                proj.getWorld().playSound(proj.getLocation(),
                        Sound.BLOCK_GLASS_HIT, 0.6f, 1.8f);
                return;
            }
        }
    }

    /* ================================================================
       ░░ TIER 2 – ICE CIRCLE (CAGE) — FIXED SEAMS & RESTORES PROPERLY ░░
       ================================================================ */

    @Override
    public boolean executeTier2(Player player) {
        UUID id = player.getUniqueId();

        long cd = getRealCooldown(player, 2);

        if (isOnCooldown(tier2Cooldown, id)) {
            long s = getRemaining(tier2Cooldown, id) / 1000;
            player.sendMessage("§cIce Circle cooldown: §b" + s + "s");
            return false;
        }

        Location center = player.getLocation().clone();
        World w = center.getWorld();
        if (w == null) return false;

        double R = 5.0;
        int height = 5;

        Map<Location, Material> original = new HashMap<>();

        for (int x = -5; x <= 5; x++)
            for (int y = 0; y <= height; y++)
                for (int z = -5; z <= 5; z++) {

                    double dist = Math.sqrt(x * x + y * y + z * z);
                    if (dist < R - 0.35 || dist > R + 0.35) continue;

                    Location loc = center.clone().add(x, y, z);
                    Block b = w.getBlockAt(loc);

                    original.put(loc, b.getType());

                    b.setType((x + y + z) % 2 == 0 ? Material.BLUE_ICE : Material.PACKED_ICE);
                    activeCageBlocks.add(loc);
                }

        for (int x = -5; x <= 5; x++)
            for (int z = -5; z <= 5; z++) {
                if (Math.sqrt(x * x + z * z) <= R) {
                    Location loc = center.clone().add(x, -1, z);
                    Block b = w.getBlockAt(loc);

                    original.put(loc, b.getType());
                    b.setType(Material.PACKED_ICE);
                    activeCageBlocks.add(loc);
                }
            }

        w.playSound(center, Sound.BLOCK_GLASS_PLACE, 1.2f, 0.5f);

        player.sendMessage("§b❄ Ice Circle formed!");

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (t >= 300) {
                    restore(original);
                    cancel();
                    return;
                }

                t++;
            }
        }.runTaskTimer(plugin, 0, 1);

        applyCooldown(tier2Cooldown, id, cd);

        return true;
    }

    private void restore(Map<Location, Material> map) {
        for (Map.Entry<Location, Material> e : map.entrySet()) {
            Block b = e.getKey().getWorld().getBlockAt(e.getKey());
            b.setType(e.getValue());
            activeCageBlocks.remove(e.getKey());
        }
    }

    /* ================================================================
       CAGE BLOCK PROTECTION
       ================================================================ */

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (activeCageBlocks.contains(e.getBlock().getLocation())) {
            e.setCancelled(true);
            e.getPlayer().playSound(e.getBlock().getLocation(),
                    Sound.BLOCK_GLASS_HIT, 0.7f, 1.8f);
        }
    }

    /* ================================================================
       PASSIVES
       ================================================================ */

    @Override
    public void applyTier1Passives(Player p) {
        if (p.getFreezeTicks() > 0) p.setFreezeTicks(0);
    }

    @Override
    public void applyTier2Passives(Player p) {
        PlayerData d = plugin.getPlayerManager().getPlayerData(p);
        if (d.getBootsData().getHitCounter() >= 10) {
            p.damage(2.0);
            p.setFreezeTicks(100);
            d.getBootsData().resetHitCounter();
            plugin.getPlayerManager().savePlayerData(d);
        }
    }
}
