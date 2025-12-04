package com.bootstier.nms;

import com.bootstier.BootsTierPlugin;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Handles NMS + packet-level effects for advanced visuals and trust visibility.
 * Now delegates invisibility to PacketManager for safe 1.21.4 compatibility.
 */
public class NMSHandler {

    private final BootsTierPlugin plugin;

    public NMSHandler(final BootsTierPlugin plugin) {
        this.plugin = plugin;
        plugin.getLogger().info("§7[NMS] Handler active (using PacketManager for visibility).");
    }

    /* --------------------------------------------------
       🔒 TRUSTED VISIBILITY SYSTEM
    -------------------------------------------------- */

    /**
     * TRUE invisibility to *untrusted* players only.
     * - invisible = true  → hidden from untrusted players (armor + entity)
     * - invisible = false → visible again to everyone
     *
     * Trusted players ALWAYS see the target.
     */
    public void setInvisibleToUntrusted(final Player target, final boolean invisible) {
        if (invisible) {
            plugin.getPacketManager().hidePlayerFromUntrusted(target);
        } else {
            plugin.getPacketManager().showPlayerToAll(target);
        }
    }

    /* --------------------------------------------------
       ⚡ VISUAL EFFECTS
    -------------------------------------------------- */

    public void sendLightningEffect(final Player target) {
        try {
            target.getWorld().strikeLightningEffect(target.getLocation());
        } catch (Exception e) {
            plugin.getLogger().warning("[NMS] Lightning effect failed: " + e.getMessage());
        }
    }

    public void setGlowing(final Player player, final boolean glowing) {
        try {
            player.setGlowing(glowing);
        } catch (Exception e) {
            plugin.getLogger().warning("[NMS] Failed to toggle glow: " + e.getMessage());
        }
    }

    public void sendCustomParticles(final Player player, final Location location, final String particleType, final int count) {
        try {
            Particle particle = Particle.valueOf(particleType.toUpperCase());
            player.spawnParticle(particle, location, count, 0.3, 0.3, 0.3, 0.01);
        } catch (Exception e) {
            plugin.getLogger().warning("[NMS] Particle error: " + e.getMessage());
        }
    }

    public void createCustomEntity(final Location location, final String entityType) {
        try {
            location.getWorld().spawnEntity(location, EntityType.valueOf(entityType.toUpperCase()));
        } catch (Exception e) {
            plugin.getLogger().warning("[NMS] Entity spawn error: " + e.getMessage());
        }
    }

    /* --------------------------------------------------
       🧊 PLAYER CONTROL
    -------------------------------------------------- */

    public void disablePlayerMovement(final Player player, final int durationTicks) {
        try {
            player.setVelocity(player.getVelocity().zero());
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationTicks, 10, false, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, durationTicks, 250, false, false, false));
        } catch (Exception e) {
            plugin.getLogger().warning("[NMS] Freeze failed: " + e.getMessage());
        }
    }

    public void sendScreenEffect(final Player player, final Sound sound, final Particle particle) {
        try {
            Location loc = player.getLocation();
            player.playSound(loc, sound, 1.0f, 1.0f);
            player.spawnParticle(particle, loc, 20, 1, 1, 1, 0.1);
        } catch (Exception e) {
            plugin.getLogger().warning("[NMS] Screen effect failed: " + e.getMessage());
        }
    }

    /* --------------------------------------------------
       🧪 DEBUG TEST
    -------------------------------------------------- */

    public void debugTest(Player player) {
        player.sendMessage("§7[NMS Test] Running advanced visibility diagnostics...");
        sendLightningEffect(player);
        setGlowing(player, true);
        sendCustomParticles(player, player.getLocation(), "END_ROD", 30);
        player.sendMessage("§aDone!");
    }
}
