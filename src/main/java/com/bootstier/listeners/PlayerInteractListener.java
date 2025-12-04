package com.bootstier.listeners;

import com.bootstier.BootsTierPlugin;
import com.bootstier.boots.BootType;
import com.bootstier.boots.abilities.impl.LifeAbility;
import com.bootstier.boots.abilities.impl.SpeedAbility;
import com.bootstier.items.CustomItemManager;
import com.bootstier.player.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles player interaction events
 */
public class PlayerInteractListener implements Listener {

    private final BootsTierPlugin plugin;

    public PlayerInteractListener(final BootsTierPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        final ItemStack item = event.getItem();

        if (item == null) {
            // Life Boots: plant growth on sneak+right click block
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK && player.isSneaking()) {
                final PlayerData data = this.plugin.getPlayerManager().getPlayerData(player);
                if (data.getBootsData() != null && !data.areBootsBroken()
                        && data.getBootsData().getBootType() == BootType.LIFE) {

                    final LifeAbility lifeAbility =
                            (LifeAbility) this.plugin.getAbilityManager().getAbilities().get(BootType.LIFE);
                    if (event.getClickedBlock() != null) {
                        lifeAbility.handlePlantGrowth(player, event.getClickedBlock().getLocation());
                        event.setCancelled(true);
                    }
                }
            }
            return;
        }

        // Handle Speed Boots insta-gap (golden apple right-click during blur)
        if ((item.getType() == Material.GOLDEN_APPLE || item.getType() == Material.ENCHANTED_GOLDEN_APPLE) &&
            (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            
            final PlayerData data = this.plugin.getPlayerManager().getPlayerData(player);
            if (data.getBootsData() != null && !data.areBootsBroken() && 
                data.getBootsData().getBootType() == BootType.SPEED &&
                player.hasMetadata("speed_blur_active")) {
                
                final SpeedAbility speedAbility = (SpeedAbility) this.plugin.getAbilityManager().getAbilities().get(BootType.SPEED);
                speedAbility.handleInstaGapUse(player);
                event.setCancelled(true);
                return;
            }
        }

        final CustomItemManager itemManager = this.plugin.getCustomItemManager();

        if (itemManager.isRerollItem(item)) {
            this.handleRerollItem(player, item, event);
        } else if (itemManager.isTierBoxItem(item)) {
            this.handleTierBoxItem(player, item, event);
        } else if (itemManager.isRepairBoxItem(item)) {
            this.handleRepairBoxItem(player, item, event);
        } else if (itemManager.isBootShardItem(item)) {
            this.handleBootShardItem(player, item, event);
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(final PlayerInteractEntityEvent event) {
        final Player player = event.getPlayer();
        final Entity entity = event.getRightClicked();

        // Life Boots: animal feeding on sneak+right click entity
        if (player.isSneaking() && entity instanceof Animals) {
            final PlayerData data = this.plugin.getPlayerManager().getPlayerData(player);
            if (data.getBootsData() != null && !data.areBootsBroken()
                    && data.getBootsData().getBootType() == BootType.LIFE) {

                final LifeAbility lifeAbility =
                        (LifeAbility) this.plugin.getAbilityManager().getAbilities().get(BootType.LIFE);
                lifeAbility.handleAnimalFeeding(player, entity);
                event.setCancelled(true);
            }
        }
    }

    private void handleRerollItem(final Player player, final ItemStack item, final PlayerInteractEvent event) {
        event.setCancelled(true);
        final PlayerData data = this.plugin.getPlayerManager().getPlayerData(player);

        if (data.getBootsData() == null) {
            player.sendMessage(ChatColor.RED + "You don't have any boots to reroll!");
            return;
        }

        if (data.areBootsBroken()) {
            player.sendMessage(ChatColor.RED + "Your boots are broken! Repair them first.");
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // Quick reroll
            item.setAmount(item.getAmount() - 1);
            this.plugin.getPlayerManager().rerollPlayerBoots(player);
            this.plugin.getBootsManager().giveBoots(player, data.getBootsData().getBootType());
            this.plugin.getUnifiedDisplayManager().refreshPlayerDisplays(player);
            player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Your boots have been rerolled to "
                    + data.getBootsData().getBootType().getColoredName() + ChatColor.GREEN + "!");
        } else if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            // Open elimination GUI
            RerollerGuiListener listener = new RerollerGuiListener(plugin);
            plugin.getServer().getPluginManager().registerEvents(listener, plugin);
            listener.openRerollerGUI(player);
        }
    }

    private void handleTierBoxItem(final Player player, final ItemStack item, final PlayerInteractEvent event) {
        event.setCancelled(true);
        final PlayerData data = this.plugin.getPlayerManager().getPlayerData(player);

        if (data.getBootsData() == null) {
            player.sendMessage(ChatColor.RED + "You don't have any boots!");
            return;
        }

        if (data.areBootsBroken()) {
            player.sendMessage(ChatColor.RED + "Your boots are broken! Repair them first.");
            return;
        }

        if (data.getBootsData().getTier().getLevel() >= 2) {
            player.sendMessage(ChatColor.RED + "Your boots are already at maximum tier!");
            return;
        }

        // Consume item
        item.setAmount(item.getAmount() - 1);

        // Upgrade tier
        this.plugin.getBootsManager().upgradeTier(player);

        player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Your boots have been upgraded to Tier II!");
    }

    private void handleRepairBoxItem(final Player player, final ItemStack item, final PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            player.sendMessage(ChatColor.RED + "Right-click the active pedestal beacon to start the ritual.");
            return;
        }

        final Block clicked = event.getClickedBlock();
        if (clicked.getType() != Material.BEACON) {
            player.sendMessage(ChatColor.RED + "Right-click the active pedestal beacon to start the ritual.");
            return;
        }

        // Must be the sacred pedestal beacon and active
        if (!plugin.getPedestalManager().isActive()) {
            player.sendMessage(ChatColor.RED + "The pedestal is not active!");
            return;
        }
        if (!plugin.getPedestalManager().isAtPedestal(clicked.getLocation())) {
            player.sendMessage(ChatColor.RED + "This is not the sacred pedestal beacon.");
            return;
        }

        final PlayerData data = this.plugin.getPlayerManager().getPlayerData(player);
        if (!data.areBootsBroken()) {
            player.sendMessage(ChatColor.RED + "Your boots are not broken!");
            return;
        }

        // All good — consume box and start NEW ritual
        event.setCancelled(true);
        item.setAmount(item.getAmount() - 1);

        plugin.getRitualManager().startRitual(player);
    }

    private void handleBootShardItem(final Player player, final ItemStack item, final PlayerInteractEvent event) {
        event.setCancelled(true);
        if (plugin.getRitualManager().hasActiveRitual(player)) {
            plugin.getRitualManager().addShardToRitual(player);
            return;
        }
        
        // Otherwise consume for life
        this.plugin.getLivesManager().consumeBootShard(player, item);
    }
}
