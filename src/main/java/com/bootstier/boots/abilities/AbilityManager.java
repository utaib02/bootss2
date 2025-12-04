package com.bootstier.boots.abilities;

import com.bootstier.BootsTierPlugin;
import com.bootstier.boots.BootType;
import com.bootstier.boots.BootsData;
import com.bootstier.boots.BootsTier;
import com.bootstier.boots.abilities.impl.*;
import com.bootstier.player.PlayerData;
import com.bootstier.utils.MessageUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages boot abilities and their execution
 */
public class AbilityManager {

    private final BootsTierPlugin plugin;
    private final Map<BootType, BootAbility> abilities;

    public AbilityManager(final BootsTierPlugin plugin) {
        this.plugin = plugin;
        this.abilities = new HashMap<>();
        this.initializeAbilities();
    }

    private void initializeAbilities() {
        this.abilities.put(BootType.SPEED, new SpeedAbility(this.plugin));
        this.abilities.put(BootType.STRENGTH, new StrengthAbility(this.plugin));
        this.abilities.put(BootType.WARD, new WardAbility(this.plugin));
        this.abilities.put(BootType.SPIDER, new SpiderAbility(this.plugin));
        this.abilities.put(BootType.FROST, new FrostAbility(this.plugin));
        this.abilities.put(BootType.WIND, new WindAbility(this.plugin));
        this.abilities.put(BootType.ASTRAL, new AstralAbility(this.plugin));
        this.abilities.put(BootType.LIFE, new LifeAbility(this.plugin));
        this.abilities.put(BootType.WATER, new WaterAbility(this.plugin));
        this.abilities.put(BootType.FIRE, new FireAbility(this.plugin));
    }

    public boolean activateAbility(final Player player, final int tier) {
        final PlayerData data = this.plugin.getPlayerManager().getPlayerData(player);
        final BootsData bootsData = data.getBootsData();
        
        if (bootsData == null || data.areBootsBroken()) {
            MessageUtils.sendMessage(player, "&cYour boots are broken!");
            return false;
        }

        if (tier == 2 && bootsData.getTier() != BootsTier.TIER_2) {
            MessageUtils.sendMessage(player, "&cYou need Tier 2 boots for this ability!");
            return false;
        }

        final BootAbility ability = this.abilities.get(bootsData.getBootType());
        if (ability == null) {
            return false;
        }

        final long cooldown = this.calculateCooldown(player, bootsData.getBootType(), tier);
        
        final boolean isReady = tier == 1 ? 
            bootsData.isAbilityReady(cooldown) : 
            bootsData.isTier2AbilityReady(cooldown);
            
        if (!isReady) {
            final long remaining = tier == 1 ? 
                bootsData.getRemainingCooldown(cooldown) / 1000 :
                bootsData.getTier2RemainingCooldown(cooldown) / 1000;
            MessageUtils.sendMessage(player, "&cAbility on cooldown! " + remaining + " seconds remaining.");
            return false;
        }

        // Execute ability
        final boolean success = tier == 1 ? 
            ability.executeTier1(player) : 
            ability.executeTier2(player);

        if (success) {
            if (tier == 1) {
                bootsData.activateAbility(this.getAbilityDuration(bootsData.getBootType(), tier));
            } else {
                bootsData.activateTier2Ability(this.getAbilityDuration(bootsData.getBootType(), tier));
            }
            this.plugin.getPlayerManager().savePlayerData(data);
            
            // Send boot-themed message
            final String bootPrefix = bootsData.getBootType().getChatPrefix();
            final String abilityName = this.getAbilityName(bootsData.getBootType(), tier);
            MessageUtils.sendMessage(player, bootPrefix + "&a" + abilityName + " activated!");
        }

        return success;
    }

    public void applyPassiveEffects(final Player player) {
        final PlayerData data = this.plugin.getPlayerManager().getPlayerData(player);
        final BootsData bootsData = data.getBootsData();
        
        if (bootsData == null || data.areBootsBroken()) {
            return;
        }

        final BootAbility ability = this.abilities.get(bootsData.getBootType());
        if (ability != null) {
            if (bootsData.getTier() == BootsTier.TIER_1) {
                ability.applyTier1Passives(player);
            } else {
                ability.applyTier2Passives(player);
            }
        }
    }

    private long calculateCooldown(final Player player, final BootType bootType, final int tier) {
        long baseCooldown = this.getBaseCooldown(bootType, tier);
        
        // Check for dragon egg
        if (this.hasDragonEgg(player)) {
            baseCooldown /= 2;
        }
        
        return baseCooldown;
    }

    private long getBaseCooldown(final BootType bootType, final int tier) {
        switch (bootType) {
            case SPEED: return tier == 1 ? 45000 : 90000;
            case STRENGTH: return tier == 1 ? 90000 : 180000;
            case WARD: return tier == 1 ? 90000 : 120000;
            case SPIDER: return tier == 1 ? 120000 : 60000;
            case FROST: return tier == 1 ? 60000 : 95000;
            case WIND: return tier == 1 ? 40000 : 60000;
            case ASTRAL: return tier == 1 ? 90000 : 120000;
            case LIFE: return tier == 1 ? 120000 : 75000;
            case WATER: return tier == 1 ? 75000 : 0;
            case FIRE: return 0; // Instant abilities
            default: return 60000;
        }
    }

    private long getAbilityDuration(final BootType bootType, final int tier) {
        switch (bootType) {
            case SPEED: return tier == 1 ? 10000 : 4000; // 10s blur / 4s stun
            case STRENGTH: return tier == 1 ? 10000 : 30000; // 10s crit / 30s damage link
            case WARD: return tier == 1 ? 10000 : 60000; // 10s invis / 1m sensor
            case SPIDER: return tier == 1 ? 0 : 15000; // Instant / 15s webs
            case FROST: return tier == 1 ? 5000 : 15000; // 5s shield / 15s ice circle
            case WIND: return tier == 1 ? 0 : 10000; // Instant dash / 10s tornado
            case ASTRAL: return tier == 1 ? 20000 : 15000; // 20s rewind / 15s disable
            case LIFE: return tier == 1 ? 0 : 15000; // Instant / 15s circle
            case WATER: return tier == 1 ? 10000 : 0; // 10s whirlpool / instant wave
            case FIRE: return 0; // Instant abilities
            default: return 10000;
        }
    }

    private boolean hasDragonEgg(final Player player) {
        for (final ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.DRAGON_EGG) {
                return true;
            }
        }
        return false;
    }
    
    private String getAbilityName(final BootType bootType, final int tier) {
        switch (bootType) {
            case SPEED: return tier == 1 ? "Blur" : "Thunder Strike";
            case STRENGTH: return tier == 1 ? "Critical Surge" : "Damage Link";
            case WARD: return tier == 1 ? "True Invisibility" : "Sculk Sensor";
            case SPIDER: return tier == 1 ? "Spider Swarm" : "Web Fireball";
            case FROST: return tier == 1 ? "Frost Shield" : "Ice Circle";
            case WIND: return tier == 1 ? "Dash" : "Tornado";
            case ASTRAL: return tier == 1 ? "Rewind" : "Boot Disable";
            case LIFE: return tier == 1 ? "Life Drain" : "Weakness Circle";
            case WATER: return tier == 1 ? "Whirlpool" : "Wave Push";
            case FIRE: return tier == 1 ? "Fire Rings" : "Blazing Dash";
            default: return "Ability " + tier;
        }
    }
    
    public Map<BootType, BootAbility> getAbilities() {
        return this.abilities;
    }
}
