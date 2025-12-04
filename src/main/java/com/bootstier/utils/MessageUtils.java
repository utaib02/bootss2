package com.bootstier.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * Utility class for message handling
 */
public class MessageUtils {

    /**
     * Sends a colorized message to a single player.
     */
    public static void sendMessage(final Player player, final String message) {
        if (player == null || message == null) return;
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    /**
     * Sends a colorized message to a collection of players.
     */
    public static void sendMessage(final Collection<? extends Player> players, final String message) {
        if (players == null || message == null) return;
        final String colored = ChatColor.translateAlternateColorCodes('&', message);
        for (final Player p : players) {
            if (p != null && p.isOnline()) p.sendMessage(colored);
        }
    }

    /**
     * Sends a colorized message to all online players (broadcast alternative).
     */
    public static void broadcast(final String message) {
        if (message == null) return;
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    /**
     * Sends an action bar message to a player.
     */
    public static void sendActionBar(final Player player, final String message) {
        if (player == null || message == null) return;
        player.sendActionBar(ChatColor.translateAlternateColorCodes('&', message));
    }

    /**
     * Colorizes any string using '&' color codes.
     */
    public static String colorize(final String message) {
        return message == null ? "" : ChatColor.translateAlternateColorCodes('&', message);
    }
}
