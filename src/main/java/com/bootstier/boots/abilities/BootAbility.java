package com.bootstier.boots.abilities;

import org.bukkit.entity.Player;

/**
 * Base interface for boot abilities
 */
public interface BootAbility {
    
    /**
     * Execute Tier 1 ability
     */
    boolean executeTier1(Player player);
    
    /**
     * Execute Tier 2 ability
     */
    boolean executeTier2(Player player);
    
    /**
     * Apply Tier 1 passive effects
     */
    void applyTier1Passives(Player player);
    
    /**
     * Apply Tier 2 passive effects
     */
    void applyTier2Passives(Player player);
}
