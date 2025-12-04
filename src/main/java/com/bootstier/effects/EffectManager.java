package com.bootstier.effects;

import com.bootstier.BootsTierPlugin;
import com.bootstier.boots.BootType;
import com.bootstier.player.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages special effects and status tracking for boots
 */
public class EffectManager {

    private final BootsTierPlugin plugin;
    private final Map<UUID, Map<String, Long>> playerEffects;

    public EffectManager(final BootsTierPlugin plugin) {
        this.plugin = plugin;
        this.playerEffects = new HashMap<>();
    }

    public void addPlayerEffect(final Player player, final String effectName, final long durationMs) {
        final UUID playerId = player.getUniqueId();
        this.playerEffects.computeIfAbsent(playerId, k -> new HashMap<>())
            .put(effectName, System.currentTimeMillis() + durationMs);
    }

    public boolean hasPlayerEffect(final Player player, final String effectName) {
        final UUID playerId = player.getUniqueId();
        final Map<String, Long> effects = this.playerEffects.get(playerId);
        
        if (effects == null) {
            return false;
        }
        
        final Long expiration = effects.get(effectName);
        if (expiration == null) {
            return false;
        }
        
        if (System.currentTimeMillis() > expiration) {
            effects.remove(effectName);
            return false;
        }
        
        return true;
    }

    public void removePlayerEffect(final Player player, final String effectName) {
        final UUID playerId = player.getUniqueId();
        final Map<String, Long> effects = this.playerEffects.get(playerId);
        
        if (effects != null) {
            effects.remove(effectName);
        }
    }

    public void clearPlayerEffects(final Player player) {
        this.playerEffects.remove(player.getUniqueId());
    }

    public void startEffectCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                final long currentTime = System.currentTimeMillis();
                
                playerEffects.entrySet().removeIf(entry -> {
                    final Map<String, Long> effects = entry.getValue();
                    effects.entrySet().removeIf(effectEntry -> currentTime > effectEntry.getValue());
                    return effects.isEmpty();
                });
            }
        }.runTaskTimer(this.plugin, 0L, 200L); // Clean up every 10 seconds
    }

    public void applyBootThemeMessage(final Player player, final String message) {
        final PlayerData data = this.plugin.getPlayerManager().getPlayerData(player);
        if (data.getBootsData() == null) {
            return;
        }

        final BootType bootType = data.getBootsData().getBootType();
        final String themedMessage = bootType.getChatPrefix() + message;
        player.sendMessage(themedMessage);
    }
}
