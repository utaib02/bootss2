package com.bootstier.listeners;

import com.bootstier.BootsTierPlugin;
import com.bootstier.boots.BootType;
import com.bootstier.boots.abilities.impl.*;
import com.bootstier.player.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Handles entity damage events for boot abilities
 */
public class EntityDamageListener implements Listener {

    private final BootsTierPlugin plugin;

    public EntityDamageListener(final BootsTierPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) {
            return;
        }

        final Player attacker = (Player) event.getDamager();
        final Player victim = (Player) event.getEntity();
        
        final PlayerData attackerData = this.plugin.getPlayerManager().getPlayerData(attacker);
        final PlayerData victimData = this.plugin.getPlayerManager().getPlayerData(victim);
        
        if (attackerData.getBootsData() == null || attackerData.areBootsBroken()) {
            return;
        }

        // Handle boot-specific damage events
        final BootType attackerBootType = attackerData.getBootsData().getBootType();
        
        switch (attackerBootType) {
            case SPEED:
                final SpeedAbility speedAbility = (SpeedAbility) this.plugin.getAbilityManager().getAbilities().get(BootType.SPEED);
                speedAbility.handlePlayerHit(attacker, victim);
                break;
            case STRENGTH:
                final StrengthAbility strengthAbility = (StrengthAbility) this.plugin.getAbilityManager().getAbilities().get(BootType.STRENGTH);
                strengthAbility.handlePlayerHit(attacker, victim, event.getDamage());
                
                // Handle critical hits if ability is active
                if (attacker.hasMetadata("strength_critical_active")) {
                    event.setDamage(event.getDamage() * 1.5); // 50% more damage
                }
                break;
            case LIFE:
                final LifeAbility lifeAbility = (LifeAbility) this.plugin.getAbilityManager().getAbilities().get(BootType.LIFE);
                lifeAbility.handlePlayerHit(attacker, victim);
                lifeAbility.handleArmorDurabilityDamage(victim);
                break;
            case FIRE:
                final FireAbility fireAbility = (FireAbility) this.plugin.getAbilityManager().getAbilities().get(BootType.FIRE);
                fireAbility.handleMarkedPlayerHit(victim);
                break;
        }
        
        // Handle victim's boot passives
        if (victimData.getBootsData() != null && !victimData.areBootsBroken()) {
            final BootType victimBootType = victimData.getBootsData().getBootType();
            
            switch (victimBootType) {
                case FROST:
                    final FrostAbility frostAbility = (FrostAbility) this.plugin.getAbilityManager().getAbilities().get(BootType.FROST);
                    frostAbility.handleDamageReflection(victim, attacker, event.getDamage());
                    
                    // Increment hit counter for Tier 2 passive
                    if (victimData.getBootsData().getTier().getLevel() == 2) {
                        victimData.getBootsData().incrementHitCounter();
                        this.plugin.getPlayerManager().savePlayerData(victimData);
                    }
                    break;
                case FIRE:
                    final FireAbility fireAbility = (FireAbility) this.plugin.getAbilityManager().getAbilities().get(BootType.FIRE);
                    fireAbility.handleAttackerIgnite(attacker, victim);
                    break;
            }
        }
    }
    
    @EventHandler
    public void onEntityDamage(final EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        final Player player = (Player) event.getEntity();
        final PlayerData data = this.plugin.getPlayerManager().getPlayerData(player);
        
        if (data.getBootsData() == null || data.areBootsBroken()) {
            return;
        }
        
        final BootType bootType = data.getBootsData().getBootType();
        
        // Handle boot-specific damage prevention
        switch (bootType) {
            case WIND:
                // No fall damage
                if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                    event.setCancelled(true);
                }
                break;
            case ASTRAL:
                // 10% chance to cancel damage
                final AstralAbility astralAbility = (AstralAbility) this.plugin.getAbilityManager().getAbilities().get(BootType.ASTRAL);
                if (astralAbility.handleDamageCancellation(player)) {
                    event.setCancelled(true);
                }
                break;
            case FROST:
                // Immune to freezing damage
                if (event.getCause() == EntityDamageEvent.DamageCause.FREEZE) {
                    event.setCancelled(true);
                }
                break;
            case WATER:
                // Fire immunity when wet
                final WaterAbility waterAbility = (WaterAbility) this.plugin.getAbilityManager().getAbilities().get(BootType.WATER);
                if (waterAbility.isWet(player) && (event.getCause() == EntityDamageEvent.DamageCause.FIRE || 
                    event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK || 
                    event.getCause() == EntityDamageEvent.DamageCause.LAVA)) {
                    event.setCancelled(true);
                }
                break;
        }
    }
}
