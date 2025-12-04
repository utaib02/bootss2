package com.bootstier.listeners;

import com.bootstier.BootsTierPlugin;
import com.bootstier.boots.BootType;
import com.bootstier.boots.abilities.impl.SpiderAbility;
import com.bootstier.boots.abilities.impl.WaterAbility;
import com.bootstier.player.PlayerData;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.entity.WindCharge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;

/**
 * Handles projectile events for boot abilities
 */
public class ProjectileListener implements Listener {

    private final BootsTierPlugin plugin;

    public ProjectileListener(final BootsTierPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onProjectileLaunch(final ProjectileLaunchEvent event) {
        final Projectile projectile = event.getEntity();
        
        if (!(projectile.getShooter() instanceof Player)) {
            return;
        }
        
        final Player player = (Player) projectile.getShooter();
        final PlayerData data = this.plugin.getPlayerManager().getPlayerData(player);
        
        if (data.getBootsData() == null || data.areBootsBroken()) {
            return;
        }
        
        final BootType bootType = data.getBootsData().getBootType();
        
        // Handle Wind Boots projectile modifications
        if (bootType == BootType.WIND) {
            // Wind charges fly faster and straighter
            if (projectile instanceof WindCharge) {
                projectile.setVelocity(projectile.getVelocity().multiply(1.5));
            }
        }
    }

    @EventHandler
    public void onProjectileHit(final ProjectileHitEvent event) {
        final Projectile projectile = event.getEntity();
        
        if (!(projectile.getShooter() instanceof Player)) {
            return;
        }
        
        final Player shooter = (Player) projectile.getShooter();
        final PlayerData data = this.plugin.getPlayerManager().getPlayerData(shooter);
        
        if (data.getBootsData() == null || data.areBootsBroken()) {
            return;
        }
        
        final BootType bootType = data.getBootsData().getBootType();
        
        // Handle Spider Boots fireball web creation
        if (bootType == BootType.SPIDER && projectile instanceof Fireball) {
            if (projectile.hasMetadata("spider_web_fireball")) {
                final SpiderAbility spiderAbility = (SpiderAbility) this.plugin.getAbilityManager().getAbilities().get(BootType.SPIDER);
                spiderAbility.handleFireballImpact(projectile.getLocation(), shooter);
            }
        }
        
        // Handle Water Boots trident thunder
        if (bootType == BootType.WATER && projectile instanceof Trident) {
            if (event.getHitEntity() instanceof Player) {
                final Player victim = (Player) event.getHitEntity();
                final WaterAbility waterAbility = (WaterAbility) this.plugin.getAbilityManager().getAbilities().get(BootType.WATER);
                waterAbility.handleTridentHit(shooter, victim);
            }
        }
    }
}
