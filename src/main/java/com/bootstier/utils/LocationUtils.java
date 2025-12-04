package com.bootstier.utils;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for location operations
 */
public class LocationUtils {

    public static List<Player> getPlayersInRadius(final Location center, final double radius) {
        return center.getWorld().getPlayers().stream()
            .filter(player -> player.getLocation().distance(center) <= radius)
            .collect(Collectors.toList());
    }

    public static boolean isWithinRadius(final Location center, final Location target, final double radius) {
        return center.distance(target) <= radius;
    }

    public static Location getCirclePoint(final Location center, final double radius, final double angle) {
        final double x = center.getX() + radius * Math.cos(angle);
        final double z = center.getZ() + radius * Math.sin(angle);
        return new Location(center.getWorld(), x, center.getY(), z);
    }

    public static org.bukkit.util.Vector getDirectionBetween(final Location from, final Location to) {
        return to.toVector().subtract(from.toVector()).normalize();
    }
}
