package com.bootstier.boots;

import lombok.Getter;

/**
 * Enum representing boot tiers
 */
@Getter
public enum BootsTier {
    TIER_1(1, "I", "Legendary"),
    TIER_2(2, "II", "Mythic");

    private final int level;
    private final String roman;
    private final String displayName;

    BootsTier(final int level, final String roman, final String displayName) {
        this.level = level;
        this.roman = roman;
        this.displayName = displayName;
    }

    public static BootsTier fromLevel(final int level) {
        for (final BootsTier tier : values()) {
            if (tier.level == level) {
                return tier;
            }
        }
        return TIER_1;
    }
}
