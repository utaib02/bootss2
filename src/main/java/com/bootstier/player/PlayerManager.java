package com.bootstier.player;

import com.bootstier.BootsTierPlugin;
import com.bootstier.boots.BootType;
import com.bootstier.boots.BootsData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player data and operations
 */
public class PlayerManager {

    private final BootsTierPlugin plugin;
    private final Gson gson;
    private final File dataFolder;
    
    @Getter
    private final Map<UUID, PlayerData> playerDataMap;
    private final Random random;

    public PlayerManager(final BootsTierPlugin plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        this.playerDataMap = new ConcurrentHashMap<>();
        this.random = new Random();

        if (!this.dataFolder.exists()) {
            this.dataFolder.mkdirs();
        }

        this.loadAllPlayerData();
    }

    public PlayerData getPlayerData(final UUID playerId) {
        return this.playerDataMap.computeIfAbsent(playerId, id -> {
            final PlayerData data = this.loadPlayerData(id);
            if (data != null) {
                return data;
            }
            return new PlayerData(id, this.plugin.getConfigManager().getDefaultLives());
        });
    }

    public PlayerData getPlayerData(final Player player) {
        return this.getPlayerData(player.getUniqueId());
    }

    public void createNewPlayer(final Player player) {
        final PlayerData data = new PlayerData(player.getUniqueId(), this.plugin.getConfigManager().getDefaultLives());
        
        // Assign random boot type
        final BootType randomBootType = BootType.values()[this.random.nextInt(BootType.values().length)];
        data.setBootsData(new BootsData(randomBootType));
        
        this.playerDataMap.put(player.getUniqueId(), data);
        this.savePlayerData(data);
    }

    public void rerollPlayerBoots(final Player player) {
        final PlayerData data = this.getPlayerData(player);
        final BootType currentType = data.getBootsData().getBootType();
        
        BootType newType;
        do {
            newType = BootType.values()[this.random.nextInt(BootType.values().length)];
        } while (newType == currentType);
        
        // Preserve tier when rerolling
        final BootsData newBootsData = new BootsData(newType);
        newBootsData.setTier(data.getBootsData().getTier());
        data.setBootsData(newBootsData);
        
        this.savePlayerData(data);
    }

    public boolean hasLives(final Player player) {
        return this.getPlayerData(player).hasLives();
    }

    public int getLives(final Player player) {
        return this.getPlayerData(player).getLives();
    }

    public void decrementLives(final Player player) {
        final PlayerData data = this.getPlayerData(player);
        data.decrementLives();
        
        if (!data.hasLives()) {
            data.breakBoots();
        }
        
        this.savePlayerData(data);
    }

    public void incrementLives(final Player player) {
        final PlayerData data = this.getPlayerData(player);
        data.incrementLives(this.plugin.getConfigManager().getMaxLives());
        this.savePlayerData(data);
    }

    public void setLives(final Player player, final int lives) {
        final PlayerData data = this.getPlayerData(player);
        data.setLives(lives, this.plugin.getConfigManager().getMaxLives());
        this.savePlayerData(data);
    }

    private PlayerData loadPlayerData(final UUID playerId) {
        final File file = new File(this.dataFolder, playerId.toString() + ".json");
        if (!file.exists()) {
            return null;
        }

        try (final FileReader reader = new FileReader(file)) {
            return this.gson.fromJson(reader, PlayerData.class);
        } catch (final IOException e) {
            this.plugin.getLogger().warning("Failed to load player data for " + playerId + ": " + e.getMessage());
            return null;
        }
    }

    public void savePlayerData(final PlayerData data) {
        final File file = new File(this.dataFolder, data.getPlayerId().toString() + ".json");
        
        try (final FileWriter writer = new FileWriter(file)) {
            this.gson.toJson(data, writer);
        } catch (final IOException e) {
            this.plugin.getLogger().warning("Failed to save player data for " + data.getPlayerId() + ": " + e.getMessage());
        }
    }

    public void saveAllPlayerData() {
        for (final PlayerData data : this.playerDataMap.values()) {
            this.savePlayerData(data);
        }
    }

    private void loadAllPlayerData() {
        if (!this.dataFolder.exists()) {
            return;
        }

        final File[] files = this.dataFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) {
            return;
        }

        for (final File file : files) {
            try {
                final String fileName = file.getName();
                final UUID playerId = UUID.fromString(fileName.substring(0, fileName.length() - 5));
                final PlayerData data = this.loadPlayerData(playerId);
                if (data != null) {
                    this.playerDataMap.put(playerId, data);
                }
            } catch (final IllegalArgumentException e) {
                this.plugin.getLogger().warning("Invalid player data file: " + file.getName());
            }
        }
    }

    public void unloadPlayer(final Player player) {
        final PlayerData data = this.playerDataMap.get(player.getUniqueId());
        if (data != null) {
            data.setLastSeen(System.currentTimeMillis());
            this.savePlayerData(data);
        }
    }
}
