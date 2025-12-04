package com.bootstier.player;

import com.bootstier.boots.BootsData;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Data class representing a player's information
 * Fully compatible with BootsManager, PlayerManager, LivesManager and others.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerData {

    private UUID playerId;
    private int lives;
    private BootsData bootsData;
    private Set<UUID> trustedPlayers;
    private boolean firstJoin;
    private long lastLogin;
    private long lastSeen;

    public PlayerData(final UUID playerId, final int defaultLives) {
        this.playerId = playerId;
        this.lives = defaultLives;
        this.bootsData = null;
        this.trustedPlayers = new HashSet<>();
        this.firstJoin = true;
        this.lastLogin = System.currentTimeMillis();
        this.lastSeen = System.currentTimeMillis();
    }

    /* ---------------------------------------------------------
       TRUST SYSTEM
    --------------------------------------------------------- */

    public boolean isTrusted(final UUID otherPlayerId) {
        return this.trustedPlayers.contains(otherPlayerId);
    }

    public void addTrustedPlayer(final UUID playerId) {
        this.trustedPlayers.add(playerId);
    }

    public void removeTrustedPlayer(final UUID playerId) {
        this.trustedPlayers.remove(playerId);
    }

    /* ---------------------------------------------------------
       LIVES SYSTEM (needed by LivesManager, BootShardManager)
    --------------------------------------------------------- */

    public boolean hasLives() {
        return this.lives > 0;
    }

    public void decrementLives() {
        if (this.lives > 0) this.lives--;
    }

    public void incrementLives(final int maxLives) {
        if (this.lives < maxLives) this.lives++;
    }

    public void setLives(final int lives, final int maxLives) {
        this.lives = Math.max(0, Math.min(lives, maxLives));
    }

    /* ---------------------------------------------------------
       BOOTS SYSTEM (needed by BootsManager & AbilityManager)
    --------------------------------------------------------- */

    public BootsData getBootsData() {
        return this.bootsData;
    }

    public void setBootsData(BootsData data) {
        this.bootsData = data;
    }

    public boolean areBootsBroken() {
        return this.bootsData != null && this.bootsData.isBroken();
    }

    public void breakBoots() {
        if (this.bootsData != null) {
            this.bootsData.setBroken(true);
        }
    }

    public void repairBoots(final int defaultLives) {
        if (this.bootsData != null) {
            this.bootsData.setBroken(false);
            this.lives = defaultLives;
        }
    }

    /* ---------------------------------------------------------
       TIMESTAMP HELPERS (required by PlayerManager)
    --------------------------------------------------------- */

    public UUID getPlayerId() {
        return this.playerId;
    }

    public void setLastSeen(long time) {
        this.lastSeen = time;
    }
}
