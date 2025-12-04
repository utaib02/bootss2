package com.bootstier.player;

import com.bootstier.BootsTierPlugin;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Manages trust relationships between players
 */
public class TrustManager {

    private final BootsTierPlugin plugin;

    public TrustManager(final BootsTierPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isTrusted(final Player player, final Player other) {
        if (player.getUniqueId().equals(other.getUniqueId())) {
            return true; // Players trust themselves
        }
        
        final PlayerData data = this.plugin.getPlayerManager().getPlayerData(player);
        return data.isTrusted(other.getUniqueId());
    }

    public boolean isTrusted(final UUID playerId, final UUID otherId) {
        if (playerId.equals(otherId)) {
            return true; // Players trust themselves
        }
        
        final PlayerData data = this.plugin.getPlayerManager().getPlayerData(playerId);
        return data.isTrusted(otherId);
    }

    public boolean addTrust(final Player player, final Player target) {
        final PlayerData data = this.plugin.getPlayerManager().getPlayerData(player);
        
        if (data.getTrustedPlayers().size() >= this.plugin.getConfigManager().getMaxTrusted()) {
            return false; // Max trusted players reached
        }
        
        if (data.isTrusted(target.getUniqueId())) {
            return false; // Already trusted
        }
        
        data.addTrustedPlayer(target.getUniqueId());
        this.plugin.getPlayerManager().savePlayerData(data);
        return true;
    }

    public boolean removeTrust(final Player player, final Player target) {
        final PlayerData data = this.plugin.getPlayerManager().getPlayerData(player);
        
        if (!data.isTrusted(target.getUniqueId())) {
            return false; // Not trusted
        }
        
        data.removeTrustedPlayer(target.getUniqueId());
        this.plugin.getPlayerManager().savePlayerData(data);
        return true;
    }

    public int getTrustedCount(final Player player) {
        final PlayerData data = this.plugin.getPlayerManager().getPlayerData(player);
        return data.getTrustedPlayers().size();
    }

    public boolean canTrustMore(final Player player) {
        return this.getTrustedCount(player) < this.plugin.getConfigManager().getMaxTrusted();
    }
}
