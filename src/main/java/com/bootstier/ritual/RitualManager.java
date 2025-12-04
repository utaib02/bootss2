package com.bootstier.ritual;

import com.bootstier.BootsTierPlugin;
import com.bootstier.player.PlayerData;
import com.bootstier.utils.MessageUtils;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class RitualManager {

    private final BootsTierPlugin plugin;
    private final Map<UUID, ActiveRitual> rituals = new HashMap<>();
    private ActiveRitual currentRitual = null; // ONLY ONE RITUAL GLOBAL

    public RitualManager(BootsTierPlugin plugin) {
        this.plugin = plugin;
    }

    /* =====================================================================
       START RITUAL
       Only ONE ritual may exist at a time
    ===================================================================== */
    public void startRitual(Player player) {

        if (currentRitual != null) {
            player.sendMessage("§cSomeone else is already performing a ritual! Break theirs first.");
            return;
        }

        Location beacon = plugin.getPedestalManager().getPedestalLocation();
        if (beacon == null) {
            player.sendMessage(ChatColor.RED + "Pedestal location not set!");
            return;
        }

        PlayerData data = plugin.getPlayerManager().getPlayerData(player);

        // DO NOT BLOCK SHARD ADDING — removed old bug
        if (data.getBootsData() == null) {
            player.sendMessage("§cYou don't even have boots to restore!");
            return;
        }

        ActiveRitual ritual = new ActiveRitual(plugin, player, beacon.clone());
        this.currentRitual = ritual;
        this.rituals.put(player.getUniqueId(), ritual);

        ritual.begin();
    }

    /* =====================================================================
       ADD SHARD
    ===================================================================== */
    public void addShardToRitual(Player player) {
        if (currentRitual == null) {
            player.sendMessage("§cThere is no ritual running!");
            return;
        }

        if (!currentRitual.isOwner(player)) {
            player.sendMessage("§cYou are not the ritual owner!");
            return;
        }

        currentRitual.addShard();
    }

    /* =====================================================================
       DAMAGE BEACON
    ===================================================================== */
    public void damageBeacon(Player attacker) {
        if (currentRitual == null) return;

        if (!currentRitual.isOwner(attacker)
                && currentRitual.isBeacon(attacker.getLocation())) {
            currentRitual.hitByEnemy(attacker);
        }
    }

    public void damageBeaconAt(Location loc, Player attacker) {
        if (currentRitual == null) return;

        if (!currentRitual.isOwner(attacker)
                && currentRitual.isBeacon(loc)) {
            currentRitual.hitByEnemy(attacker);
        }
    }

    /* =====================================================================
       CLEANUP
    ===================================================================== */
    public void cleanupPlayer(Player player) {
        if (currentRitual != null && currentRitual.isOwner(player)) {
            currentRitual.cancel("§c§lRitual cancelled - player disconnected");
        }
    }

    public boolean hasActiveRitual(Player player) {
        return currentRitual != null;
    }

    public void cleanupAll() {
        if (currentRitual != null) {
            currentRitual.cleanup();
        }
        currentRitual = null;
        rituals.clear();
    }

    /* =====================================================================
       INNER CLASS — RITUAL LOGIC
    ===================================================================== */
    private static class ActiveRitual {

        private final BootsTierPlugin plugin;
        private final Player owner;
        private final Location beaconLoc;

        private ArmorStand hologram;

        private int lives = 3;
        private int progress = 0;
        private long lastShard = 0;
        private long startTime;

        private BukkitRunnable timerTask;
        private BukkitRunnable particleTask;

        ActiveRitual(BootsTierPlugin plugin, Player owner, Location loc) {
            this.plugin = plugin;
            this.owner = owner;
            this.beaconLoc = loc;
        }

        /* -------------------------------------------------------------- */
        void begin() {

            World w = beaconLoc.getWorld();
            if (w == null) return;

            startTime = System.currentTimeMillis();
            w.getBlockAt(beaconLoc).setType(Material.BEACON);

            spawnHologram();

            MessageUtils.broadcast("§5§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            MessageUtils.broadcast("§d✦ §e" + owner.getName() + " §7has begun the §d§lRitual of Restoration§7!");
            MessageUtils.broadcast("§7Add §e5 Boot Shards §7(one per minute).");
            MessageUtils.broadcast("§7Progress: §a0§8/§e5");
            MessageUtils.broadcast("§5§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            owner.sendTitle("§d§lRITUAL BEGUN", "§7Right-click with a shard to start.", 10, 60, 10);
            owner.sendMessage("§7Add §e1 shard per minute.§7 Total needed: §e5§7.");

            startParticles();
            startTimer();
        }

        /* -------------------------------------------------------------- */
        void spawnHologram() {
            Location holoLoc = beaconLoc.clone().add(0.5, 2.2, 0.5);

            hologram = beaconLoc.getWorld().spawn(holoLoc, ArmorStand.class, stand -> {
                stand.setGravity(false);
                stand.setVisible(false);
                stand.setMarker(true);
                stand.setCustomNameVisible(true);
                stand.setCustomName("§d§lRitual of Restoration\n§7Owner: §e" + owner.getName() +
                        "\n§7Progress: §a0/5");
            });
        }

        void updateHologram() {
            if (hologram != null) {
                hologram.setCustomName("§d§lRitual of Restoration\n§7Owner: §e" + owner.getName()
                        + "\n§7Progress: §a" + progress + "§7/§e5");
            }
        }

        /* -------------------------------------------------------------- */
        void addShard() {

            long now = System.currentTimeMillis();

            if (progress > 0 && now - lastShard < 60000) {
                long remaining = (60000 - (now - lastShard)) / 1000;
                owner.sendMessage("§cWait " + remaining + " sec before adding the next shard!");
                return;
            }

            if (owner.getLocation().distance(beaconLoc) > 5) {
                owner.sendMessage("§cStay within 5 blocks of the beacon.");
                return;
            }

            if (!removeShard()) {
                owner.sendMessage("§cYou don't have any Boot Shards!");
                return;
            }

            progress++;
            lastShard = now;

            updateHologram();

            World w = beaconLoc.getWorld();
            w.spawnParticle(Particle.TOTEM_OF_UNDYING, beaconLoc.clone().add(0.5, 1.3, 0.5),
                    40, 0.4, 0.6, 0.4, 0.1);

            owner.playSound(owner.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.7f);

            MessageUtils.broadcast("§d✦ §e" + owner.getName() + " §7added a shard! §a" + progress + "§8/§e5");

            if (progress == 5) {
                finishPhaseStart();
            }
        }

        /* -------------------------------------------------------------- */
        boolean removeShard() {
            var inv = owner.getInventory();
            for (int i = 0; i < inv.getSize(); i++) {
                var item = inv.getItem(i);
                if (plugin.getCustomItemManager().isBootShardItem(item)) {
                    if (item.getAmount() > 1) item.setAmount(item.getAmount() - 1);
                    else inv.setItem(i, null);
                    return true;
                }
            }
            return false;
        }

        /* -------------------------------------------------------------- */
        void finishPhaseStart() {
            owner.sendMessage("§a§lAll shards added! §7Stand on the beacon to complete the ritual!");

            World w = beaconLoc.getWorld();
            w.playSound(beaconLoc, Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.5f);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (owner.getLocation().distance(beaconLoc) < 2) {
                        complete();
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 0L, 10L);
        }

        /* -------------------------------------------------------------- */
        void startParticles() {
            particleTask = new BukkitRunnable() {
                int tick = 0;

                @Override
                public void run() {
                    Location center = beaconLoc.clone().add(0.5, 1.5, 0.5);
                    World w = center.getWorld();

                    double radius = 5.0;
                    double angle = (tick * 0.15) % (Math.PI * 2); // Smooth rotation
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;

                    Location particleLoc = center.clone().add(x, 0.2, z);
                    w.spawnParticle(Particle.END_ROD, particleLoc, 1, 0.0, 0.0, 0.0, 0.0);

                    w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(x, 0, z), 2, 0.1, 0.1, 0.1, 0.0);
                    w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(x, 1, z), 2, 0.1, 0.1, 0.1, 0.0);

                    tick++;
                }
            };
            particleTask.runTaskTimer(plugin, 0L, 2L);
        }

        /* -------------------------------------------------------------- */
        void startTimer() {
            timerTask = new BukkitRunnable() {
                private long lastMessage = 0;

                @Override
                public void run() {
                    long remaining = 600000 - (System.currentTimeMillis() - startTime);

                    if (remaining <= 0) {
                        fail("§cTime ran out!");
                        return;
                    }

                    if (owner.getLocation().distance(beaconLoc) > 50) {
                        fail("§cYou left the ritual area!");
                        return;
                    }

                    long now = System.currentTimeMillis();
                    if (now - lastMessage >= 10000) {
                        owner.sendMessage("§dRitual Progress: §a" + progress + "/5"
                                + " §7| Lives: §c" + lives + " §7| Time Left: §b"
                                + (remaining / 1000) + "s");
                        lastMessage = now;
                    }
                }
            };
            timerTask.runTaskTimer(plugin, 0L, 20L);
        }

        /* -------------------------------------------------------------- */
        boolean isOwner(Player p) {
            return p.getUniqueId().equals(owner.getUniqueId());
        }

        boolean isBeacon(Location loc) {
            return loc.getWorld().equals(beaconLoc.getWorld())
                    && loc.distance(beaconLoc) < 2.3;
        }

        /* -------------------------------------------------------------- */
        void hitByEnemy(Player enemy) {
            lives--;

            World w = beaconLoc.getWorld();
            w.spawnParticle(Particle.EXPLOSION,
                    beaconLoc.clone().add(0.5, 1, 0.5),
                    1);

            enemy.sendMessage("§cYou damaged the ritual!");

            if (lives <= 0) {
                fail("§cYour ritual was destroyed!");
                return;
            }

            owner.sendTitle("§c§lBEACON HIT!", "§7Lives: §c" + lives + "/3", 10, 30, 10);
        }

        /* -------------------------------------------------------------- */
        void complete() {
            cleanup();

            PlayerData data = plugin.getPlayerManager().getPlayerData(owner);
            data.repairBoots(5);
            plugin.getPlayerManager().savePlayerData(data);
            plugin.getBootsManager().repairBoots(owner);

            World w = beaconLoc.getWorld();

            w.spawnParticle(Particle.EXPLOSION_EMITTER,
                    beaconLoc.clone().add(0.5, 1, 0.5), 1);

            w.playSound(beaconLoc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);

            MessageUtils.broadcast("§a§l✔ §e" + owner.getName() +
                    " §7completed the §d§lRitual of Restoration§7!");

            owner.sendMessage("§aYour boots now have §e5 lives§a!");
        }

        /* -------------------------------------------------------------- */
        void fail(String reason) {
            cleanup();
            owner.sendMessage(reason);
            owner.getWorld().dropItemNaturally(beaconLoc,
                    plugin.getCustomItemManager().createRepairBoxItem());
        }

        /* -------------------------------------------------------------- */
        void cancel(String reason) {
            cleanup();
            owner.sendMessage(reason);
        }

        /* -------------------------------------------------------------- */
        void cleanup() {
            if (timerTask != null) {
                timerTask.cancel();
                timerTask = null;
            }
            if (particleTask != null) {
                particleTask.cancel();
                particleTask = null;
            }

            if (hologram != null && !hologram.isDead()) hologram.remove();

            if (beaconLoc.getBlock().getType() == Material.BEACON)
                beaconLoc.getBlock().setType(Material.AIR);

            plugin.getUnifiedDisplayManager().clearPlayerDisplays(owner);

            plugin.getRitualManager().currentRitual = null;
        }
    }
}
