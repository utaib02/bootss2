package com.bootstier.effects;

import com.bootstier.BootsTierPlugin;
import com.bootstier.boots.BootType;
import com.bootstier.boots.BootsData;
import com.bootstier.boots.BootsTier;
import com.bootstier.player.PlayerData;
import com.bootstier.utils.MessageUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * FIXED VERSION — cooldown halving now applies to *real* cooldown timers
 * All abilities must use getTotalAbilityCooldown() for real cooldown logic.
 */
public class ActionBarManager {

    private final BootsTierPlugin plugin;
    private static final int COOLDOWN_BAR_LENGTH = 15;

    public ActionBarManager(final BootsTierPlugin plugin) {
        this.plugin = plugin;
    }

    public void updateAllActionBars() {
        if (!this.plugin.getConfigManager().isShowActionBar()) return;

        for (Player player : this.plugin.getServer().getOnlinePlayers()) {
            this.updatePlayerActionBar(player);
        }
    }

    private void updatePlayerActionBar(final Player player) {
        final PlayerData data = this.plugin.getPlayerManager().getPlayerData(player);

        if (data.getBootsData() == null) return;

        final String actionBarText = this.buildActionBarText(player, data);
        MessageUtils.sendActionBar(player, actionBarText);
    }

    private String buildActionBarText(final Player player, final PlayerData data) {
        final BootsData bootsData = data.getBootsData();

        if (data.areBootsBroken()) return "§c§lBROKEN";

        final StringBuilder sb = new StringBuilder();
        final boolean hasDragonEgg = hasDragonEgg(player);
        final BootType type = bootsData.getBootType();

        sb.append(getAbilityStatus(type, 1, bootsData, hasDragonEgg));

        if (bootsData.getTier() == BootsTier.TIER_2) {
            sb.append(" §8│ ");
            sb.append(getAbilityStatus(type, 2, bootsData, hasDragonEgg));
        }

        return sb.toString();
    }

    private String getAbilityStatus(
            final BootType type,
            final int tier,
            final BootsData bootsData,
            final boolean hasDragonEgg
    ) {
        final String abilityName = getAbilityName(type, tier);
        final String colored = getColoredAbilityName(type, abilityName);

        final long totalCooldown = getTotalAbilityCooldown(type, tier, hasDragonEgg);

        boolean ready = (tier == 1)
                ? bootsData.isAbilityReady(totalCooldown)
                : bootsData.isTier2AbilityReady(totalCooldown);

        if (ready) {
            return colored + ": §aReady";
        }

        long remaining = (tier == 1)
                ? bootsData.getRemainingCooldown(totalCooldown)
                : bootsData.getTier2RemainingCooldown(totalCooldown);

        return colored + ": §c" + (remaining / 1000) + "s";
    }

    private String getColoredAbilityName(final BootType bootType, final String abilityName) {
        switch (bootType) {
            case SPEED: return "§e" + abilityName;
            case STRENGTH: return "§c" + abilityName;
            case WARD: return "§d" + abilityName;
            case SPIDER: return "§2" + abilityName;
            case FROST: return "§b" + abilityName;
            case WIND: return "§f" + abilityName;
            case ASTRAL: return "§d" + abilityName;
            case LIFE: return "§a" + abilityName;
            case WATER: return "§9" + abilityName;
            case FIRE: return "§6" + abilityName;
            default: return "§7" + abilityName;
        }
    }

    private String getAbilityName(final BootType type, final int tier) {
        switch (type) {
            case SPEED: return (tier == 1) ? "Blur" : "Thunder Strike";
            case STRENGTH: return (tier == 1) ? "Critical Surge" : "Damage Link";
            case WARD: return (tier == 1) ? "True Invisibility" : "Sculk Sensor";
            case SPIDER: return (tier == 1) ? "Spider Swarm" : "Web Fireball";
            case FROST: return (tier == 1) ? "Frost Shield" : "Ice Circle";
            case WIND: return (tier == 1) ? "Dash" : "Tornado";
            case ASTRAL: return (tier == 1) ? "Rewind" : "Boot Disable";
            case LIFE: return (tier == 1) ? "Life Drain" : "Weakness Circle";
            case WATER: return (tier == 1) ? "Whirlpool" : "Wave Push";
            case FIRE: return (tier == 1) ? "Fire Rings" : "Blazing Dash";
            default: return "Ability " + tier;
        }
    }

    private long getBaseCooldown(final BootType type, final int tier) {
        switch (type) {
            case SPEED: return (tier == 1) ? 45000 : 90000;
            case STRENGTH: return (tier == 1) ? 90000 : 180000;
            case WARD: return (tier == 1) ? 90000 : 120000;
            case SPIDER: return (tier == 1) ? 120000 : 60000;
            case FROST: return (tier == 1) ? 60000 : 95000;
            case WIND: return (tier == 1) ? 40000 : 60000;
            case ASTRAL: return (tier == 1) ? 90000 : 120000;
            case LIFE: return (tier == 1) ? 120000 : 75000;
            case WATER: return (tier == 1) ? 75000 : 0;
            case FIRE: return 0;
        }
        return 0;
    }

    /**
     * FIXED:
     * This method is now the ONE TRUE source of cooldown duration.
     * Abilities MUST use this value instead of hardcoding their own.
     */
    public long getTotalAbilityCooldown(final BootType type, final int tier, final boolean hasDragonEgg) {
        long base = getBaseCooldown(type, tier);
        return hasDragonEgg ? (base / 2) : base;
    }

    private boolean hasDragonEgg(final Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.DRAGON_EGG)
                return true;
        }
        return false;
    }
}
