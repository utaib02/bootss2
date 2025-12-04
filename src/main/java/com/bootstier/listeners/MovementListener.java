package com.bootstier.listeners;

import com.bootstier.BootsTierPlugin;
import com.bootstier.boots.BootType;
import com.bootstier.boots.abilities.impl.FireAbility;
import com.bootstier.boots.abilities.impl.WindAbility;
import com.bootstier.player.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;

/**
 * Handles player movement events for boot passives
 */
public class MovementListener implements Listener {

    private final BootsTierPlugin plugin;

    public MovementListener(final BootsTierPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(final PlayerMoveEvent event) {
        final Player player = event.getPlayer();
        final PlayerData data = this.plugin.getPlayerManager().getPlayerData(player);
        
        if (data.getBootsData() == null || data.areBootsBroken()) {
            return;
        }

        // Handle boot-specific movement effects
        final BootType bootType = data.getBootsData().getBootType();
        
        switch (bootType) {
            case SPIDER:
                this.handleSpiderMovement(player, event);
                break;
            case FROST:
                this.handleFrostMovement(player, event);
                break;
            case FIRE:
                final FireAbility fireAbility = (FireAbility) this.plugin.getAbilityManager().getAbilities().get(BootType.FIRE);
                fireAbility.handleLavaMovement(player);
                break;
            case WIND:
                // Reset double jump when on ground
                if (player.isOnGround()) {
                    final WindAbility windAbility = (WindAbility) this.plugin.getAbilityManager().getAbilities().get(BootType.WIND);
                    windAbility.resetDoubleJump(player);
                }
                break;
        }
    }
    
    @EventHandler
    public void onPlayerToggleFlight(final PlayerToggleFlightEvent event) {
        final Player player = event.getPlayer();
        final PlayerData data = this.plugin.getPlayerManager().getPlayerData(player);
        
        if (data.getBootsData() == null || data.areBootsBroken()) {
            return;
        }
        
        // Handle Wind Boots double jump
        if (data.getBootsData().getBootType() == BootType.WIND && 
            data.getBootsData().getTier().getLevel() == 2) {
            
            if (!player.isOnGround() && player.getAllowFlight()) {
                event.setCancelled(true);
                player.setAllowFlight(false);
                player.setFlying(false);
                
                final WindAbility windAbility = (WindAbility) this.plugin.getAbilityManager().getAbilities().get(BootType.WIND);
                windAbility.handleDoubleJump(player);
            }
        }
    }

    private void handleSpiderMovement(final Player player, final PlayerMoveEvent event) {
        // Spider boots: fly through cobwebs
        if (player.getLocation().getBlock().getType() == Material.COBWEB) {
            // Remove slowness effect from cobweb
            player.removePotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS);
        }
    }

    private void handleFrostMovement(final Player player, final PlayerMoveEvent event) {
        // Frost boots: fly through powdered snow
        if (player.getLocation().getBlock().getType() == Material.POWDER_SNOW) {
            // Remove freezing effects
            player.setFreezeTicks(0);
        }
    }

}
