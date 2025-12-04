package com.bootstier.nms;

import com.bootstier.BootsTierPlugin;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Manages advanced packet operations using ProtocolLib for true invisibility,
 * selective visibility, custom particles, and client-side effects.
 */
public class PacketManager {

    private final BootsTierPlugin plugin;
    private final ProtocolManager protocolManager;
    private final Map<UUID, Set<UUID>> hiddenPlayers; // viewer -> hidden players

    public PacketManager(final BootsTierPlugin plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        this.hiddenPlayers = new HashMap<>();
    }

    /* ---------------------------------------------
       TRUE INVISIBILITY SYSTEM
    --------------------------------------------- */

    /**
     * Hides player from all *untrusted* players using Bukkit API.
     * - Trusted players still see them.
     * - This is true invis: entity + armor + cosmetics gone for those viewers.
     */
    public void hidePlayerFromUntrusted(final Player player) {
        final UUID playerUUID = player.getUniqueId();

        for (final Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(player)) continue;

            final boolean isTrusted = this.plugin.getTrustManager().isTrusted(player, viewer);

            if (!isTrusted) {
                // Hide full entity
                viewer.hidePlayer(this.plugin, player);

                // Track hidden
                this.hiddenPlayers
                        .computeIfAbsent(viewer.getUniqueId(), k -> new HashSet<>())
                        .add(playerUUID);
            }
        }

