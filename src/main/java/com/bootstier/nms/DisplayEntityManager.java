package com.bootstier.nms;

import com.bootstier.BootsTierPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;

/**
 * Manages Display entities (BlockDisplay, ItemDisplay) for visual effects.
 * Ensures proper cleanup and optimization.
 */
public class DisplayEntityManager {

    private final BootsTierPlugin plugin;
    private final Map<UUID, List<Display>> activeDisplays = new HashMap<>();

    public DisplayEntityManager(final BootsTierPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates a floating block display entity
     */
    public BlockDisplay createBlockDisplay(final Location location, final Material material, final int durationTicks) {
        final BlockDisplay display = (BlockDisplay) location.getWorld().spawnEntity(location, EntityType.BLOCK_DISPLAY);
        
        display.setBlock(material.createBlockData());
        display.setBrightness(new Display.Brightness(15, 15));
        display.setViewRange(64.0f);
        
        final Transformation transform = display.getTransformation();
        transform.getScale().set(0.5f, 0.5f, 0.5f);
        display.setTransformation(transform);
        
        this.trackDisplay(display, durationTicks);
        return display;
    }

    /**
     * Creates a floating item display entity
     */
    public ItemDisplay createItemDisplay(final Location location, final ItemStack item, final int durationTicks) {
        final ItemDisplay display = (ItemDisplay) location.getWorld().spawnEntity(location, EntityType.ITEM_DISPLAY);
        
        display.setItemStack(item);
        display.setBrightness(new Display.Brightness(15, 15));
        display.setViewRange(64.0f);
        
        final Transformation transform = display.getTransformation();
        transform.getScale().set(0.4f, 0.4f, 0.4f);
        display.setTransformation(transform);
        
        this.trackDisplay(display, durationTicks);
        return display;
    }

    /**
     * Creates a ring of block displays
     */
    public List<BlockDisplay> createBlockRing(final Location center, final double radius, final int count, 
                                               final Material material, final int durationTicks) {
        final List<BlockDisplay> displays = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            final double angle = (2 * Math.PI * i) / count;
            final double x = Math.cos(angle) * radius;
            final double z = Math.sin(angle) * radius;
            final Location displayLoc = center.clone().add(x, 0, z);
            
            final BlockDisplay display = this.createBlockDisplay(displayLoc, material, durationTicks);
            displays.add(display);
        }
        
        return displays;
    }

    /**
     * Creates a circle of item displays
     */
    public List<ItemDisplay> createItemCircle(final Location center, final double radius, final int count, 
                                               final ItemStack item, final int durationTicks) {
        final List<ItemDisplay> displays = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            final double angle = (2 * Math.PI * i) / count;
            final double x = Math.cos(angle) * radius;
            final double z = Math.sin(angle) * radius;
            final Location displayLoc = center.clone().add(x, 0.5, z);
            
            final ItemDisplay display = this.createItemDisplay(displayLoc, item, durationTicks);
            displays.add(display);
        }
        
        return displays;
    }

    /**
     * Animates a ring of displays to expand
     */
    public void animateExpandingRing(final List<? extends Display> displays, final Location center, 
                                      final double targetRadius, final int durationTicks) {
        final double startRadius = 0.5;
        final double radiusPerTick = (targetRadius - startRadius) / durationTicks;
        
        this.plugin.getServer().getScheduler().runTaskTimer(this.plugin, task -> {
            final int elapsed = task.getTaskId();
            
            if (elapsed >= durationTicks) {
                task.cancel();
                this.removeDisplays(displays);
                return;
            }
            
            final double currentRadius = startRadius + (radiusPerTick * elapsed);
            final int count = displays.size();
            
            for (int i = 0; i < count; i++) {
                final Display display = displays.get(i);
                if (display.isDead()) continue;
                
                final double angle = (2 * Math.PI * i) / count;
                final double x = Math.cos(angle) * currentRadius;
                final double z = Math.sin(angle) * currentRadius;
                final Location newLoc = center.clone().add(x, 0.5, z);
                
                display.teleport(newLoc);
            }
        }, 0L, 1L);
    }

    /**
     * Animates displays to rotate around a center point
     */
    public void animateRotatingRing(final List<? extends Display> displays, final Location center, 
                                     final double radius, final int durationTicks) {
        this.plugin.getServer().getScheduler().runTaskTimer(this.plugin, task -> {
            final int elapsed = task.getTaskId();
            
            if (elapsed >= durationTicks) {
                task.cancel();
                this.removeDisplays(displays);
                return;
            }
            
            final double rotation = elapsed * 0.1;
            final int count = displays.size();
            
            for (int i = 0; i < count; i++) {
                final Display display = displays.get(i);
                if (display.isDead()) continue;
                
                final double angle = (2 * Math.PI * i / count) + rotation;
                final double x = Math.cos(angle) * radius;
                final double z = Math.sin(angle) * radius;
                final Location newLoc = center.clone().add(x, center.getY() + 0.5, z);
                
                display.teleport(newLoc);
                
                // Rotate the display itself
                final Transformation transform = display.getTransformation();
                transform.getLeftRotation().set(new AxisAngle4f((float) angle, 0, 1, 0));
                display.setTransformation(transform);
            }
        }, 0L, 2L);
    }

    /**
     * Tracks a display for automatic cleanup
     */
    private void trackDisplay(final Display display, final int durationTicks) {
        final UUID displayId = display.getUniqueId();
        final List<Display> displays = this.activeDisplays.computeIfAbsent(displayId, k -> new ArrayList<>());
        displays.add(display);
        
        // Schedule cleanup
        if (durationTicks > 0) {
            this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
                if (!display.isDead()) {
                    display.remove();
                }
                this.activeDisplays.remove(displayId);
            }, durationTicks);
        }
    }

    /**
     * Removes a list of displays immediately
     */
    public void removeDisplays(final List<? extends Display> displays) {
        for (final Display display : displays) {
            if (display != null && !display.isDead()) {
                display.remove();
                this.activeDisplays.remove(display.getUniqueId());
            }
        }
    }

    /**
     * Cleans up all active displays (called on plugin disable)
     */
    public void cleanupAll() {
        for (final List<Display> displays : this.activeDisplays.values()) {
            for (final Display display : displays) {
                if (display != null && !display.isDead()) {
                    display.remove();
                }
            }
        }
        this.activeDisplays.clear();
    }

    /**
     * Updates all displays (called periodically)
     */
    public void updateDisplays() {
        final Iterator<Map.Entry<UUID, List<Display>>> it = this.activeDisplays.entrySet().iterator();
        
        while (it.hasNext()) {
            final Map.Entry<UUID, List<Display>> entry = it.next();
            final List<Display> displays = entry.getValue();
            
            displays.removeIf(display -> display == null || display.isDead());
            
            if (displays.isEmpty()) {
                it.remove();
            }
        }
    }
}
