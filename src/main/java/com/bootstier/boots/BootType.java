package com.bootstier.boots;

import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

/**
 * Enum representing different boot types with armor trims & particles
 */
@Getter
public enum BootType {

    SPEED("Speed Boots", ChatColor.AQUA, Particle.CLOUD,
            TrimMaterial.EMERALD, TrimPattern.BOLT, "⚡"),

    STRENGTH("Strength Boots", ChatColor.RED, Particle.CRIT,
            TrimMaterial.GOLD, TrimPattern.SHAPER, "⚔"),

    WARD("Ward Boots", ChatColor.DARK_PURPLE, Particle.PORTAL,
            TrimMaterial.AMETHYST, TrimPattern.SILENCE, "👁"),

    SPIDER("Spider Boots", ChatColor.DARK_GREEN, Particle.ITEM_SLIME,
            TrimMaterial.COPPER, TrimPattern.SNOUT, "🕷"),

    FROST("Frost Boots", ChatColor.BLUE, Particle.SNOWFLAKE,
            TrimMaterial.DIAMOND, TrimPattern.RIB, "❄"),

    WIND("Wind Boots", ChatColor.WHITE, Particle.CLOUD,
            TrimMaterial.QUARTZ, TrimPattern.DUNE, "💨"),

    ASTRAL("Astral Boots", ChatColor.LIGHT_PURPLE, Particle.END_ROD,
            TrimMaterial.NETHERITE, TrimPattern.EYE, "✦"),

    LIFE("Life Boots", ChatColor.GREEN, Particle.HEART,
            TrimMaterial.EMERALD, TrimPattern.WILD, "💚"),

    WATER("Water Boots", ChatColor.DARK_AQUA, Particle.DRIPPING_WATER,
            TrimMaterial.LAPIS, TrimPattern.TIDE, "🌊"),

    FIRE("Fire Boots", ChatColor.GOLD, Particle.FLAME,
            TrimMaterial.REDSTONE, TrimPattern.SENTRY, "🔥");

    private final String displayName;
    private final ChatColor color;
    private final Particle particle;
    private final TrimMaterial trimMaterial;
    private final TrimPattern trimPattern;
    private final String emoji;

    BootType(String displayName, ChatColor color, Particle particle,
             TrimMaterial trimMaterial, TrimPattern trimPattern, String emoji) {
        this.displayName = displayName;
        this.color = color;
        this.particle = particle;
        this.trimMaterial = trimMaterial;
        this.trimPattern = trimPattern;
        this.emoji = emoji;
    }

    public String getColoredName() {
        return this.color + this.displayName;
    }

    public String getChatPrefix() {
        return ChatColor.DARK_GRAY + "[" + this.color + this.emoji + " " + this.displayName +
                ChatColor.DARK_GRAY + "] " + ChatColor.RESET;
    }

    public String getStyledName() {
        return this.color + this.emoji + " " + this.displayName;
    }
}
