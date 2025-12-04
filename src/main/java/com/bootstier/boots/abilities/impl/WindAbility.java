package com.bootstier.boots.abilities.impl;

import com.bootstier.BootsTierPlugin;
import com.bootstier.boots.abilities.BootAbility;
import com.bootstier.boots.BootType;
import com.bootstier.boots.BootsData;
import com.bootstier.player.PlayerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;

public class WindAbility implements BootAbility {

    private final BootsTierPlugin plugin;

    public WindAbility(final BootsTierPlugin plugin) {
        this.plugin = plugin;
    }

    /* ---------------------------------------------------
       DRAGON EGG CHECK
    --------------------------------------------------- */
    private boolean hasDragonEgg(Player p) {
        return p.getInventory().contains(Material.DRAGON_EGG);
    }

    private long getTotalCD(Player p, int tier) {
        PlayerData pd = plugin.getPlayerManager().getPlayerData(p);
        BootType type = pd.getBootsData().getBootType();
        return plugin.getActionBarManager().getTotalAbilityCooldown(type, tier, hasDragonEgg(p));
    }

    /* ---------------------------------------------------
       COOLDOWN SETTER (REAL SYSTEM!)
    --------------------------------------------------- */
    private void applyCooldown(Player p, int tier) {
        PlayerData pd = plugin.getPlayerManager().getPlayerData(p);
        BootsData bd = pd.getBootsData();
        long now = System.currentTimeMillis();

        if (tier == 1) {
            bd.setLastAbilityUse(now);
        } else {
            bd.setLastTier2AbilityUse(now);
        }

        plugin.getPlayerManager().savePlayerData(pd);
    }

    /* ---------------------------------------------------
       TIER 1 — DASH
    --------------------------------------------------- */
    @Override
    public boolean executeTier1(final Player player) {

        applyCooldown(player, 1);

        Vector dir = player.getLocation().getDirection().normalize();
        Vector dash = dir.multiply(1.4);
        dash.setY(0.35);
        player.setVelocity(dash);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 10) {
                    cancel();
                    return;
                }
                Location loc = player.getLocation().subtract(dir.clone().multiply(ticks * 0.25));
                player.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, loc, 6, 0.2, 0.1, 0.2, 0.05);
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    /* ---------------------------------------------------
       TIER 2 — TORNADO
    --------------------------------------------------- */
    @Override
    public boolean executeTier2(final Player player) {

        applyCooldown(player, 2);

        final Location center = player.getLocation();
        final ArmorStand tornado = plugin.getUnifiedDisplayManager().createTornado(center, 200);

        player.playSound(center, org.bukkit.Sound.ITEM_ELYTRA_FLYING, 1.5f, 0.6f);
        player.sendMessage("§b🌪 §lTornado! §7A vortex forms around you!");

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 200 || tornado.isDead()) {
                    tornado.remove();
                    cancel();
                    return;
                }

                handleTornadoParticles(center, ticks);

                List<Entity> ents = center.getWorld()
                        .getNearbyEntities(center, 8, 8, 8)
                        .stream()
                        .filter(e -> e instanceof LivingEntity)
                        .filter(e -> !e.equals(player))
                        .toList();

                for (Entity e : ents) {
                    LivingEntity l = (LivingEntity) e;

                    if (l instanceof Player p &&
                            plugin.getTrustManager().isTrusted(player, p))
                        continue;

                    Vector pull = center.toVector().subtract(l.getLocation().toVector());
                    if (pull.lengthSquared() < 0.001) continue;

                    pull.normalize().multiply(0.3);
                    pull.setY(0.15);

                    l.setVelocity(l.getVelocity().add(pull));
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 5L);

        return true;
    }

    private void handleTornadoParticles(Location center, int ticks) {
        double speed = ticks * 0.15;

        for (double r = 0.5; r <= 4.0; r += 0.8) {
            double offset = speed + (r * 0.3);
            for (double ang = 0; ang < 2*Math.PI; ang += 0.35) {
                double x = Math.cos(ang + offset) * r;
                double z = Math.sin(ang + offset) * r;
                double y = 0.5 + ((ticks % 20) * 0.15);

                center.getWorld().spawnParticle(
                        org.bukkit.Particle.CLOUD,
                        center.clone().add(x, y, z),
                        1, 0.1, 0.1, 0.1, 0.01
                );
            }
        }
    }

    /* ---------------------------------------------------
       PASSIVES
    --------------------------------------------------- */
    @Override
    public void applyTier1Passives(Player p) {}

    @Override
    public void applyTier2Passives(Player p) {}

    /* ---------------------------------------------------
       DOUBLE TAP DOUBLE JUMP
    --------------------------------------------------- */
    private static final long DOUBLE_TAP_WINDOW = 300;

    public void handleDoubleJump(Player player) {

        long now = System.currentTimeMillis();

        if (!player.hasMetadata("wind_jump_tap")) {
            player.setMetadata("wind_jump_tap", new FixedMetadataValue(plugin, now));
            return;
        }

        long lastTap = player.getMetadata("wind_jump_tap").get(0).asLong();
        player.setMetadata("wind_jump_tap", new FixedMetadataValue(plugin, now));

        if (now - lastTap > DOUBLE_TAP_WINDOW) return;
        if (player.isOnGround()) return;
        if (player.hasMetadata("wind_jumped")) return;

        Vector v = player.getLocation().getDirection().multiply(0.25);
        v.setY(0.95);
        player.setVelocity(v);

        player.getWorld().spawnParticle(
                org.bukkit.Particle.CLOUD,
                player.getLocation(),
                20, 0.4, 0.2, 0.4, 0.1
        );

        player.setMetadata("wind_jumped", new FixedMetadataValue(plugin, true));
    }

    public void resetDoubleJump(Player p) {
        p.removeMetadata("wind_jumped", plugin);
    }
}
