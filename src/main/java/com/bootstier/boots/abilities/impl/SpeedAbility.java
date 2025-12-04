package com.bootstier.boots.abilities.impl;

import com.bootstier.BootsTierPlugin;
import com.bootstier.boots.BootType;
import com.bootstier.boots.abilities.BootAbility;
import com.bootstier.effects.ActionBarManager;
import com.bootstier.player.PlayerData;
import com.bootstier.utils.MessageUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class SpeedAbility implements BootAbility {

    private final BootsTierPlugin plugin;

    // REAL cooldown maps — ability stores NEXT USE TIMESTAMP in ms
    private final Map<UUID, Long> tier1Cooldown = new HashMap<>();
    private final Map<UUID, Long> tier2Cooldown = new HashMap<>();

    public SpeedAbility(final BootsTierPlugin plugin) {
        this.plugin = plugin;
    }

    /* ============================================================
                     COOLDOWN USING ACTIONBAR MANAGER
       ============================================================ */

    private long getRealCooldown(Player player, int tier) {
        ActionBarManager ab = plugin.getActionBarManager();

        return ab.getTotalAbilityCooldown(
                BootType.SPEED,
                tier,
                hasDragonEgg(player)
        );
    }

    private boolean hasDragonEgg(Player p) {
        for (ItemStack item : p.getInventory().getContents()) {
            if (item != null && item.getType() == Material.DRAGON_EGG) return true;
        }
        return false;
    }

    private boolean isOnCooldown(Map<UUID, Long> map, UUID id) {
        return map.getOrDefault(id, 0L) > System.currentTimeMillis();
    }

    private long getRemaining(Map<UUID, Long> map, UUID id) {
        return Math.max(0L, map.getOrDefault(id, 0L) - System.currentTimeMillis());
    }

    private void applyCooldown(Map<UUID, Long> map, UUID id, long cdMs) {
        map.put(id, System.currentTimeMillis() + cdMs);
    }

    /* ============================================================
                           TIER 1 – BLUR
       ============================================================ */

    @Override
    public boolean executeTier1(Player player) {
        UUID id = player.getUniqueId();
        long realCD = getRealCooldown(player, 1);

        if (isOnCooldown(tier1Cooldown, id)) {
            player.sendMessage(ChatColor.RED + "Blur on cooldown: §b" + (getRemaining(tier1Cooldown, id) / 1000) + "s");
            return false;
        }

        // Effects (unchanged)
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 2, true, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 200, 2, true, false));

        createBlurEffect(player);

        // Insta-gap marking
        player.setMetadata("speed_blur_active",
                new FixedMetadataValue(plugin, System.currentTimeMillis() + 10_000));

        player.playSound(player.getLocation(), Sound.ENTITY_PHANTOM_SWOOP, 1f, 1.5f);
        MessageUtils.sendMessage(player, "§b⚡ §lBlur activated!");

        // REAL DRAGONEGG-AWARE COOLDOWN
        applyCooldown(tier1Cooldown, id, realCD);

        return true;
    }

    private void createBlurEffect(final Player player) {
        new BukkitRunnable() {
            int ticks = 0;
            public void run() {
                if (!player.isOnline()) { cancel(); return; }
                if (ticks >= 200) {
                    player.removePotionEffect(PotionEffectType.INVISIBILITY);
                    cancel();
                    return;
                }

                if (ticks % 8 == 0) {
                    if (player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                        player.removePotionEffect(PotionEffectType.INVISIBILITY);
                        player.getWorld().spawnParticle(Particle.CLOUD,
                                player.getLocation().add(0, 1, 0), 5,
                                0.3, 0.5, 0.3, 0.05);
                    } else {
                        player.addPotionEffect(new PotionEffect(
                                PotionEffectType.INVISIBILITY, 6, 0, true, false
                        ));
                        player.getWorld().spawnParticle(Particle.SMOKE,
                                player.getLocation().add(0, 1, 0),
                                8, 0.4, 0.6, 0.4, 0.02);
                    }
                }

                Location loc = player.getLocation();
                Vector dir = loc.getDirection().multiply(-1);
                if (dir.lengthSquared() == 0) dir = new Vector(0, 0, -1);

                for (int i = 1; i <= 5; i++) {
                    Location t = loc.clone().add(dir.clone().multiply(i * 0.3));
                    player.getWorld().spawnParticle(Particle.CLOUD, t, 2, 0.1, 0.1, 0.1, 0.02);
                    player.getWorld().spawnParticle(Particle.CRIT, t, 1, 0.1, 0.1, 0.1, 0.01);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    /* ============================================================
                 TIER 2 – THUNDER STRIKE (5s WINDOW)
       ============================================================ */

    @Override
    public boolean executeTier2(Player player) {
        UUID id = player.getUniqueId();
        long realCD = getRealCooldown(player, 2);

        if (isOnCooldown(tier2Cooldown, id)) {
            player.sendMessage(ChatColor.RED + "Thunder Strike on cooldown: §e" + (getRemaining(tier2Cooldown, id) / 1000) + "s");
            return false;
        }

        // Mark 5s lightning window
        player.setMetadata("speed_thunder_window",
                new FixedMetadataValue(plugin, System.currentTimeMillis() + 5000));

        createThunderChargingEffect(player);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1.8f);
        MessageUtils.sendMessage(player, "§e⚡ §lThunder Strike activated! §7(5s)");

        // REAL DRAGON-EGG-MODIFIED COOLDOWN
        applyCooldown(tier2Cooldown, id, realCD);

        return true;
    }

    private void createThunderChargingEffect(Player player) {
        new BukkitRunnable() {
            int ticks = 0;
            public void run() {
                if (!player.isOnline()) { cancel(); return; }
                if (ticks >= 60) { cancel(); return; }

                Location loc = player.getLocation().add(0, 1.8, 0);
                player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 4,
                        0.25, 0.25, 0.25, 0.1);

                if (ticks % 20 == 0) {
                    player.playSound(player.getLocation(),
                            Sound.BLOCK_NOTE_BLOCK_CHIME, 0.6f, 2f);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    /* ============================================================
                      HIT HANDLER (5s thunder window)
       ============================================================ */

    public void handlePlayerHit(Player attacker, Player victim) {
        PlayerData data = plugin.getPlayerManager().getPlayerData(attacker);

        // passive stacking
        if (data.getBootsData().getTier().getLevel() >= 2) {
            int cur = attacker.hasPotionEffect(PotionEffectType.SPEED)
                    ? attacker.getPotionEffect(PotionEffectType.SPEED).getAmplifier() : -1;

            attacker.addPotionEffect(new PotionEffect(
                    PotionEffectType.SPEED, 200, Math.min(cur + 1, 4), true, false
            ));
        }

        // 5s lightning window?
        if (attacker.hasMetadata("speed_thunder_window")) {
            long end = attacker.getMetadata("speed_thunder_window").get(0).asLong();

            if (System.currentTimeMillis() <= end) {

                victim.getWorld().strikeLightningEffect(victim.getLocation());
                victim.damage(2.0, attacker);

                victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 255, true, false));
                victim.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 80, 255, true, false));
                victim.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 80, -10, true, false));
                victim.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 80, 0, true, false));

                victim.getWorld().spawnParticle(
                        Particle.ELECTRIC_SPARK,
                        victim.getLocation().add(0, 1, 0),
                        10, 0.5, 0.5, 0.5, 0.2
                );
            } else {
                attacker.removeMetadata("speed_thunder_window", plugin);
            }
        }
    }

    /* ============================================================
                          PASSIVES / GAPS
       ============================================================ */

    @Override
    public void applyTier1Passives(Player player) {
        if (!player.hasPotionEffect(PotionEffectType.SPEED)) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.SPEED, 100, 0, true, false
            ));
        }
        handleInstaGap(player);
    }

    @Override
    public void applyTier2Passives(Player player) {}

    private void handleInstaGap(Player player) {
        if (!player.hasMetadata("speed_blur_active")) return;

        long end = player.getMetadata("speed_blur_active").get(0).asLong();
        if (System.currentTimeMillis() > end) {
            player.removeMetadata("speed_blur_active", plugin);
        }
    }

    public void handleInstaGapUse(Player player) {
        if (!player.hasMetadata("speed_blur_active")) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.GOLDEN_APPLE &&
                item.getType() != Material.ENCHANTED_GOLDEN_APPLE) return;

        if (player.hasMetadata("speed_gap_cooldown")) {
            long cd = player.getMetadata("speed_gap_cooldown").get(0).asLong();
            if (System.currentTimeMillis() < cd) return;
        }

        if (item.getType() == Material.GOLDEN_APPLE) {
            player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 4));
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 0));
        } else {
            player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 8));
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 400, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 3));
        }

        item.setAmount(item.getAmount() - 1);
        player.setMetadata("speed_gap_cooldown",
                new FixedMetadataValue(plugin, System.currentTimeMillis() + 1000));
    }
}
