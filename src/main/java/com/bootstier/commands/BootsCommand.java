package com.bootstier.commands;

import com.bootstier.BootsTierPlugin;
import com.bootstier.boots.BootType;
import com.bootstier.boots.BootsTier;
import com.bootstier.boots.BootsData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class BootsCommand implements CommandExecutor {

    private final BootsTierPlugin plugin;

    public BootsCommand(final BootsTierPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }

        final Player player = (Player) sender;

        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "This command is OP-only!");
            return true;
        }

        openBootsGUI(player);
        return true;
    }

    private void openBootsGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "§6§lBoots SMP - Master Showcase");

        int slot = 0;
        for (BootType bootType : BootType.values()) {
            BootsData bootsData = new BootsData(bootType);
            bootsData.setTier(BootsTier.TIER_1);
gui.setItem(slot++, plugin.getBootsManager().createBootsItem(player, bootsData));

        }

        slot = 9;
        for (BootType bootType : BootType.values()) {
            BootsData bootsData = new BootsData(bootType);
            bootsData.setTier(BootsTier.TIER_2);
gui.setItem(slot++, plugin.getBootsManager().createBootsItem(player, bootsData));

        }

        gui.setItem(27, plugin.getLivesManager().getBootShardManager().createBootShard());
        gui.setItem(28, createInfoItem(Material.ECHO_SHARD, "§dEcho Shard", 
            "§7Used in Reroller system", "§7to eliminate boot types"));

        gui.setItem(30, plugin.getCustomItemManager().createRerollItem());
        gui.setItem(31, plugin.getCustomItemManager().createTierBoxItem());
        gui.setItem(32, plugin.getCustomItemManager().createRepairBoxItem());

        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }
        for (int i = 0; i < 54; i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }

        player.openInventory(gui);
    }

    private ItemStack createInfoItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(line);
            }
            meta.setLore(loreList);
            item.setItemMeta(meta);
        }
        return item;
    }
}
