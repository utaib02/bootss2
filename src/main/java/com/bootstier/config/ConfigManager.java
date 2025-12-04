package com.bootstier.config;

import com.bootstier.BootsTierPlugin;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Manages plugin configuration
 */
public class ConfigManager {

    private final BootsTierPlugin plugin;
    private final FileConfiguration config;

    @Getter
    private final int defaultLives;
    @Getter
    private final int maxLives;
    @Getter
    private final boolean giveBootsOnJoin;
    @Getter
    private final boolean debug;
    @Getter
    private final int ritualDuration;
    @Getter
    private final int beaconStandTime;
    @Getter
    private final boolean particlesEnabled;
    @Getter
    private final int particleUpdateFrequency;
    @Getter
    private final int particleCount;
    @Getter
    private final double particleRadius;
    @Getter
    private final double dragonEggReduction;
    @Getter
    private final boolean showActionBar;
    @Getter
    private final int actionBarFrequency;
    @Getter
    private final int maxTrusted;
    @Getter
    private final boolean broadcastTrust;
    @Getter
    private final boolean armorTrimsEnabled;
    @Getter
    private final boolean breakingAnimationEnabled;
    @Getter
    private final boolean lowLifeWarningEnabled;
    @Getter
    private final int warningInterval;

    public ConfigManager(final BootsTierPlugin plugin) {
        this.plugin = plugin;
        this.plugin.saveDefaultConfig();
        this.config = this.plugin.getConfig();

        // Load configuration values
        this.defaultLives = this.config.getInt("general.default-lives", 5);
        this.maxLives = this.config.getInt("general.max-lives", 10);
        this.giveBootsOnJoin = this.config.getBoolean("general.give-boots-on-join", true);
        this.debug = this.config.getBoolean("general.debug", false);
        this.ritualDuration = this.config.getInt("pedestal.ritual-duration", 600);
        this.beaconStandTime = this.config.getInt("pedestal.beacon-stand-time", 10);
        this.particlesEnabled = this.config.getBoolean("particles.enabled", true);
        this.particleUpdateFrequency = this.config.getInt("particles.update-frequency", 5);
        this.particleCount = this.config.getInt("particles.particle-count", 3);
        this.particleRadius = this.config.getDouble("particles.radius", 0.8);
        this.dragonEggReduction = this.config.getDouble("cooldowns.dragon-egg-reduction", 0.5);
        this.showActionBar = this.config.getBoolean("cooldowns.show-action-bar", true);
        this.actionBarFrequency = this.config.getInt("cooldowns.action-bar-frequency", 20);
        this.maxTrusted = this.config.getInt("trust.max-trusted", 10);
        this.broadcastTrust = this.config.getBoolean("trust.broadcast-trust", true);
        this.armorTrimsEnabled = this.config.getBoolean("general.armor-trims-enabled", true);
        this.breakingAnimationEnabled = this.config.getBoolean("boot-breaking.breaking-animation", true);
        this.lowLifeWarningEnabled = this.config.getBoolean("boot-breaking.low-life-warning", true);
        this.warningInterval = this.config.getInt("boot-breaking.warning-interval", 30);
    }

    public String getMessage(final String key) {
        final String prefix = this.config.getString("messages.prefix", "&8[&6Boots&8] &r");
        final String message = this.config.getString("messages." + key, "&cMessage not found: " + key);
        return prefix + message;
    }

    public Location getPedestalLocation() {
        final String worldName = this.config.getString("pedestal.location.world", "world");
        final double x = this.config.getDouble("pedestal.location.x", 0);
        final double y = this.config.getDouble("pedestal.location.y", 64);
        final double z = this.config.getDouble("pedestal.location.z", 0);
        
        return new Location(this.plugin.getServer().getWorld(worldName), x, y, z);
    }

    public void setPedestalLocation(final Location location) {
        this.config.set("pedestal.location.world", location.getWorld().getName());
        this.config.set("pedestal.location.x", location.getX());
        this.config.set("pedestal.location.y", location.getY());
        this.config.set("pedestal.location.z", location.getZ());
        this.plugin.saveConfig();
    }

    public boolean isPedestalActive() {
        return this.config.getBoolean("pedestal.active", false);
    }

    public void setPedestalActive(final boolean active) {
        this.config.set("pedestal.active", active);
        this.plugin.saveConfig();
    }

    public void reloadConfig() {
        this.plugin.reloadConfig();
    }
}
