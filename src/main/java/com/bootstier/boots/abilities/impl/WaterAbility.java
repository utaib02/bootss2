package com.bootstier.boots.abilities.impl;

import com.bootstier.BootsTierPlugin;
import com.bootstier.boots.abilities.BootAbility;
import com.bootstier.utils.LocationUtils;
import com.bootstier.utils.MessageUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Water boots abilities implementation (dev-doc accurate, trust-aware, entity-aware)
 */
public class WaterAbility implements BootAbility {

    private final BootsTierPlugin plugin;
    private final Random random;

    // Per-player cooldowns (respecting dev doc)
    // Tier 1: 10s duration, 1:15 cooldown
    // Tier 2: not specified in doc, we mirror 1:15 for balance
    private static final long TIER1_CD_MS = 75_000L; // 1:15
    private static final long TIER2_CD_MS = 75_000L; // 1:15

    private final Map<UUID, Long> tier1Cooldown = new HashMap<>();
    private final Map<UUID, Long> tier2Cooldown = new HashMap<>();

    public WaterAbility(final BootsTierPlugin plugin) {
        this.plugin = plugin;
        this.random = new Random();
    }
private boolean hasDragonEgg(Player player) {
    return Arrays.stream(player.getInventory().getContents())
            .anyMatch(item -> item != null && item.getType() == Material.DRAGON_EGG);
}

    /* ==========================
       COOLDOWN HELPERS
       ========================== */

    private boolean isOnCooldown(Map<UUID, Long> map, UUID id) {
        long now = System.currentTimeMillis();
        return map.containsKey(id) && map.get(id) > now;
    }

    private long getCooldownLeft(Map<UUID, Long> map, UUID id) {
        long now = System.currentTimeMillis();
        return Math.max(0L, map.getOrDefault(id, 0L) - now);
    }

    private void setCooldown(Map<UUID, Long> map, UUID id, long cdMs) {
        map.put(id, System.currentTimeMillis() + cdMs);
    }

    /* ==========================
       TIER 1 – WHIRLPOOL
       Dev doc:
       - Follows player
       - 5 block radius
       - Outside radius: sucked in
       - Inside radius: stop sucking, slowly drown
       - In water: drown even faster
       - Duration: 10s, CD: 1:15
       ========================== */

    @Override
    public boolean executeTier1(final Player player) {
        UUID id = player.getUniqueId();

        // Cooldown check
        if (isOnCooldown(tier1Cooldown, id)) {
            long left = getCooldownLeft(tier1Cooldown, id) / 1000L;
            player.sendMessage(ChatColor.RED + "Whirlpool on cooldown: " +
                    ChatColor.AQUA + left + "s");
            return false;
        }

        // Mark active
        player.setMetadata("water_whirlpool_active", new FixedMetadataValue(this.plugin, true));

        // SFX + start burst
        Location center = player.getLocation();
        center.getWorld().playSound(center, Sound.ENTITY_DOLPHIN_AMBIENT, 1.2f, 1.4f);
        center.getWorld().playSound(center, Sound.ENTITY_PLAYER_SPLASH, 0.9f, 1.0f);

        // Initial cinematic burst – conduit + blue smoke + a bit of water
        center.getWorld().spawnParticle(Particle.NAUTILUS,
                center.clone().add(0, 1.0, 0),
                40, 1.0, 0.6, 1.0, 0.1);
        center.getWorld().spawnParticle(Particle.CLOUD,
                center.clone().add(0, 0.7, 0),
                30, 1.2, 0.4, 1.2, 0.02);
        center.getWorld().spawnParticle(Particle.SPLASH,
                center.clone().add(0, 0.8, 0),
                20, 0.8, 0.4, 0.8, 0.15);
        center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                center.clone().add(0, 1.4, 0),
                15, 0.5, 0.4, 0.5, 0.02);

        MessageUtils.sendMessage(player, ChatColor.AQUA + "🌊 " + ChatColor.BOLD + "Whirlpool! "
                + ChatColor.GRAY + "The water bends around you.");

