package com.bootstier.commands;

import com.bootstier.BootsTierPlugin;
import com.bootstier.boots.BootType;
import com.bootstier.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class CooldownsCommand implements CommandExecutor, Listener {

    private final BootsTierPlugin plugin;

    public CooldownsCommand(final BootsTierPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }

        Player p = (Player) sender;

        if (!p.isOp()) {
            p.sendMessage(ChatColor.RED + "This command is OP-only!");
            return true;
        }

        openBootSelectionGUI(p);
        return true;
    }

    /* ========================================================
                GUI #1 — Select Boot Type
     ======================================================== */
    private void openBootSelectionGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "§6§lCooldowns - Select Boot");

        int slot = 10;
        for (BootType type : BootType.values()) {
            ItemStack item = new ItemStack(Material.DIAMOND_BOOTS);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(type.getColoredName());
                meta.setLore(Collections.singletonList("§7Click to view ability cooldowns"));
                item.setItemMeta(meta);
            }
            gui.setItem(slot++, item);
        }

        player.openInventory(gui);
    }

    /* ========================================================
                GUI #2 — Show Cooldowns For That Boot
     ======================================================== */
    private void openCooldownsGUI(Player viewer, BootType bootType) {
        Inventory gui = Bukkit.createInventory(null, 27, "§6§l" + bootType.getDisplayName() + " Cooldowns");

        PlayerData data = plugin.getPlayerManager().getPlayerData(viewer);
        if (data == null || data.getBootsData() == null) {
            gui.setItem(13, createItem(Material.BARRIER, "§cNo boots equipped", "§7This player is not wearing any boots."));
            viewer.openInventory(gui);
            return;
        }

        /* ===== Get actual cooldown values from metadata ===== */

        Map<String, Long> cds = getCooldownMap(viewer, bootType);

        // Ability 1
        long cd1 = cds.getOrDefault("t1", 0L);
        gui.setItem(11, createCooldownItem(getAbilityName(bootType, 1), cd1));

        // Ability 2
        long cd2 = cds.getOrDefault("t2", 0L);
        gui.setItem(15, createCooldownItem(getAbilityName(bootType, 2), cd2));

        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backm = back.getItemMeta();
        backm.setDisplayName("§cBack");
        back.setItemMeta(backm);
        gui.setItem(22, back);

        viewer.openInventory(gui);
    }

    /* ========================================================
                     Create GUI Cooldown Item
     ======================================================== */
    private ItemStack createCooldownItem(String abilityName, long cd) {
        Material mat = cd > 0 ? Material.RED_DYE : Material.LIME_DYE;
        String status = cd > 0 ? "§cCooldown: §e" + (cd / 1000) + "s" : "§aReady";

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(abilityName);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(status);

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    private ItemStack createItem(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Collections.singletonList(lore));
        item.setItemMeta(meta);
        return item;
    }

    /* ========================================================
                 ABILITY NAME LOOKUP (DEV DOC ACCURATE)
     ======================================================== */
    private String getAbilityName(BootType type, int tier) {
        return switch (type) {
            case SPEED -> tier == 1 ? "§bBlur" : "§bQuickstep";
            case STRENGTH -> tier == 1 ? "§cCritical Surge" : "§cDamage Link";
            case WARD -> tier == 1 ? "§5True Invisibility" : "§5Sculk Sensor";
            case SPIDER -> tier == 1 ? "§2Spider Swarm" : "§2Web Shot";
            case FROST -> tier == 1 ? "§bFrost Shield" : "§bShroud of Frost";
            case WIND -> tier == 1 ? "§fWind Dash" : "§fTornado";
            case ASTRAL -> tier == 1 ? "§dAstral Rewind" : "§dBoot Disable";
            case LIFE -> tier == 1 ? "§aOpposite Heal" : "§aWeakening Aura";
            case WATER -> tier == 1 ? "§9Whirlpool" : "§9Wave Burst";
            case FIRE -> tier == 1 ? "§cFire Rings" : "§cBlazing Dash";
            default -> "§7Unknown";
        };
    }

    /* ========================================================
               READ COOLDOWNS FROM METADATA PER BOOT
     ======================================================== */
    private Map<String, Long> getCooldownMap(Player p, BootType type) {
        long now = System.currentTimeMillis();
        Map<String, Long> map = new HashMap<>();

        /* ----- SPEED / BLUR ----- */
        if (type == BootType.SPEED) {
            map.put("t1", getCD(p, "blur_until", now));
            map.put("t2", getCD(p, "quickstep_until", now));
        }

        /* ----- STRENGTH ----- */
        if (type == BootType.STRENGTH) {
            map.put("t1", getCD(p, "strength_critical_active", now));
            map.put("t2", getCD(p, "damage_link_active", now));
        }

        /* ----- WARD ----- */
        if (type == BootType.WARD) {
            map.put("t1", getCD(p, "ward_invisible_until", now));
            map.put("t2", getCD(p, "ward_sensor_active_until", now));
        }

        /* ----- SPIDER ----- */
        if (type == BootType.SPIDER) {
            map.put("t1", getCD(p, "spider_summon_until", now));
            map.put("t2", getCD(p, "spider_web_until", now));
        }

        /* ----- FROST ----- */
        if (type == BootType.FROST) {
            map.put("t1", getCD(p, "frost_shield_until", now));
            map.put("t2", getCD(p, "frost_circle_until", now));
        }

        /* ----- WIND ----- */
        if (type == BootType.WIND) {
            map.put("t1", getCD(p, "wind_dash_until", now));
            map.put("t2", getCD(p, "wind_tornado_until", now));
        }

        /* ----- ASTRAL ----- */
        if (type == BootType.ASTRAL) {
            map.put("t1", getCD(p, "astral_rewind_until", now));
            map.put("t2", getCD(p, "astral_disable_until", now));
        }

        /* ----- LIFE ----- */
        if (type == BootType.LIFE) {
            map.put("t1", getCD(p, "life_reverse_until", now));
            map.put("t2", getCD(p, "life_weak_until", now));
        }

        /* ----- WATER ----- */
        if (type == BootType.WATER) {
            map.put("t1", getCD(p, "water_whirlpool_until", now));
            map.put("t2", getCD(p, "water_wave_until", now));
        }

        /* ----- FIRE ----- */
        if (type == BootType.FIRE) {
            map.put("t1", getCD(p, "fire_rings_until", now));
            map.put("t2", getCD(p, "fire_dash_until", now));
        }

        return map;
    }

    /* ========================================================
                    HELPER — METADATA COOLDOWN
     ======================================================== */
    private long getCD(Player p, String key, long now) {
        if (!p.hasMetadata(key)) return 0;
        long until = p.getMetadata(key).get(0).asLong();
        return Math.max(0, until - now);
    }

    /* ========================================================
                Inventory Click Event Handler
     ======================================================== */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;

        Player p = (Player) e.getWhoClicked();
        String title = e.getView().getTitle();

        if (!title.startsWith("§6§lCooldowns")) return;

        e.setCancelled(true);

        if (title.equals("§6§lCooldowns - Select Boot")) {
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;

            String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
            for (BootType type : BootType.values()) {
                if (name.equalsIgnoreCase(type.getDisplayName())) {
                    openCooldownsGUI(p, type);
                    return;
                }
            }
            return;
        }

        // BACK
        if (e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.ARROW) {
            openBootSelectionGUI(p);
        }
    }
}
