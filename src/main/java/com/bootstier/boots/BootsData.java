package com.bootstier.boots;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Data class representing a player's boots information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BootsData {
    private BootType bootType;
    private BootsTier tier;
    private boolean broken;
    private long lastAbilityUse;
    private long abilityActiveUntil;
    private long lastTier2AbilityUse;
    private long tier2AbilityActiveUntil;
    private int hitCounter;
    private double radius;
    private boolean lowLifeWarningShown;
    
    public BootsData(final BootType bootType) {
        this.bootType = bootType;
        this.tier = BootsTier.TIER_1;
        this.broken = false;
        this.lastAbilityUse = 0;
        this.abilityActiveUntil = 0;
        this.lastTier2AbilityUse = 0;
        this.tier2AbilityActiveUntil = 0;
        this.hitCounter = 0;
        this.radius = 0.0;
        this.lowLifeWarningShown = false;
    }

    public boolean isAbilityReady(final long cooldownMs) {
        return System.currentTimeMillis() >= this.lastAbilityUse + cooldownMs;
    }

    public boolean isTier2AbilityReady(final long cooldownMs) {
        return System.currentTimeMillis() >= this.lastTier2AbilityUse + cooldownMs;
    }

    public boolean isAbilityActive() {
        return System.currentTimeMillis() < this.abilityActiveUntil;
    }

    public boolean isTier2AbilityActive() {
        return System.currentTimeMillis() < this.tier2AbilityActiveUntil;
    }

    public void activateAbility(final long durationMs) {
        this.lastAbilityUse = System.currentTimeMillis();
        this.abilityActiveUntil = System.currentTimeMillis() + durationMs;
    }

    public void activateTier2Ability(final long durationMs) {
        this.lastTier2AbilityUse = System.currentTimeMillis();
        this.tier2AbilityActiveUntil = System.currentTimeMillis() + durationMs;
    }

    public long getRemainingCooldown(final long cooldownMs) {
        final long remaining = (this.lastAbilityUse + cooldownMs) - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    public long getTier2RemainingCooldown(final long cooldownMs) {
        final long remaining = (this.lastTier2AbilityUse + cooldownMs) - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    public void incrementHitCounter() {
        this.hitCounter++;
    }

    public void resetHitCounter() {
        this.hitCounter = 0;
    }
}
