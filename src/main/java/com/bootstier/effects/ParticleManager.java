package com.bootstier.effects;

import com.bootstier.BootsTierPlugin;
import com.bootstier.boots.BootType;
import com.bootstier.player.PlayerData;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.Random;

/**
 * Lightweight particle-only system (NO ItemDisplays)
 * Smooth rotating aura around the player
 */
public class ParticleManager {

    private final BootsTierPlugin plugin;
    private double angle = 0.0;
    private final Random random = new Random();

    public ParticleManager(BootsTierPlugin plugin) {
        this.plugin = plugin;
    }

    /** Update all boot particles every tick */
    public void updateAllParticles() {
        if (!plugin.getConfigManager().isParticlesEnabled()) return;

        angle += 0.15;
        if (angle >= Math.PI * 2) angle = 0;

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            updatePlayerParticles(player);
        }
    }

    /** Render particles for a single player */
    private void updatePlayerParticles(Player player) {
        PlayerData data = plugin.getPlayerManager().getPlayerData(player);

        if (data.getBootsData() == null || data.areBootsBroken())
            return;

        BootType type = data.getBootsData().getBootType();
        spawnRotatingAura(player, type);
    }

    /** Rotating particle circle based on boot type */
    private void spawnRotatingAura(Player player, BootType type) {
        Particle particle = type.getParticle();

        Location base = player.getLocation().add(0, 0.3, 0);
        double radius = plugin.getConfigManager().getParticleRadius();
        int points = 12;

        for (int i = 0; i < points; i++) {
            double a = angle + (i * (2 * Math.PI / points));
            double x = Math.cos(a) * radius;
            double z = Math.sin(a) * radius;

            Location loc = base.clone().add(x, 0, z);

            base.getWorld().spawnParticle(particle, loc, 1, 0, 0, 0, 0);
        }
    }

    /* ============================================================
       ABILITY PARTICLES (these remain unchanged)
    ============================================================ */

    public void spawnAbilityParticles(Player player, BootType bootType, String abilityType) {
        Location loc = player.getLocation().add(0, 1, 0);

        switch (bootType) {
            case SPEED:
                if ("blur".equals(abilityType)) {
                    plugin.getAdvancedEffects().createSpiralEffect(loc, 0.5, 0.5, Particle.CLOUD, 20);
                } else if ("thunder".equals(abilityType)) {
                    plugin.getAdvancedEffects().createLightningEffect(loc);
                }
                break;

            case STRENGTH:
                if ("critical".equals(abilityType)) {
                    loc.getWorld().spawnParticle(Particle.CRIT, loc, 15, 0.5, 0.5, 0.5, 0.1);
                    loc.getWorld().spawnParticle(Particle.ENCHANTED_HIT, loc, 10, 0.3, 0.3, 0.3, 0.1);
                }
                break;

            case FIRE:
                if ("rings".equals(abilityType)) {
                    plugin.getAdvancedEffects().createFireRings(loc, 3, 9.0);
                }
                break;

            case FROST:
                if ("shield".equals(abilityType)) {
                    loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 20, 1, 1, 1, 0.1);
                } else if ("ice_circle".equals(abilityType)) {
                    plugin.getAdvancedEffects().createIceCircle(loc, 10, 300);
                }
                break;

            case WIND:
                if ("tornado".equals(abilityType)) {
                    plugin.getAdvancedEffects().createTornado(loc, 200);
                }
                break;

            case WARD:
                if ("shadow".equals(abilityType)) {
                    plugin.getAdvancedEffects().createShadowAura(player, 200);
                }
                break;

            case WATER:
                if ("whirlpool".equals(abilityType)) {
                    plugin.getAdvancedEffects().createWhirlpool(loc, 5, 200);
                }
                break;

            default:
                break;
        }
    }

    public void spawnAbilityEffect(Player player, String effectType, int durationTicks) {
        if ("damage_link".equalsIgnoreCase(effectType)) {
            double radius = player.hasMetadata("damage_link_radius")
                    ? player.getMetadata("damage_link_radius").get(0).asDouble()
                    : 1.0;

            createDamageLinkRing(player, radius);
        }
    }

    private void createDamageLinkRing(Player player, double radius) {
        Location center = player.getLocation().add(0, 0.1, 0);
        int points = 16;

        for (int i = 0; i < points; i++) {
            double a = 2 * Math.PI * i / points;
            double x = Math.cos(a) * radius;
            double z = Math.sin(a) * radius;

            Location ring = center.clone().add(x, 0, z);
            center.getWorld().spawnParticle(Particle.CRIMSON_SPORE, ring, 1, 0, 0, 0, 0);
        }
    }
}
