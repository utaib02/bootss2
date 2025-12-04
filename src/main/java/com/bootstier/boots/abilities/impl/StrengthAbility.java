package com.bootstier.boots.abilities.impl;

import com.bootstier.BootsTierPlugin;
import com.bootstier.boots.BootType;
import com.bootstier.boots.abilities.BootAbility;
import com.bootstier.effects.ActionBarManager;
import com.bootstier.player.PlayerData;
import com.bootstier.utils.MessageUtils;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StrengthAbility implements BootAbility {

    private final BootsTierPlugin plugin;

    // cooldown maps
    private final Map<UUID, Long> tier1Cooldown = new HashMap<>();
    private final Map<UUID, Long> tier2Cooldown = new HashMap<>();

    public StrengthAbility(final BootsTierPlugin plugin) {
        this.plugin = plugin;
    }

    /* ============================================================
                   DRAGON-EGG COOLDOWN SYSTEM
       ============================================================ */

    private long getRealCooldown(Player player, int tier) {
        ActionBarManager ab = plugin.getActionBarManager();
        boolean hasEgg = hasDragonEgg(player);
        return ab.getTotalAbilityCooldown(BootType.STRENGTH, tier, hasEgg);
    }

    private boolean hasDragonEgg(Player p) {
        for (ItemStack i : p.getInventory().getContents()) {
            if (i != null && i.getType() == Material.DRAGON_EGG) return true;
        }
        return false;
    }

    private boolean isOnCooldown(Map<UUID, Long> map, Player p) {
        return map.getOrDefault(p.getUniqueId(), 0L) > System.currentTimeMillis();
    }

    private long getRemaining(Map<UUID, Long> map, Player p) {
        return Math.max(0, map.getOrDefault(p.getUniqueId(), 0L) - System.currentTimeMillis());
    }

    private void applyCooldown(Map<UUID, Long> map, Player p, long ms) {
        map.put(p.getUniqueId(), System.currentTimeMillis() + ms);
    }

    /* ============================================================
                    TIER 1 – CRITICAL SURGE (10s)
       ============================================================ */

    @Override
    public boolean executeTier1(final Player player) {

        if (isOnCooldown(tier1Cooldown, player)) {
            player.sendMessage(ChatColor.RED + "Critical Strikes on cooldown: §e"
                    + (getRemaining(tier1Cooldown, player) / 1000) + "s");
            return false;
        }

        long realCD = getRealCooldown(player, 1);

        long end = System.currentTimeMillis() + 10_000L; // 10s window
        player.setMetadata("strength_critical_active",
                new FixedMetadataValue(plugin, end));

        createCriticalAura(player);

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 0.8f);
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.5f);

        MessageUtils.sendMessage(player,
                "§c⚔ §lCritical Strikes! §7Every hit pierces shields for §c10s§7!");

        applyCooldown(tier1Cooldown, player, realCD);
        return true;
    }

    /* ============================================================
                  TIER 2 – DAMAGE LINK (30s)
       ============================================================ */

    @Override
    public boolean executeTier2(final Player player) {

        if (isOnCooldown(tier2Cooldown, player)) {
            player.sendMessage(ChatColor.RED + "Damage Link on cooldown: §c"
                    + (getRemaining(tier2Cooldown, player) / 1000) + "s");
            return false;
        }

        long realCD = getRealCooldown(player, 2);

        PlayerData data = plugin.getPlayerManager().getPlayerData(player);
        data.getBootsData().setRadius(1.0);

        long end = System.currentTimeMillis() + 30_000L;

        player.setMetadata("damage_link_active", new FixedMetadataValue(plugin, end));
        player.setMetadata("damage_link_radius", new FixedMetadataValue(plugin, 1.0));

        createDamageLinkAura(player);

        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.2f);

        MessageUtils.sendMessage(player,
                "§4⚔ §lDamage Link! §7Your pain spreads to enemies. Radius: §c1 block§7.");

        plugin.getPlayerManager().savePlayerData(data);
        applyCooldown(tier2Cooldown, player, realCD);

        return true;
    }

    /* ============================================================
                    PASSIVES (UNCHANGED)
       ============================================================ */

    @Override
    public void applyTier1Passives(Player player) {}

    @Override
    public void applyTier2Passives(Player player) {
        if (!player.hasPotionEffect(PotionEffectType.STRENGTH)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH,
                    100, 0, true, false));
        }
    }

    /* ============================================================
                          HIT HANDLER
       ============================================================ */

    public void handlePlayerHit(Player attacker, Player victim, double baseDamage) {
        PlayerData data = plugin.getPlayerManager().getPlayerData(attacker);

        /* ===== TIER 1 CRITICAL WINDOW ===== */
        if (attacker.hasMetadata("strength_critical_active")) {
            long end = attacker.getMetadata("strength_critical_active").get(0).asLong();
            if (System.currentTimeMillis() > end) {
                attacker.removeMetadata("strength_critical_active", plugin);
            } else {
                double bonus = baseDamage * 0.5; // +50% damage
                victim.damage(bonus, attacker);

                Location hit = victim.getLocation().add(0, 1, 0);
                World w = victim.getWorld();

                w.spawnParticle(Particle.CRIT, hit, 8, 0.3, 0.3, 0.3, 0.1);
                w.spawnParticle(Particle.ENCHANTED_HIT, hit, 4, 0.3, 0.3, 0.3, 0.05);
                w.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1.2f);
            }
        }

        /* ===== TIER 1 SIPHON ===== */
        if (data.getBootsData().getTier().getLevel() == 1) {
            data.getBootsData().incrementHitCounter();

            if (data.getBootsData().getHitCounter() >= 5) {
                attacker.setHealth(Math.min(attacker.getMaxHealth(), attacker.getHealth() + 1.0));

                data.getBootsData().resetHitCounter();
                attacker.getWorld().spawnParticle(Particle.HEART,
                        attacker.getLocation().add(0, 2, 0),
                        6, 0.4, 0.4, 0.4, 0.1);
                attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2.0f);

                MessageUtils.sendMessage(attacker, "§c⚔ §lSiphon! §7You drain life.");
            }

        } else {
            /* ===== TIER 2 DAMAGE LINK ===== */
            if (attacker.hasMetadata("damage_link_active")) {
                long end = attacker.getMetadata("damage_link_active").get(0).asLong();

                if (System.currentTimeMillis() > end) {
                    attacker.removeMetadata("damage_link_active", plugin);
                    attacker.removeMetadata("damage_link_radius", plugin);
                    data.getBootsData().setRadius(0.0);
                } else {
                    boolean crit = attacker.hasMetadata("strength_critical_active");
                    boolean untrusted = !(victim instanceof Player &&
                            plugin.getTrustManager().isTrusted(attacker, (Player) victim));

                    if (crit && untrusted && attacker.hasMetadata("damage_link_radius")) {
                        double current = attacker.getMetadata("damage_link_radius").get(0).asDouble();
                        double next = Math.min(current + 1.0, 10.0);

                        if (next > current) {
                            attacker.setMetadata("damage_link_radius",
                                    new FixedMetadataValue(plugin, next));
                            data.getBootsData().setRadius(next);
                            createRadiusExpansionEffect(attacker, next);

                            MessageUtils.sendMessage(attacker,
                                    "§4⚔ §lRadius Expanded! §7Now: §c" + (int) next);
                        }
                    }

                    double radius = data.getBootsData().getRadius();
                    Location center = attacker.getLocation();
                    World w = center.getWorld();

                    for (Entity e : w.getNearbyEntities(center, radius, radius, radius)) {
                        if (!(e instanceof LivingEntity)) continue;

                        LivingEntity le = (LivingEntity) e;

                        if (le.equals(attacker) || le.equals(victim)) continue;

                        if (le instanceof Player && plugin.getTrustManager().isTrusted(attacker, (Player) le))
                            continue;

                        le.damage(baseDamage, attacker);

                        createDamageLinkEffect(attacker, le);
                    }
                }
            }
        }

        plugin.getPlayerManager().savePlayerData(data);
    }

    /* ============================================================
                          VISUAL EFFECTS
       ============================================================ */

    private void createCriticalAura(Player player) {
        new BukkitRunnable() {

            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline()
                        || !player.hasMetadata("strength_critical_active")
                        || ticks >= 80) {
                    cancel();
                    return;
                }

                Location loc = player.getLocation().add(0, 1.1, 0);
                World w = loc.getWorld();

                w.spawnParticle(Particle.CRIT, loc, 4, 0.4, 0.3, 0.4, 0.08);
                w.spawnParticle(Particle.CRIMSON_SPORE,
                        loc.clone().add(0, 0.2, 0), 2, 0.15, 0.15, 0.15, 0.02);

                ticks += 10;
            }
        }.runTaskTimer(plugin, 0, 10);
    }

    private void createDamageLinkAura(Player player) {
        new BukkitRunnable() {

            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline()
                        || !player.hasMetadata("damage_link_active")
                        || ticks >= 120) {
                    cancel();
                    return;
                }

                double radius = player.hasMetadata("damage_link_radius")
                        ? player.getMetadata("damage_link_radius").get(0).asDouble()
                        : 1.0;

                Location base = player.getLocation();
                World w = base.getWorld();

                for (double angle = 0; angle < 2 * Math.PI; angle += 0.35) {
                    Location ring = base.clone().add(Math.cos(angle) * radius, 0.35, Math.sin(angle) * radius);
                    w.spawnParticle(Particle.CRIMSON_SPORE, ring, 1, 0.04, 0.04, 0.04, 0.0);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 5);
    }

    private void createRadiusExpansionEffect(Player player, double newRadius) {
        new BukkitRunnable() {

            double current = Math.max(0.5, newRadius - 1.0);

            @Override
            public void run() {
                if (!player.isOnline() || current >= newRadius) {
                    cancel();
                    return;
                }

                Location base = player.getLocation();
                World w = base.getWorld();

                for (double angle = 0; angle < 2 * Math.PI; angle += 0.3) {
                    Location ring = base.clone().add(Math.cos(angle) * current, 0.35, Math.sin(angle) * current);
                    w.spawnParticle(Particle.CRIMSON_SPORE, ring, 1, 0.06, 0.06, 0.06, 0.0);
                }

                current += 0.35;
            }
        }.runTaskTimer(plugin, 0, 3);
    }

    private void createDamageLinkEffect(Player src, LivingEntity target) {
        Location a = src.getLocation().add(0, 1, 0);
        Location b = target.getLocation().add(0, 1, 0);

        World w = src.getWorld();
        double dist = a.distance(b);
        int steps = Math.max(4, (int)(dist * 3));

        Vector step = b.clone().subtract(a).toVector().multiply(1.0 / steps);

        for (int i = 0; i <= steps; i++) {
            w.spawnParticle(Particle.CRIMSON_SPORE,
                    a.clone().add(step.clone().multiply(i)), 1, 0, 0, 0, 0);
        }

        w.spawnParticle(Particle.DAMAGE_INDICATOR, b, 4, 0.2, 0.2, 0.2, 0.1);
        w.playSound(b, Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.8f, 1.0f);
    }
}