        this.plugin.getLogger().info("[visibility] Hiding " + player.getName() + " from untrusted players.");
    }

    /**
     * Restores visibility to everyone that had this player hidden.
     */
    public void showPlayerToAll(final Player player) {
        final UUID playerUUID = player.getUniqueId();

        for (final Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(player)) continue;

            final Set<UUID> viewerHidden = this.hiddenPlayers.get(viewer.getUniqueId());
            if (viewerHidden != null && viewerHidden.contains(playerUUID)) {
                viewer.showPlayer(this.plugin, player);
                viewerHidden.remove(playerUUID);
            } else {
                // Safety: ensure they're visible even if not tracked
                viewer.showPlayer(this.plugin, player);
            }
        }

        this.plugin.getLogger().info("[visibility] Restoring visibility for " + player.getName());
    }

    /**
     * Legacy low-level packet hide (currently unused).
     * Kept for future crazy stuff if you ever want it.
     */
    private void hidePlayerFromViewer(final Player target, final Player viewer) {
        try {
            final PacketContainer destroyPacket = this.protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
            destroyPacket.getIntLists().write(0, Collections.singletonList(target.getEntityId()));
            this.protocolManager.sendServerPacket(viewer, destroyPacket);

            final PacketContainer playerInfoRemove = this.protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO_REMOVE);
            playerInfoRemove.getModifier().write(0, Collections.singletonList(target.getUniqueId()));
            this.protocolManager.sendServerPacket(viewer, playerInfoRemove);
        } catch (Exception e) {
            this.plugin.getLogger().warning("[PacketManager] hidePlayerFromViewer failed: " + e.getMessage());
        }
    }

    /**
     * Legacy low-level packet show (currently unused).
     */
    private void showPlayerToViewer(final Player target, final Player viewer) {
        try {
            final PacketContainer playerInfoAdd = this.protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);
            final WrappedGameProfile profile = WrappedGameProfile.fromPlayer(target);

            final PlayerInfoData playerData = new PlayerInfoData(
                    profile,
                    target.getPing(),
                    EnumWrappers.NativeGameMode.fromBukkit(target.getGameMode()),
                    WrappedChatComponent.fromText(target.getDisplayName())
            );

            playerInfoAdd.getPlayerInfoActions().write(0, EnumSet.of(EnumWrappers.PlayerInfoAction.ADD_PLAYER));
            playerInfoAdd.getPlayerInfoDataLists().write(1, Collections.singletonList(playerData));

            this.protocolManager.sendServerPacket(viewer, playerInfoAdd);

            final PacketContainer spawnPacket = this.protocolManager.createPacket(PacketType.Play.Server.NAMED_ENTITY_SPAWN);
            spawnPacket.getIntegers().write(0, target.getEntityId());
            spawnPacket.getUUIDs().write(0, target.getUniqueId());
            spawnPacket.getDoubles()
                    .write(0, target.getLocation().getX())
                    .write(1, target.getLocation().getY())
                    .write(2, target.getLocation().getZ());

            this.protocolManager.sendServerPacket(viewer, spawnPacket);

            this.sendEquipmentPacket(target, viewer);
        } catch (Exception e) {
            this.plugin.getLogger().warning("[PacketManager] showPlayerToViewer failed: " + e.getMessage());
        }
    }

    /**
     * Sends equipment update packets (armor, boots, items).
     * Currently just sends a generic equipment packet; we let the client sync.
     */
    private void sendEquipmentPacket(final Player target, final Player viewer) {
        try {
            final PacketContainer equipmentPacket = this.protocolManager.createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);
            equipmentPacket.getIntegers().write(0, target.getEntityId());
            this.protocolManager.sendServerPacket(viewer, equipmentPacket);
        } catch (Exception e) {
            this.plugin.getLogger().warning("[PacketManager] sendEquipmentPacket failed: " + e.getMessage());
        }
    }

    /* ---------------------------------------------
       ACTION BAR SYSTEM
    --------------------------------------------- */

    public void sendActionBarPacket(final Player player, final String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }

    /* ---------------------------------------------
       PARTICLE SYSTEM
    --------------------------------------------- */

    public void sendParticlePacket(final Player viewer, final Location location,
                                   final Particle particle, final int count) {
        this.sendParticlePacket(viewer, location, particle, count, 0.0, 0.0, 0.0, 0.0);
    }

    public void sendParticlePacket(final Player viewer, final Location location,
                                   final Particle particle, final int count,
                                   final double offsetX, final double offsetY, final double offsetZ,
                                   final double speed) {
        viewer.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed);
    }

    public void sendParticleCircle(final Player viewer, final Location center,
                                   final Particle particle, final double radius, final int points) {
        for (int i = 0; i < points; i++) {
            final double angle = 2 * Math.PI * i / points;
            final double x = center.getX() + Math.cos(angle) * radius;
            final double z = center.getZ() + Math.sin(angle) * radius;
            final Location loc = new Location(center.getWorld(), x, center.getY(), z);
            this.sendParticlePacket(viewer, loc, particle, 1);
        }
    }

    public void sendParticleSphere(final Player viewer, final Location center,
                                   final Particle particle, final double radius, final int points) {
        for (int i = 0; i < points; i++) {
            final double phi = Math.acos(1 - 2 * (i + 0.5) / points);
            final double theta = Math.PI * (1 + Math.sqrt(5)) * i;

            final double x = center.getX() + radius * Math.cos(theta) * Math.sin(phi);
            final double y = center.getY() + radius * Math.sin(theta) * Math.sin(phi);
            final double z = center.getZ() + radius * Math.cos(phi);

            final Location loc = new Location(center.getWorld(), x, y, z);
            this.sendParticlePacket(viewer, loc, particle, 1);
        }
    }

    public void sendParticleHelix(final Player viewer, final Location start,
                                  final Particle particle, final double height,
                                  final double radius, final int rotations) {
        final int points = rotations * 20;
        for (int i = 0; i < points; i++) {
            final double angle = 2 * Math.PI * rotations * i / points;
            final double y = start.getY() + (height * i / points);
            final double x = start.getX() + Math.cos(angle) * radius;
            final double z = start.getZ() + Math.sin(angle) * radius;

            final Location loc = new Location(start.getWorld(), x, y, z);
            this.sendParticlePacket(viewer, loc, particle, 1);
        }
    }

    /* ---------------------------------------------
       BLOCK CHANGE PACKETS (CLIENT-SIDE ILLUSIONS)
    --------------------------------------------- */

    public void sendFakeBlockChange(final Player viewer, final Location location,
                                    final org.bukkit.Material material) {
        viewer.sendBlockChange(location, material.createBlockData());
    }

    public void resetFakeBlock(final Player viewer, final Location location) {
        viewer.sendBlockChange(location, location.getBlock().getBlockData());
    }

    /* ---------------------------------------------
       DAMAGE INDICATOR PACKETS
    --------------------------------------------- */

    public void sendDamageIndicator(final Player viewer, final Location location, final double damage) {
        viewer.spawnParticle(Particle.DAMAGE_INDICATOR, location, (int) (damage * 2), 0.2, 0.5, 0.2, 0.1);
    }

    /* ---------------------------------------------
       TITLE & SUBTITLE PACKETS
    --------------------------------------------- */

    public void sendTitle(final Player player, final String title, final String subtitle,
                          final int fadeIn, final int stay, final int fadeOut) {
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    /* ---------------------------------------------
       CLEANUP
    --------------------------------------------- */

    public void cleanup(final Player player) {
        this.hiddenPlayers.remove(player.getUniqueId());

        final UUID playerUUID = player.getUniqueId();
        for (final Set<UUID> hiddenSet : this.hiddenPlayers.values()) {
            hiddenSet.remove(playerUUID);
        }
    }

    public boolean isHiddenFrom(final Player target, final Player viewer) {
        final Set<UUID> viewerHidden = this.hiddenPlayers.get(viewer.getUniqueId());
        return viewerHidden != null && viewerHidden.contains(target.getUniqueId());
    }
}
