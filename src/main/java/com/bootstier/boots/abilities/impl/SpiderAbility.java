package com.bootstier.boots.abilities.impl;

import com.bootstier.BootsTierPlugin;
import com.bootstier.boots.BootType;
import com.bootstier.boots.abilities.BootAbility;
import com.bootstier.effects.ActionBarManager;
import com.bootstier.utils.MessageUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class SpiderAbility implements BootAbility {

    private final BootsTierPlugin plugin;
    private final Random random = new Random();

    // REAL cooldown maps (use ActionBarManager durations)
    private final Map<UUID, Long> tier1Cooldown = new HashMap<>();
    private final Map<UUID, Long> tier2Cooldown = new HashMap<>();

    public SpiderAbility(BootsTierPlugin plugin) {
        this.plugin = plugin;
    }

    /* ============================================================
                     DRAGON-EGG COOLDOWN SUPPORT
       ============================================================ */

    private long getRealCooldown(Player player, int tier) {
        ActionBarManager ab = plugin.getActionBarManager();
        boolean egg = hasDragonEgg(player);

        return ab.getTotalAbilityCooldown(BootType.SPIDER, tier, egg);
    }

    private boolean hasDragonEgg(Player p) {
        for (ItemStack item : p.getInventory().getContents()) {
            if (item != null && item.getType() == Material.DRAGON_EGG) return true;
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
                       TIER 1 – SPIDER HORDE
       ============================================================ */

    @Override
    public boolean executeTier1(final Player player) {

        if (isOnCooldown(tier1Cooldown, player)) {
            player.sendMessage("§cSpider Swarm on cooldown: §a" + (getRemaining(tier1Cooldown, player) / 1000) + "s");
            return false;
        }

        long realCD = getRealCooldown(player, 1);

        Location base = player.getLocation();
        List<CaveSpider> spiders = new ArrayList<>();

        base.getWorld().playSound(base, Sound.ENTITY_SPIDER_AMBIENT, 1, 0.6f);
        base.getWorld().spawnParticle(Particle.SMOKE, base, 20, 1, 0.2, 1, 0.02);

        for (int i = 0; i < 5; i++) {

            double angle = (2 * Math.PI * i) / 5;
            Location loc = base.clone().add(Math.cos(angle) * 2.5, 0, Math.sin(angle) * 2.5);

            CaveSpider spider = (CaveSpider) base.getWorld().spawnEntity(loc, EntityType.CAVE_SPIDER);
            spider.setCustomName("§2" + player.getName() + "'s Spider");
            spider.setCustomNameVisible(true);

            spider.setMetadata("spider_owner", new FixedMetadataValue(plugin, player.getUniqueId()));
            spider.setMetadata("spider_summoned", new FixedMetadataValue(plugin, true));

            spider.setRemoveWhenFarAway(false);
            spider.setTarget(null);

            // TARGETING LOGIC:
            // - Do NOT attack owner
            // - Do NOT attack trusted players
            // - Attack ALL mobs (villagers, iron golems, animals, monsters)
            // - Attack UNTRUSTED players
            // - Ignore other friendly summoned spiders

            new BukkitRunnable() {

                @Override
                public void run() {

                    if (!player.isOnline() || spider.isDead()) {
                        cancel();
                        return;
                    }

                    LivingEntity best = null;
                    Location sl = spider.getLocation();

                    for (Entity e : sl.getWorld().getNearbyEntities(sl, 16, 16, 16)) {

                        if (!(e instanceof LivingEntity)) continue;
                        LivingEntity le = (LivingEntity) e;

                        // owner protection
                        if (le.equals(player)) continue;

                        // no friendly fire to other summoned spiders
                        if (le instanceof CaveSpider && le.hasMetadata("spider_summoned")) continue;

                        // PLAYERS LOGIC
                        if (le instanceof Player) {
                            Player target = (Player) le;

                            // skip trusted
                            if (plugin.getTrustManager().isTrusted(player, target)) continue;

                            // untrusted -> priority target
                            best = target;
                            break;
                        }

                        // MOBS (ALL mobs except your own spiders)
                        if (best == null) best = le;
                    }

                    spider.setTarget(best);
                }

            }.runTaskTimer(plugin, 0, 20);

            createSpiderAura(spider);
            spiders.add(spider);
        }

        // despawn after 20s
        new BukkitRunnable() {
            @Override
            public void run() {
                for (CaveSpider spider : spiders) {
                    if (!spider.isDead()) {
                        spider.setTarget(null);
                        spider.getWorld().spawnParticle(Particle.ITEM_SLIME, spider.getLocation(), 10, 0.5, 0.5, 0.5, 0.1);
                        spider.getWorld().playSound(spider.getLocation(), Sound.ENTITY_SPIDER_DEATH, 0.7f, 1.2f);
                        spider.remove();
                    }
                }
            }
        }.runTaskLater(plugin, 20 * 20);

        MessageUtils.sendMessage(player, "§2🕷 §lSpider Swarm! §7Your spiders rise!");

        applyCooldown(tier1Cooldown, player, realCD);
        return true;
    }

    private void createSpiderAura(final CaveSpider spider) {
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (spider.isDead() || ticks >= 400) {
                    cancel();
                    return;
                }

                Location loc = spider.getLocation().add(0, 0.5, 0);
                spider.getWorld().spawnParticle(Particle.ITEM_SLIME, loc, 2, 0.3, 0.3, 0.3, 0.02);

                if (ticks % 40 == 0) {
                    for (double angle = 0; angle < 2 * Math.PI; angle += Math.PI / 2) {
                        Location f = spider.getLocation().add(Math.cos(angle) * 0.8, 0.1, Math.sin(angle) * 0.8);
                        spider.getWorld().spawnParticle(Particle.CRIT, f, 1, 0, 0, 0, 0);
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    /* ============================================================
                       TIER 2 – WEB FIREBALL
       ============================================================ */

    @Override
    public boolean executeTier2(Player player) {

        if (isOnCooldown(tier2Cooldown, player)) {
            player.sendMessage("§cWeb Fireball on cooldown: §a" +
                    (getRemaining(tier2Cooldown, player) / 1000) + "s");
            return false;
        }

        long realCD = getRealCooldown(player, 2);

        Fireball fb = player.launchProjectile(Fireball.class);
        fb.setMetadata("spider_web_fireball", new FixedMetadataValue(plugin, player.getUniqueId()));
        fb.setYield(0);
        fb.setIsIncendiary(false);
        fb.setDirection(player.getLocation().getDirection().multiply(1.2));

        player.playSound(player.getLocation(), Sound.ENTITY_GHAST_SHOOT, 0.8f, 1.5f);
        player.spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0),
                10, 0.3, 0.3, 0.3, 0.02);

        MessageUtils.sendMessage(player, "§2🕷 §lWeb Fireball!");

        applyCooldown(tier2Cooldown, player, realCD);
        return true;
    }

    /* ============================================================
                    FIREBALL IMPACT (unchanged)
       ============================================================ */

    public void handleFireballImpact(Location location, Player owner) {
        if (location == null || owner == null || location.getWorld() == null) return;

        List<Location> webs = new ArrayList<>();

        for (int x = -3; x <= 3; x++)
            for (int y = -1; y <= 2; y++)
                for (int z = -3; z <= 3; z++) {

                    if (Math.sqrt(x * x + z * z) > 3) continue;
                    if (random.nextDouble() >= 0.4) continue;

                    Location wl = location.clone().add(x, y, z);

                    if (!wl.getBlock().getType().isAir()) continue;

                    boolean nearTrusted = false;
                    for (Player p : wl.getWorld().getPlayers()) {
                        if (plugin.getTrustManager().isTrusted(owner, p) &&
                                p.getLocation().distanceSquared(wl) < 2.25) {
                            nearTrusted = true;
                            break;
                        }
                    }
                    if (nearTrusted) continue;

                    wl.getBlock().setType(Material.COBWEB);
                    webs.add(wl);
                }

        location.getWorld().spawnParticle(
                Particle.ITEM, location, 30,
                1.5, 1.5, 1.5, 0.1, new ItemStack(Material.COBWEB));

        location.getWorld().spawnParticle(Particle.EXPLOSION, location, 1);
        location.getWorld().playSound(location, Sound.ENTITY_SPIDER_HURT, 1, 0.8f);

        // shimmer
        new BukkitRunnable() {
            int ticks = 0;
            public void run() {
                if (ticks++ >= 5) { cancel(); return; }
                for (Location wl : webs)
                    wl.getWorld().spawnParticle(
                            Particle.WHITE_ASH, wl.clone().add(0.5,0.5,0.5),
                            3, 0.3,0.3,0.3, 0.01);
            }
        }.runTaskTimer(plugin, 0, 1);

        // remove webs after 15s
        new BukkitRunnable() {
            public void run() {
                for (Location wl : webs) {
                    if (wl.getBlock().getType() == Material.COBWEB) {
                        wl.getBlock().setType(Material.AIR);
                        wl.getWorld().spawnParticle(Particle.CLOUD, wl.clone().add(0.5,0.5,0.5),
                                5, 0.3,0.3,0.3,0.05);
                    }
                }
                if (owner.isOnline())
                    MessageUtils.sendMessage(owner, "§2🕷 §7Your webs dissolve...");
            }
        }.runTaskLater(plugin, 20 * 15);

        MessageUtils.sendMessage(owner,
                "§2🕷 §lWeb Trap! §7" + webs.size() + " webs created!");
    }

    /* ============================================================
                PASSIVE — Natural spider behavior override
       ============================================================ */

    public boolean canSpiderAttack(CaveSpider spider, Player target) {

        if (!spider.hasMetadata("spider_summoned")) return true;

        Player owner = plugin.getServer().getPlayer(
                (UUID) spider.getMetadata("spider_owner").get(0).value()
        );

        if (owner == null) return true;

        // NEVER attack: owner or trusted players
        return !target.equals(owner) &&
                !plugin.getTrustManager().isTrusted(owner, target);
    }

    @Override
    public void applyTier1Passives(Player p) {}

    @Override
    public void applyTier2Passives(Player p) {}
}
