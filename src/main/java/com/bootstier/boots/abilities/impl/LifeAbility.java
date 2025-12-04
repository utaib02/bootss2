package com.bootstier.boots.abilities.impl;

import com.bootstier.BootsTierPlugin;
import com.bootstier.boots.abilities.BootAbility;
import org.bukkit.*;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class LifeAbility implements BootAbility {

    private final BootsTierPlugin plugin;

    private final Map<UUID, Long> tier1Cooldown = new HashMap<>();
    private final Map<UUID, Long> tier2Cooldown = new HashMap<>();

    private static final long TIER1_CD_MS = 120_000L; // 2 minutes
    private static final long TIER2_CD_MS = 75_000L;  // 1:15

    public LifeAbility(final BootsTierPlugin plugin) {
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

    private long getCooldownLeft(Map<UUID, Long> map, UUID id) {
        return Math.max(0L, map.getOrDefault(id, 0L) - System.currentTimeMillis());
    }

    private void setCooldown(Map<UUID, Long> map, Player p, long base) {
        boolean egg = hasDragonEgg(p);
        long effective = egg ? base / 2 : base;
        map.put(p.getUniqueId(), System.currentTimeMillis() + effective);
    }

    /* ==========================================================
       TIER 1 – LIFE DRAIN
       ========================================================== */

    @Override
    public boolean executeTier1(final Player player) {
        UUID id = player.getUniqueId();

        if (isOnCooldown(tier1Cooldown, id)) {
            long left = getCooldownLeft(tier1Cooldown, id) / 1000L;
            player.sendMessage(ChatColor.RED + "Life Drain on cooldown: "
                    + ChatColor.GREEN + left + "s");
            return false;
        }

        player.setMetadata("life_heal_ready", new FixedMetadataValue(plugin, true));

        Location loc = player.getLocation();
        World w = loc.getWorld();

        if (w != null) {
            w.spawnParticle(Particle.HEART, loc.clone().add(0, 2.0, 0), 25, 0.7, 0.7, 0.7, 0.15);
            w.spawnParticle(Particle.HAPPY_VILLAGER, loc.clone().add(0, 1.2, 0), 15, 0.5, 0.5, 0.5, 0.1);
            w.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.9f, 1.6f);
        }

        player.sendMessage(ChatColor.GREEN + "💚 " + ChatColor.BOLD + "Life Drain primed!");

        // REAL COOLDOWN
        setCooldown(tier1Cooldown, player, TIER1_CD_MS);
        return true;
    }

    public void handlePlayerHit(final Player attacker, final Player victim) {
        if (!attacker.hasMetadata("life_heal_ready")) return;

        if (plugin.getTrustManager().isTrusted(attacker, victim)) return;

        attacker.removeMetadata("life_heal_ready", plugin);

        double victimHearts = victim.getHealth() / 2.0;
        double healHearts = 10.0 - victimHearts;
        double healAmount = Math.max(0, healHearts * 2.0);

        attacker.setHealth(Math.min(attacker.getMaxHealth(), attacker.getHealth() + healAmount));

        victim.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 0));
        victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1));

        World w = attacker.getWorld();
        Location aLoc = attacker.getLocation();
        Location vLoc = victim.getLocation();

        w.spawnParticle(Particle.HEART, aLoc.clone().add(0, 2.0, 0), 20, 0.6, 0.6, 0.6, 0.15);
        w.spawnParticle(Particle.CHERRY_LEAVES, aLoc.clone().add(0, 1.0, 0), 15, 0.3, 0.4, 0.3, 0.1);

        w.spawnParticle(Particle.SMOKE, vLoc.clone().add(0, 1.0, 0), 20, 0.5, 0.5, 0.5, 0.05);
    }

    /* ==========================================================
       TIER 2 – LIFE CIRCLE (Weakness Aura)
       ========================================================== */

    @Override
    public boolean executeTier2(final Player player) {
        UUID id = player.getUniqueId();

        if (isOnCooldown(tier2Cooldown, id)) {
            long left = getCooldownLeft(tier2Cooldown, id) / 1000L;
            player.sendMessage(ChatColor.RED + "Life Circle on cooldown: "
                    + ChatColor.GREEN + left + "s");
            return false;
        }

        player.setMetadata("life_circle_active", new FixedMetadataValue(plugin, true));

        Location loc = player.getLocation();
        World w = loc.getWorld();

        if (w != null) {
            w.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.2f);
            w.spawnParticle(Particle.HEART, loc.clone().add(0, 1.4, 0),
                    30, 1.0, 0.4, 1.0, 0.1);
        }

        player.sendMessage(ChatColor.GREEN + "💚 " + ChatColor.BOLD + "Blooming Sanctuary activated!");

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {

                if (!player.isOnline() || !player.hasMetadata("life_circle_active")) {
                    cleanup(player);
                    cancel();
                    return;
                }

                if (ticks >= 20 * 15) {
                    cleanup(player);
                    cancel();
                    return;
                }

                Location center = player.getLocation();
                World world = center.getWorld();
                if (world == null) return;

                double radius = 5;
                double y = center.getY() + 0.3;

                // Perfect ring
                for (double a = 0; a < Math.PI * 2; a += 0.25) {
                    double x = Math.cos(a) * radius;
                    double z = Math.sin(a) * radius;

                    Location p = new Location(world, center.getX() + x, y, center.getZ() + z);

                    world.spawnParticle(Particle.HEART, p, 1, 0, 0, 0, 0);
                    world.spawnParticle(Particle.HAPPY_VILLAGER, p, 0, 0, 0, 0, 0);
                    world.spawnParticle(Particle.END_ROD, p, 0, 0, 0, 0, 0);
                }

                // Damage logic
                for (Entity e : world.getNearbyEntities(center, radius, 3, radius)) {
                    if (!(e instanceof LivingEntity)) continue;

                    LivingEntity le = (LivingEntity) e;
                    if (le.equals(player)) continue;

                    if (le instanceof Player) {
                        Player pl = (Player) le;
                        if (plugin.getTrustManager().isTrusted(player, pl)) continue;
                    }

                    double desired = Math.min(14.0, le.getMaxHealth());
                    if (le.getHealth() > desired) le.setHealth(desired);

                    if (le instanceof Player) {
                        le.setMetadata("life_health_reduced", new FixedMetadataValue(plugin, true));
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // REAL COOLDOWN
        setCooldown(tier2Cooldown, player, TIER2_CD_MS);

        return true;
    }

    private void cleanup(Player player) {
        player.removeMetadata("life_circle_active", plugin);

        for (Player p : plugin.getServer().getOnlinePlayers()) {
            p.removeMetadata("life_health_reduced", plugin);
        }

        World w = player.getWorld();
        if (w != null) {
            w.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.0f, 1.3f);
        }
    }

    /* ==========================================================
       PASSIVES
       ========================================================== */

    @Override
    public void applyTier1Passives(final Player player) {}

    @Override
    public void applyTier2Passives(final Player player) {
        if (!player.hasPotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE)) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.HERO_OF_THE_VILLAGE, 200, 2, true, false
            ));
        }
    }

    /* ==========================================================
       ARMOR DAMAGE
       ========================================================== */

    public void handleArmorDurabilityDamage(final Player target) {
        if (!target.hasMetadata("life_health_reduced")) return;

        damageItem(target.getInventory().getHelmet());
        damageItem(target.getInventory().getChestplate());
        damageItem(target.getInventory().getLeggings());
        damageItem(target.getInventory().getBoots());
    }

    private void damageItem(ItemStack item) {
        if (item == null) return;
        short max = item.getType().getMaxDurability();
        if (max <= 0) return;
        item.setDurability((short) (item.getDurability() + 1));
    }

    /* ==========================================================
       PLANT & ANIMAL PASSIVES
       ========================================================== */

    public void handlePlantGrowth(Player player, Location loc) {
        Material type = loc.getBlock().getType();

        switch (type) {
            case WHEAT:
            case CARROTS:
            case POTATOES:
            case BEETROOTS:
                org.bukkit.block.data.Ageable age = (org.bukkit.block.data.Ageable) loc.getBlock().getBlockData();
                age.setAge(age.getMaximumAge());
                loc.getBlock().setBlockData(age);
                break;
        }

        loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                loc.clone().add(0, 1, 0), 12, 0.5, 0.5, 0.5, 0.1);
    }

    public void handleAnimalFeeding(Player player, Entity e) {
        if (!(e instanceof Animals)) return;
        Animals a = (Animals) e;

        a.setHealth(a.getMaxHealth());
        if (!a.isAdult()) a.setAdult();

        a.getWorld().spawnParticle(Particle.HEART,
                a.getLocation().add(0, 1, 0),
                6, 0.3, 0.3, 0.3, 0.1);
    }
}
