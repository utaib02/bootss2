package com.bootstier.listeners;

import com.bootstier.BootsTierPlugin;
import com.bootstier.boots.BootType;
import com.bootstier.boots.BootsData;
import com.bootstier.boots.BootsTier;
import com.bootstier.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Handles the Boot Reroller elimination GUI.
 * - Left-click boots to eliminate / restore from the reroll pool
 * - Right-click the Echo Shard in the center to confirm reroll
 * - Each eliminated boot costs shards (1 -> first, 2 -> second, etc.)
 */
public class RerollerGuiListener implements Listener {

    private final BootsTierPlugin plugin;

    // Track open reroller sessions per player
    private final Map<UUID, RerollerSession> activeSessions = new HashMap<>();

    public RerollerGuiListener(final BootsTierPlugin plugin) {
        this.plugin = plugin;
    }

    /* ======================================================================
       OPEN GUI
    ======================================================================= */

    /**
     * Opens the reroller GUI for the given player.
     */
    public void openRerollerGUI(Player player) {
        PlayerData data = plugin.getPlayerManager().getPlayerData(player);

        if (data.getBootsData() == null) {
            player.sendMessage(ChatColor.RED + "You don't have any boots to reroll!");
            return;
        }

        if (data.areBootsBroken()) {
            player.sendMessage(ChatColor.RED + "Your boots are broken! Repair them first.");
            return;
        }

        // Max eliminations = lives - 1 (never allow eliminating everything)
        int maxEliminations = Math.max(0, data.getLives() - 1);

        // Create session for this player
        RerollerSession session = new RerollerSession(player.getUniqueId(), maxEliminations);
        activeSessions.put(player.getUniqueId(), session);

        // Open the GUI
        Inventory gui = createRerollerGUI(session);
        player.openInventory(gui);

        player.sendMessage("§6§l✦ Reroller GUI");
        player.sendMessage("§7Left-click boots to eliminate them from the pool.");
        player.sendMessage("§7Right-click the §dReroll Boots §7(Echo Shard) to reroll.");
        player.sendMessage("§7You can eliminate up to §e" + maxEliminations + " §7boots.");
    }

    /* ======================================================================
       GUI CREATION
    ======================================================================= */

    private Inventory createRerollerGUI(RerollerSession session) {
        Inventory gui = Bukkit.createInventory(null, 27, "§6§lBoot Reroller - Elimination");

        // Center slot (13): Echo Shard (reroll trigger)
        ItemStack echoShard = new ItemStack(Material.ECHO_SHARD);
        ItemMeta echoMeta = echoShard.getItemMeta();
        if (echoMeta != null) {
            echoMeta.setDisplayName("§d§lReroll Boots");
            List<String> echoLore = new ArrayList<>();
            echoLore.add("§7Right-click to reroll your boots.");
            echoLore.add("");
            echoLore.add("§7Eliminations: §e" + session.eliminatedBoots.size()
                    + "§8/§7" + session.maxEliminations);
            echoLore.add("§7Cost: §6" + session.eliminatedBoots.size() + " Boot Shards");
            echoMeta.setLore(echoLore);
            echoShard.setItemMeta(echoMeta);
        }
        gui.setItem(13, echoShard);

        // Surrounding slots for boots
        int[] bootSlots = {1, 2, 3, 10, 11, 12, 19, 20, 21};
        int slotIndex = 0;

        for (BootType bootType : BootType.values()) {
            if (slotIndex >= bootSlots.length) break;

            boolean eliminated = session.eliminatedBoots.contains(bootType);
            ItemStack bootItem = createBootItem(bootType, eliminated, session);
            gui.setItem(bootSlots[slotIndex++], bootItem);
        }

        // Info item in slot 25
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName("§e§lHow to Use");
            List<String> infoLore = new ArrayList<>();
            infoLore.add("§7Left-click boots to eliminate them.");
            infoLore.add("§7Eliminated boots won't appear in rerolls.");
            infoLore.add("§7Each elimination increases shard cost.");
            infoLore.add("");
            infoLore.add("§7Max eliminations: §e" + session.maxEliminations);
            infoMeta.setLore(infoLore);
            info.setItemMeta(infoMeta);
        }
        gui.setItem(25, info);

