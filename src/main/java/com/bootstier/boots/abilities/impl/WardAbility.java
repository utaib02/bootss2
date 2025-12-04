package com.bootstier.boots.abilities.impl;

import com.bootstier.BootsTierPlugin;
import com.bootstier.boots.BootType;
import com.bootstier.boots.abilities.BootAbility;
import com.bootstier.effects.ActionBarManager;
import com.bootstier.utils.LocationUtils;
import com.bootstier.utils.MessageUtils;
import org.bukkit.*;
import org.bukkit.util.Vector;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class WardAbility implements BootAbility {

    private final BootsTierPlugin plugin;

    private final Map<UUID, Long> tier1Cooldown = new HashMap<>();
    private final Map<UUID, Long> tier2Cooldown = new HashMap<>();

    public WardAbility(final BootsTierPlugin plugin) {
        this.plugin = plugin;
    }

    /* ============================================================
                   DRAGON EGG COOLDOWN SYSTEM
       ============================================================ */

    private boolean hasDragonEgg(Player p) {
        for (ItemStack i : p.getInventory().getContents()) {
            if (i != null && i.getType() == Material.DRAGON_EGG) return true;
        }
        return false;
    }

    private long getRealCooldown(Player p, int tier) {
        ActionBarManager ab = plugin.getActionBarManager();
        return ab.getTotalAbilityCooldown(BootType.WARD, tier, hasDragonEgg(p));
    }

    private boolean isOnCooldown(Map<UUID, Long> map, Player p) {
        return map.getOrDefault(p.getUniqueId(), 0L) > System.currentTimeMillis();
    }

    private long getLeft(Map<UUID, Long> map, Player p) {
        return Math.max(0, map.getOrDefault(p.getUniqueId(), 0L) - System.currentTimeMillis());
    }

    private void applyCooldown(Map<UUID, Long> map, Player p, long ms) {
        map.put(p.getUniqueId(), System.currentTimeMillis() + ms);
    }

    /* ============================================================
                       TIER 1 – SHADOW VEIL
       ============================================================ */

    @Override
    public boolean executeTier1(final Player player) {

        if (isOnCooldown(tier1Cooldown, player)) {
            player.sendMessage("§5[Ward] §cShadow Veil on cooldown: §d"
                    + (getLeft(tier1Cooldown, player) / 1000) + "s");
            return false;
        }

        if (player.hasMetadata("ward_invisible")) {
            player.sendMessage("§5[Ward] §7You are already cloaked.");
            return false;
        }

        long realCD = getRealCooldown(player, 1);

        long until = System.currentTimeMillis() + 10_000L;
        player.setMetadata("ward_invisible", new FixedMetadataValue(plugin, true));
        player.setMetadata("ward_invisible_until", new FixedMetadataValue(plugin, until));

        plugin.getPacketManager().hidePlayerFromUntrusted(player);

        Location loc = player.getLocation();
        World w = player.getWorld();

        w.playSound(loc, Sound.ENTITY_WARDEN_HEARTBEAT, 1f, 0.5f);
        w.playSound(loc, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.7f, 1.4f);

        MessageUtils.sendMessage(player, "§5👁 §lShadow Veil Activated.");

        createShadowAura(player);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cleanup(player);
                    cancel();
                    return;
                }

                long now = System.currentTimeMillis();
                long end = player.getMetadata("ward_invisible_until").get(0).asLong();
                if (now >= end) {
                    cleanup(player);
                    cancel();
                    return;
                }

                Location c = player.getLocation();
                World world = c.getWorld();

                for (Entity e : world.getNearbyEntities(c, 5, 5, 5)) {
                    if (!(e instanceof LivingEntity le)) continue;
                    if (e.equals(player)) continue;

                    if (e instanceof Player p2) {
                        if (plugin.getTrustManager().isTrusted(player, p2)) continue;

                        p2.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 40, 0, true, false));
                        p2.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, true, false));

                        p2.spawnParticle(Particle.SMOKE, p2.getLocation().add(0, 1, 0),
                                10, 0.4, 0.5, 0.4, 0.02);
                        p2.spawnParticle(Particle.SCULK_SOUL, p2.getLocation().add(0, 1.2, 0),
                                6, 0.3, 0.4, 0.3, 0.04);

                    } else {
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0, true, false));
                        le.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 40, 0, true, false));
                    }
                }

            }
        }.runTaskTimer(plugin, 0L, 10L);

        applyCooldown(tier1Cooldown, player, realCD);
        return true;
    }

    private void cleanup(Player player) {
        if (player.hasMetadata("ward_invisible"))
            player.removeMetadata("ward_invisible", plugin);
        if (player.hasMetadata("ward_invisible_until"))
            player.removeMetadata("ward_invisible_until", plugin);

        plugin.getPacketManager().showPlayerToAll(player);

        World w = player.getWorld();
        w.playSound(player.getLocation(), Sound.BLOCK_SCULK_SENSOR_CLICKING_STOP, 0.8f, 1.2f);

        w.spawnParticle(Particle.SCULK_SOUL,
                player.getLocation().add(0, 1.2, 0),
                30, 0.5, 0.5, 0.5, 0.06);
    }

    /* ============================================================
                     TIER 2 – SCULK SENSOR
       ============================================================ */

    @Override
    public boolean executeTier2(final Player player) {

        if (isOnCooldown(tier2Cooldown, player)) {
            player.sendMessage("§5[Ward] §cSensor on cooldown: §d"
                    + (getLeft(tier2Cooldown, player) / 1000) + "s");
            return false;
        }

        if (player.hasMetadata("ward_sensor_active")) {
            player.sendMessage("§5👁 §cYou already placed a sensor.");
            return false;
        }

        Location place = findSensorPlacement(player);
        if (place == null) {
            player.sendMessage("§5[Ward] §cNo valid placement.");
            return false;
        }

        long realCD = getRealCooldown(player, 2);

        World w = place.getWorld();

        ArmorStand sensor = w.spawn(place.clone().add(0, 0.1, 0), ArmorStand.class, as -> {
            as.setVisible(false);
            as.setGravity(false);
            as.setInvulnerable(false);
            as.setCustomName("§5Ward Sensor");
            as.setCustomNameVisible(true);
        });

        sensor.setMetadata("ward_sensor", new FixedMetadataValue(plugin, player.getUniqueId()));
        sensor.setMetadata("ward_sensor_hp", new FixedMetadataValue(plugin, 3));

        player.setMetadata("ward_sensor_active", new FixedMetadataValue(plugin, sensor.getUniqueId()));

        place.getBlock().setType(Material.SCULK_SENSOR);

        createSensorAnimation(sensor, place);

        new BukkitRunnable() {
            int time = 60;

            @Override
            public void run() {
                if (sensor.isDead() || !player.isOnline()) {
                    place.getBlock().setType(Material.AIR);
                    player.removeMetadata("ward_sensor_active", plugin);
                    cancel();
                    return;
                }

                if (time <= 0) {
                    activateSensor(player, place);
                    sensor.remove();
                    place.getBlock().setType(Material.AIR);
                    player.removeMetadata("ward_sensor_active", plugin);
                    cancel();
                    return;
                }

                sensor.setCustomName("§5Ward Sensor §7(" + time + "s)");

                if (time <= 10) {
                    w.spawnParticle(Particle.SCULK_SOUL,
                            place.clone().add(0, 1, 0),
                            10, 0.4, 0.4, 0.4, 0.08);

                    w.playSound(place, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.4f, 1.8f);
                }

                time--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        w.playSound(place, Sound.BLOCK_SCULK_SENSOR_PLACE, 1f, 0.9f);
        w.spawnParticle(Particle.SCULK_SOUL,
                place.clone().add(0, 0.8, 0),
                25, 0.5, 0.5, 0.5, 0.07);

        MessageUtils.sendMessage(player, "§5👁 §lSensor placed — §d60s§7 till activation.");

        applyCooldown(tier2Cooldown, player, realCD);
        return true;
    }

    /* ============================================================
             VISUALS – DARKER, STRONGER PARTICLE FX
       ============================================================ */

    private void createShadowAura(Player player) {
        new BukkitRunnable() {

            int t = 0;

            @Override
            public void run() {
                if (!player.isOnline() || !player.hasMetadata("ward_invisible")) {
                    cancel();
                    return;
                }

                if (t >= 200) {
                    cancel();
                    return;
                }

                Location loc = player.getLocation();
                World w = loc.getWorld();

                List<Player> viewers = new ArrayList<>();
                for (Player p : w.getPlayers()) {
                    if (p.equals(player) || plugin.getTrustManager().isTrusted(player, p))
                        viewers.add(p);
                }

                double r = 5.0;
                final double spin = t * 0.09;

                for (Player v : viewers) {

                    for (double a = 0; a < Math.PI * 2; a += 0.3) {
                        double x = Math.cos(a + spin) * r;
                        double z = Math.sin(a + spin) * r;

                        Location ring = loc.clone().add(x, 1.1, z);

                        v.spawnParticle(Particle.SMOKE, ring, 1, 0.05, 0.05, 0.05, 0.01);
                        v.spawnParticle(Particle.SCULK_SOUL, ring, 1, 0.01, 0.01, 0.01, 0.03);
                    }

                    double y = 0.3 + (t % 20) * 0.05;
                    if (y > 1.8) y = 0.3;

                    v.spawnParticle(Particle.SCULK_SOUL,
                            loc.clone().add(0, y, 0),
                            6, 0.12, 0.12, 0.12, 0.03);
                }

                t += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void createSensorAnimation(final ArmorStand sensor, final Location loc) {
        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (sensor.isDead()) {
                    cancel();
                    return;
                }

                World w = loc.getWorld();

                double base = 3.0 + 0.5 * Math.sin(t * 0.13);

                for (double a = 0; a < Math.PI * 2; a += 0.25) {
                    Location l = loc.clone().add(Math.cos(a) * base, 0.12, Math.sin(a) * base);

                    w.spawnParticle(Particle.SCULK_SOUL, l, 1, 0, 0, 0, 0);
                }

                if (t % 40 == 0) {
                    w.spawnParticle(Particle.SCULK_CHARGE,
                            loc.clone().add(0, 0.5, 0),
                            12, 0.3, 0.4, 0.3, 0.12);

                    w.playSound(loc, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.4f, 1.0f);
                }

                t++;
            }

        }.runTaskTimer(plugin, 0L, 5L);
    }

    /* ============================================================
       (EVERYTHING ELSE UNTOUCHED BELOW)
       ============================================================ */

    private Location findSensorPlacement(Player player) {
        // your original method untouched
        World w = player.getWorld();
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();

        Location hit = null;

        for (int i = 1; i <= 10; i++) {
            Location check = eye.clone().add(dir.clone().multiply(i));
            Material m = check.getBlock().getType();

            if (!m.isAir() && m != Material.WATER && m != Material.LAVA) {
                hit = check.getBlock().getLocation().add(0.5, 0, 0.5);
                break;
            }
        }

        if (hit != null) return hit;

        Location end = eye.clone().add(dir.clone().multiply(10));
        Location d = end.clone();
        int minY = w.getMinHeight();

        for (int i = 0; i < 40; i++) {
            if (d.getY() < minY) break;

            Material m = d.getBlock().getType();
            if (!m.isAir() && m != Material.WATER && m != Material.LAVA) {
                break;
            }
            d.subtract(0, 1, 0);
        }

        return d.getBlock().getType().isAir() ? null : d.getBlock().getLocation().add(0.5, 0, 0.5);
    }

    @Override
    public void applyTier1Passives(Player player) {}

    @Override
    public void applyTier2Passives(Player player) {
        handleEchoSense(player);
    }

    private void activateSensor(Player owner, Location loc) {
        World w = loc.getWorld();
        for (Entity e : w.getNearbyEntities(loc, 5, 5, 5)) {
            if (!(e instanceof LivingEntity le)) continue;

            if (e instanceof Player target) {
                if (plugin.getTrustManager().isTrusted(owner, target)) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1, true, false));
                    w.spawnParticle(Particle.HEART,
                            target.getLocation().add(0, 2, 0),
                            6, 0.4, 0.4, 0.4, 0.05);

                } else {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 200, 0, true, false));
                    target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 200, 0, true, false));

                    w.spawnParticle(Particle.SMOKE,
                            target.getLocation().add(0, 1, 0),
                            12, 0.5, 0.5, 0.5, 0.05);
                    w.spawnParticle(Particle.SCULK_SOUL,
                            target.getLocation().add(0, 1.2, 0),
                            6, 0.3, 0.4, 0.3, 0.06);
                }
            } else {
                le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 200, 0, true, false));
                le.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 200, 0, true, false));
            }
        }

        w.spawnParticle(Particle.SCULK_SOUL,
                loc.clone().add(0, 0.8, 0),
                40, 2, 1.2, 2, 0.15);
        w.spawnParticle(Particle.EXPLOSION, loc, 1, 0, 0, 0, 0);
        w.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.2f, 1.2f);

        MessageUtils.sendMessage(owner, "§5👁 §lSensor Activated.");
    }

    private void handleEchoSense(Player player) {
        // untouched
        List<Player> near = LocationUtils.getPlayersInRadius(player.getLocation(), 20);

        for (Player p : near) {
            if (p.equals(player)) continue;

            boolean invis = (p.hasPotionEffect(PotionEffectType.INVISIBILITY)
                    && p.hasMetadata("ward_invisible"));
            if (invis) continue;

            if (p.isSneaking() && !p.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                if (!p.hasMetadata("echo_sense_detected")) {
                    p.setMetadata("echo_sense_detected",
                            new FixedMetadataValue(plugin, System.currentTimeMillis()));

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (!p.isOnline()) {
                                p.removeMetadata("echo_sense_detected", plugin);
                                return;
                            }
                            if (p.isSneaking() && p.hasMetadata("echo_sense_detected")) {
                                MessageUtils.sendMessage(player,
                                        "§5👁 §lEcho Sense: §7Someone is near...");
                                p.removeMetadata("echo_sense_detected", plugin);

                                player.getWorld().playSound(
                                        player.getLocation(),
                                        Sound.ENTITY_WARDEN_SONIC_BOOM,
                                        0.4f, 2f);
                                player.getWorld().spawnParticle(Particle.SCULK_SOUL,
                                        player.getLocation().add(0, 1.5, 0),
                                        10, 0.4, 0.5, 0.4, 0.06);
                            }
                        }
                    }.runTaskLater(plugin, 200);
                }
            } else {
                p.removeMetadata("echo_sense_detected", plugin);
            }
        }
    }

    public void handleSensorDamage(final ArmorStand sensor, final Player attacker) {
        if (!sensor.hasMetadata("ward_sensor")) return;

        int hp = sensor.getMetadata("ward_sensor_hp").get(0).asInt();
        hp--;

        World w = sensor.getWorld();
        Location loc = sensor.getLocation();

        if (hp <= 0) {
            UUID ownerId = (UUID) sensor.getMetadata("ward_sensor").get(0).value();
            Player owner = plugin.getServer().getPlayer(ownerId);

            if (owner != null) {
                owner.removeMetadata("ward_sensor_active", plugin);
                MessageUtils.sendMessage(owner, "§5👁 §cYour sensor was destroyed!");
            }

            w.spawnParticle(Particle.BLOCK_CRUMBLE,
                    loc, 20, 0.5, 0.5, 0.5, 0.1,
                    Material.SCULK_SENSOR.createBlockData());
            w.playSound(loc, Sound.BLOCK_SCULK_SENSOR_BREAK, 1.2f, 0.9f);

            loc.getBlock().setType(Material.AIR);
            sensor.remove();
        } else {
            sensor.setMetadata("ward_sensor_hp", new FixedMetadataValue(plugin, hp));
            sensor.setCustomName("§5Ward Sensor §c(" + hp + " HP)");

            w.spawnParticle(Particle.CRIT,
                    loc.clone().add(0, 1, 0),
                    10, 0.3, 0.3, 0.3, 0.1);
            w.playSound(loc, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.7f, 1.6f);
        }
    }
}