        // Start cooldown immediately
final long totalCD = plugin.getActionBarManager().getTotalAbilityCooldown(
        plugin.getPlayerManager().getPlayerData(player).getBootsData().getBootType(),
        1,
        hasDragonEgg(player)
);
setCooldown(tier1Cooldown, id, totalCD);


        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    player.removeMetadata("water_whirlpool_active", plugin);
                    clearWhirlpoolMetadata();
                    cancel();
                    return;
                }

                if (ticks >= 200) { // 10 seconds
                    player.removeMetadata("water_whirlpool_active", plugin);
                    clearWhirlpoolMetadata();
                    cancel();
                    return;
                }

                Location center = player.getLocation(); // follows the player
                double whirlRadius = 5.0; // dev doc

                // All nearby living entities (players + mobs)
                List<LivingEntity> livingEntities = center.getWorld().getNearbyEntities(center, 8.0, 4.0, 8.0)
                        .stream()
                        .filter(e -> e instanceof LivingEntity)
                        .map(e -> (LivingEntity) e)
                        .collect(Collectors.toList());

                for (LivingEntity living : livingEntities) {
                    if (living.equals(player)) continue;

                    // Trust: trusted players NEVER get harmed
                    if (living instanceof Player) {
                        Player pl = (Player) living;
                        if (plugin.getTrustManager().isTrusted(player, pl)) {
                            // Give slight buff if they're inside the whirlpool
                            if (pl.getLocation().distance(center) <= whirlRadius) {
                                pl.addPotionEffect(new PotionEffect(
                                        PotionEffectType.DOLPHINS_GRACE, 40, 0, true, false));
                                pl.addPotionEffect(new PotionEffect(
                                        PotionEffectType.SPEED, 40, 0, true, false));
                            }
                            continue;
                        }
                    }

                    double distance = living.getLocation().distance(center);

                    if (distance > whirlRadius) {
                        // Outside whirlpool: get sucked in gently
                        Vector pull = center.toVector().subtract(living.getLocation().toVector());
                        if (pull.lengthSquared() > 0) {
                            pull.normalize().multiply(0.18); // soft pull
                            pull.setY(0);
                            living.setVelocity(living.getVelocity().add(pull));
                        }
                    } else {
                        // Inside whirlpool: they stop being pulled, start drowning / debuffed
                        if (living instanceof Player) {
                            Player p = (Player) living;
                            p.setMetadata("whirlpool_drowning", new FixedMetadataValue(plugin, true));

                            // Air reduction (faster in water)
                            int currentAir = p.getRemainingAir();
                            int loss = 6; // base loss
                            if (p.getLocation().getBlock().getType() == Material.WATER ||
                                    p.getLocation().add(0, 1, 0).getBlock().getType() == Material.WATER) {
                                loss += 10; // extra if actually in water
                            }
                            p.setRemainingAir(Math.max(0, currentAir - loss));
                        }

                        // Universal slow (all mobs and players)
                        living.addPotionEffect(new PotionEffect(
                                PotionEffectType.SLOWNESS, 40, 1, true, false));

                        // Extra "pressure" damage every 20 ticks
                        if (ticks % 20 == 0) {
                            living.damage(0.5, player); // 0.25 hearts per second-ish
                        }
                    }
                }

                // Whirlpool visual: rotating spiral rings (conduit + blue mist)
                spawnWhirlpoolParticles(center, ticks, whirlRadius);

                ticks++;
            }
        }.runTaskTimer(this.plugin, 0L, 1L);

        return true;
    }

    private void clearWhirlpoolMetadata() {
        for (Player online : this.plugin.getServer().getOnlinePlayers()) {
            online.removeMetadata("whirlpool_drowning", this.plugin);
        }
    }

    private void spawnWhirlpoolParticles(Location center, int ticks, double whirlRadius) {
        for (double r = 1.0; r <= whirlRadius; r += 0.7) {
            double angleOffset = ticks * 0.18; // rotation speed
            for (double angle = 0; angle < 2 * Math.PI; angle += 0.35) {
                double x = Math.cos(angle + angleOffset) * r;
                double z = Math.sin(angle + angleOffset) * r;
                double y = 0.1 + (r * 0.03);

                Location loc = center.clone().add(x, y, z);

                // Main "water magic" look – nautilus for consistency
                center.getWorld().spawnParticle(
                        Particle.NAUTILUS,
                        loc,
                        1, 0.08, 0.08, 0.08, 0.0
                );

                // Blue enchanted spark for magical effect
                if (random.nextDouble() < 0.5) {
                    center.getWorld().spawnParticle(
                            Particle.ENCHANTED_HIT,
                            loc.clone().add(0, 0.1, 0),
                            1, 0.12, 0.08, 0.12, 0.01
                    );
                }

                // Water dripping effect for aquatic feel
                if (random.nextDouble() < 0.25) {
                    center.getWorld().spawnParticle(
                            Particle.DRIPPING_WATER,
                            loc.clone().add(0, 0.05, 0),
                            1, 0.02, 0.04, 0.02, 0.0
                    );
                }

                // Splash particles to reinforce water aesthetic
                if (random.nextDouble() < 0.15) {
                    center.getWorld().spawnParticle(
                            Particle.SPLASH,
                            loc,
                            1, 0.03, 0.03, 0.03, 0.0
                    );
                }
            }
        }

        // Central vertical column – rising magic water core
        if (ticks % 2 == 0) {
            for (double y = 0.0; y <= 2.0; y += 0.25) {
                Location col = center.clone().add(0, y, 0);
                center.getWorld().spawnParticle(
                        Particle.NAUTILUS,
                        col,
                        1, 0.07, 0.07, 0.07, 0.0
                );

                if (random.nextDouble() < 0.4) {
                    center.getWorld().spawnParticle(
                            Particle.DRIPPING_WATER,
                            col,
                            1, 0.05, 0.05, 0.05, 0.0
                    );
                }
            }
        }

        // Every 10 ticks, an aqua shockwave ring
        if (ticks % 10 == 0) {
            double radius = whirlRadius - 0.5;
            for (double angle = 0; angle < 2 * Math.PI; angle += 0.18) {
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                Location ringLoc = center.clone().add(x, 0.3, z);

                center.getWorld().spawnParticle(
                        Particle.ENCHANTED_HIT,
                        ringLoc,
                        1, 0.15, 0.1, 0.15, 0.01
                );
                center.getWorld().spawnParticle(
                        Particle.DRIPPING_WATER,
                        ringLoc.clone().add(0, 0.05, 0),
                        1, 0.02, 0.03, 0.02, 0.0
                );
            }
        }
    }

    /* ==========================
       TIER 2 – TIDAL WAVE
       Dev doc:
       - Summons a wave from player's location
       - 10 block radius circle
       - Untrusted players pushed by wave can't jump for 5s
       - All mobs also get pushed
       ========================== */

    @Override
    public boolean executeTier2(final Player player) {
        UUID id = player.getUniqueId();

        if (isOnCooldown(tier2Cooldown, id)) {
            long left = getCooldownLeft(tier2Cooldown, id) / 1000L;
            player.sendMessage(ChatColor.RED + "Tidal Wave on cooldown: " +
                    ChatColor.AQUA + left + "s");
            return false;
        }

        final Location center = player.getLocation().clone();

        // Start cooldown
final long totalCD = plugin.getActionBarManager().getTotalAbilityCooldown(
        plugin.getPlayerManager().getPlayerData(player).getBootsData().getBootType(),
        2,
        hasDragonEgg(player)
);
setCooldown(tier2Cooldown, id, totalCD);


        // Push entities out
        List<LivingEntity> livingEntities = center.getWorld().getNearbyEntities(center, 10.0, 4.0, 10.0)
                .stream()
                .filter(e -> e instanceof LivingEntity)
                .map(e -> (LivingEntity) e)
                .collect(Collectors.toList());

        for (LivingEntity living : livingEntities) {
            if (living.equals(player)) continue;

            if (living instanceof Player) {
                Player target = (Player) living;

                // Trust: trusted players never get debuffs
                if (plugin.getTrustManager().isTrusted(player, target)) {
                    // Optional small buff from the wave
                    target.addPotionEffect(new PotionEffect(
                            PotionEffectType.SPEED, 60, 0, true, false));
                    continue;
                }

                // Untrusted players: push + jump disable 5s
                Vector push = living.getLocation().toVector().subtract(center.toVector());
                if (push.lengthSquared() > 0) {
                    push.normalize().multiply(1.9);
                    push.setY(0.6);
                    living.setVelocity(push);
                }

                target.setMetadata("water_jump_disabled",
                        new FixedMetadataValue(this.plugin, System.currentTimeMillis() + 5000));
            } else {
                // Mobs: just get yeeted by the wave
                Vector push = living.getLocation().toVector().subtract(center.toVector());
                if (push.lengthSquared() > 0) {
                    push.normalize().multiply(1.9);
                    push.setY(0.6);
                    living.setVelocity(push);
                }
            }
        }

        // Visual wave ring expanding out – blue smoke + conduit streaks
        new BukkitRunnable() {
            private double currentRadius = 1.0;

            @Override
            public void run() {
                if (currentRadius > 10.0) {
                    cancel();
                    return;
                }

                for (double angle = 0; angle < 2 * Math.PI; angle += 0.12) {
                    double x = Math.cos(angle) * currentRadius;
                    double z = Math.sin(angle) * currentRadius;
                    Location loc = center.clone().add(x, 0.4, z);

                    // Main wave mist
                    center.getWorld().spawnParticle(
                            Particle.CLOUD,
                            loc,
                            2, 0.18, 0.12, 0.18, 0.01
                    );

                    // Magic water streaks
                    if (random.nextDouble() < 0.4) {
                        center.getWorld().spawnParticle(
                                Particle.NAUTILUS,
                                loc.clone().add(0, 0.05, 0),
                                1, 0.06, 0.06, 0.06, 0.0
                        );
                    }

                    // Occasional soul-flame crackle at edge
                    if (random.nextDouble() < 0.25) {
                        center.getWorld().spawnParticle(
                                Particle.SOUL_FIRE_FLAME,
                                loc.clone().add(0, 0.1, 0),
                                1, 0.02, 0.03, 0.02, 0.0
                        );
                    }

                    // Very small splash hints so it still feels like water
                    if (random.nextDouble() < 0.15) {
                        center.getWorld().spawnParticle(
                                Particle.SPLASH,
                                loc,
                                1, 0.04, 0.04, 0.04, 0.0
                        );
                    }
                }

                currentRadius += 0.6;
            }
        }.runTaskTimer(this.plugin, 0L, 2L);

        // Sounds + chat
        center.getWorld().playSound(center, Sound.ENTITY_DROWNED_SWIM, 1.3f, 0.7f);
        center.getWorld().playSound(center, Sound.WEATHER_RAIN_ABOVE, 0.8f, 1.3f);
        center.getWorld().playSound(center, Sound.BLOCK_CONDUIT_ACTIVATE, 1.0f, 1.2f);
        MessageUtils.sendMessage(player, ChatColor.AQUA + "🌊 " + ChatColor.BOLD + "Tidal Wave! "
                + ChatColor.GRAY + "The ocean surges out from your feet.");

        return true;
    }

    /* ==========================
       PASSIVES
       ========================== */

    @Override
    public void applyTier1Passives(final Player player) {
        // Conduit Power (dev doc)
        if (!player.hasPotionEffect(PotionEffectType.CONDUIT_POWER)) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.CONDUIT_POWER, 100, 0, true, false));
        }
    }

    @Override
    public void applyTier2Passives(final Player player) {
        // Wet passive:
        // - While in water: become "wet" (no fire damage)
        // - After leaving, stay immune 5s via metadata

        boolean inWater = player.getLocation().getBlock().getType() == Material.WATER
                || player.getLocation().add(0, 1, 0).getBlock().getType() == Material.WATER;

        if (inWater) {
            player.setMetadata("water_wet",
                    new FixedMetadataValue(this.plugin, System.currentTimeMillis() + 5000));

            // Visual wet drip
            player.getWorld().spawnParticle(Particle.DRIPPING_WATER,
                    player.getLocation().add(0, 0.1, 0),
                    3, 0.3, 0.1, 0.3, 0.0);
        }

        // While "wet": no fire damage
        if (isWet(player)) {
            if (player.getFireTicks() > 0) {
                player.setFireTicks(0);
            }
        }
    }

    /* ==========================
       EXTRA HANDLERS (DEV DOC)
       ========================== */

    // Tier 2 passive: Trident hit 20% chance to thunder target, 1 heart damage
    public void handleTridentHit(final Player attacker, final LivingEntity victim) {
        // Don't hurt trusted players
        if (victim instanceof Player) {
            Player p = (Player) victim;
            if (this.plugin.getTrustManager().isTrusted(attacker, p)) return;
        }

        if (this.random.nextDouble() < 0.20) { // 20%
            victim.getWorld().strikeLightningEffect(victim.getLocation());
            victim.damage(2.0, attacker); // 1 heart

            victim.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                    victim.getLocation().add(0, 1, 0),
                    15, 0.5, 1, 0.5, 0.1);

            if (attacker.isOnline()) {
                MessageUtils.sendMessage(attacker, ChatColor.AQUA + "⚡ "
                        + ChatColor.GRAY + "Your trident channels a storm!");
            }
        }
    }

    public boolean canJump(final Player player) {
        if (player.hasMetadata("water_jump_disabled")) {
            final long disabledUntil = player.getMetadata("water_jump_disabled").get(0).asLong();
            if (System.currentTimeMillis() < disabledUntil) {
                return false;
            } else {
                player.removeMetadata("water_jump_disabled", this.plugin);
            }
        }
        return true;
    }

    public boolean isWet(final Player player) {
        if (player.hasMetadata("water_wet")) {
            final long wetUntil = player.getMetadata("water_wet").get(0).asLong();
            if (System.currentTimeMillis() < wetUntil) {
                return true;
            } else {
                player.removeMetadata("water_wet", this.plugin);
            }
        }
        return false;
    }
}