        // Optional: fill empty slots with glass to look cleaner
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        if (fm != null) {
            fm.setDisplayName(" ");
            filler.setItemMeta(fm);
        }
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }

        return gui;
    }

    /**
     * Creates the visual item for a given BootType for the GUI.
     * Uses BootsManager.createBootsItem(Player, BootsData) correctly.
     */
    private ItemStack createBootItem(BootType bootType, boolean eliminated, RerollerSession session) {
        BootsData tempData = new BootsData(bootType);
        tempData.setTier(BootsTier.TIER_1);

        Player p = Bukkit.getPlayer(session.playerId);
        ItemStack item;

        if (p != null) {
            // ✅ Use the correct method signature from BootsManager
            item = plugin.getBootsManager().createBootsItem(p, tempData);
        } else {
            // Fallback in case player somehow went offline mid-process
            item = new ItemStack(Material.DIAMOND_BOOTS);
            ItemMeta fallbackMeta = item.getItemMeta();
            if (fallbackMeta != null) {
                fallbackMeta.setDisplayName(bootType.getColoredName());
                item.setItemMeta(fallbackMeta);
            }
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (eliminated) {
                meta.setDisplayName("§c§m" + bootType.getDisplayName());
                List<String> lore = new ArrayList<>();
                lore.add("§c§lELIMINATED");
                lore.add("§7This boot won't appear in rerolls.");
                lore.add("");
                lore.add("§7Left-click to restore.");
                meta.setLore(lore);
            } else {
                // Make sure non-eliminated boots still have a clear hint
                List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add("");
                lore.add("§7Left-click to eliminate.");
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }

        return item;
    }

    /* ======================================================================
       CLICK HANDLING
    ======================================================================= */

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (!title.equals("§6§lBoot Reroller - Elimination")) return;

        // Prevent item movement/stealing
        event.setCancelled(true);

        RerollerSession session = activeSessions.get(player.getUniqueId());
        if (session == null) {
            player.closeInventory();
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        // Echo Shard -> right-click to reroll
        if (clicked.getType() == Material.ECHO_SHARD && event.isRightClick()) {
            performReroll(player, session);
            return;
        }

        // Boots -> left-click to eliminate / restore
        if (clicked.getType() == Material.DIAMOND_BOOTS && event.isLeftClick()) {
            String displayName = clicked.getItemMeta().getDisplayName();

            for (BootType bootType : BootType.values()) {
                String rawName = ChatColor.stripColor(displayName);
                String rawTypeName = ChatColor.stripColor(bootType.getColoredName());
                String rawEnumName = bootType.name();

                if (rawName.contains(rawEnumName) || rawName.contains(rawTypeName) || rawName.contains(bootType.getDisplayName())) {
                    toggleBootElimination(player, session, bootType);
                    break;
                }
            }
        }
    }

    /* ======================================================================
       ELIMINATION TOGGLING
    ======================================================================= */

    private void toggleBootElimination(Player player, RerollerSession session, BootType bootType) {
        if (session.eliminatedBoots.contains(bootType)) {
            // Restore boot
            session.eliminatedBoots.remove(bootType);
            player.sendMessage("§a✦ Restored " + bootType.getColoredName());
        } else {
            // Eliminate boot
            if (session.eliminatedBoots.size() >= session.maxEliminations) {
                player.sendMessage("§c✦ Maximum eliminations reached! (" + session.maxEliminations + ")");
                return;
            }

            int requiredShards = session.eliminatedBoots.size() + 1;
            if (!hasBootShards(player, requiredShards)) {
                player.sendMessage("§c✦ Not enough Boot Shards! You need " + requiredShards + " total.");
                return;
            }

            session.eliminatedBoots.add(bootType);
            player.sendMessage("§c✦ Eliminated " + bootType.getColoredName());
        }

        // Refresh GUI with updated eliminations
        player.openInventory(createRerollerGUI(session));
    }

    /* ======================================================================
       PERFORM REROLL
    ======================================================================= */

    private void performReroll(Player player, RerollerSession session) {
        PlayerData data = plugin.getPlayerManager().getPlayerData(player);

        // Cost = number of eliminated boots
        int shardCost = session.eliminatedBoots.size();

        if (!hasBootShards(player, shardCost)) {
            player.sendMessage("§c✦ Not enough Boot Shards! You need " + shardCost + ".");
            return;
        }

        if (shardCost > 0) {
            removeBootShards(player, shardCost);
        }

        // Build pool of available boots (all minus eliminated)
        List<BootType> availableBoots = new ArrayList<>(Arrays.asList(BootType.values()));
        availableBoots.removeAll(session.eliminatedBoots);

        if (availableBoots.isEmpty()) {
            player.sendMessage("§c✦ No boots available to reroll! (All eliminated)");
            return;
        }

        // Random new boot type
        BootType newBoot = availableBoots.get(new Random().nextInt(availableBoots.size()));

        // Actually reroll boots & apply
        plugin.getPlayerManager().rerollPlayerBoots(player);
        data.getBootsData().setBootType(newBoot);
        plugin.getBootsManager().giveBoots(player, newBoot);
        plugin.getPlayerManager().savePlayerData(data);

        // Refresh particles / displays
        plugin.getUnifiedDisplayManager().refreshPlayerDisplays(player);

        // Close GUI and clear session
        player.closeInventory();
        activeSessions.remove(player.getUniqueId());

        player.sendMessage("§a§l✦ REROLL SUCCESSFUL!");
        player.sendMessage("§7Your new boots: " + newBoot.getColoredName());
    }

    /* ======================================================================
       INVENTORY CLOSE
    ======================================================================= */

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();

        String title = event.getView().getTitle();
        if (title.equals("§6§lBoot Reroller - Elimination")) {
            // Session is cancelled on close
            activeSessions.remove(player.getUniqueId());
        }
    }

    /* ======================================================================
       SHARD HELPERS
    ======================================================================= */

    private boolean hasBootShards(Player player, int amount) {
        if (amount <= 0) return true;

        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && plugin.getCustomItemManager().isBootShardItem(item)) {
                count += item.getAmount();
            }
        }
        return count >= amount;
    }

    private void removeBootShards(Player player, int amount) {
        int remaining = amount;
        if (remaining <= 0) return;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && plugin.getCustomItemManager().isBootShardItem(item)) {
                int stackAmount = item.getAmount();

                if (stackAmount <= remaining) {
                    remaining -= stackAmount;
                    item.setAmount(0);
                } else {
                    item.setAmount(stackAmount - remaining);
                    remaining = 0;
                }

                if (remaining <= 0) break;
            }
        }
    }

    /* ======================================================================
       SESSION CLASS
    ======================================================================= */

    private static class RerollerSession {
        final UUID playerId;
        final int maxEliminations;
        final Set<BootType> eliminatedBoots;

        RerollerSession(UUID playerId, int maxEliminations) {
            this.playerId = playerId;
            this.maxEliminations = maxEliminations;
            this.eliminatedBoots = new HashSet<>();
        }
    }
}
