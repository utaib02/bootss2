package com.bootstier.items;

import com.bootstier.BootsTierPlugin;
import com.bootstier.ritual.PedestalManager;
import com.bootstier.utils.ItemUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * Handles creation, identification, and crafting of all custom items.
 */
public class CustomItemManager implements Listener {

    private final BootsTierPlugin plugin;

    public CustomItemManager(final BootsTierPlugin plugin) {
        this.plugin = plugin;
        registerAllRecipes();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /* ---------------------------------------------
       ITEM CREATION
    --------------------------------------------- */

    /**
     * Creates a boot shard item by delegating to BootShardManager.
     * This ensures all boot shards are created identically.
     */
    public ItemStack createBootShardItem() {
        return plugin.getLivesManager().getBootShardManager().createBootShard();
    }

    public ItemStack createRerollItem() {
        final ItemStack reroll = new ItemStack(Material.PITCHER_POD);
        final ItemMeta meta = reroll.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§l✦ Boot Reroller ✦");
            meta.setLore(Arrays.asList(
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7Right-click to §6reforge §7your boots",
                    "§7Preserves §etier §7and §clives",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"

            ));
            ItemUtils.addGlow(meta);
            reroll.setItemMeta(meta);
        }
        return reroll;
    }

    public ItemStack createTierBoxItem() {
        final ItemStack tierBox = new ItemStack(Material.MAGENTA_GLAZED_TERRACOTTA);
        final ItemMeta meta = tierBox.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§5§l✦ Ascension Box ✦");
            meta.setLore(Arrays.asList(
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7Right-click to §5ascend §7your boots",
                    "§7Upgrades from §eTier I §7to §6Tier II",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
            ));
            ItemUtils.addGlow(meta);
            tierBox.setItemMeta(meta);
        }
        return tierBox;
    }

    public ItemStack createRepairBoxItem() {
        final ItemStack repairBox = new ItemStack(Material.RECOVERY_COMPASS);
        final ItemMeta meta = repairBox.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c§l✦ Restoration Catalyst ✦");
            meta.setLore(Arrays.asList(
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§7Right-click a §ebeacon pedestal §7to start a ritual",
                    "§7Repairs §cbroken boots §7and restores §elives",
                    "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                    "§c§lDIVINE ARTIFACT",
                    "§8\"From ashes, we rise again\""
            ));
            ItemUtils.addGlow(meta);
            repairBox.setItemMeta(meta);
        }
        return repairBox;
    }

    /* ---------------------------------------------
       RECIPE REGISTRATION
    --------------------------------------------- */

    private void registerAllRecipes() {
        registerRerollRecipe();
        registerTierBoxRecipe();
        registerRepairBoxRecipe();
    }

    /** Reroller recipe */
    private void registerRerollRecipe() {
        final ItemStack result = createRerollItem();
        final ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "boot_reroller"), result);
        recipe.shape("WNP", "TBT", "RGL");

        recipe.setIngredient('W', Material.WIND_CHARGE);
        recipe.setIngredient('N', Material.NETHERITE_INGOT);
        recipe.setIngredient('P', new RecipeChoice.MaterialChoice(
            Material.POTION,
            Material.SPLASH_POTION,
            Material.LINGERING_POTION
        ));
        recipe.setIngredient('T', Material.OMINOUS_TRIAL_KEY);
        recipe.setIngredient('B', Material.DIAMOND_BOOTS);
        recipe.setIngredient('R', Material.RABBIT_FOOT);
        recipe.setIngredient('G', Material.GOLDEN_APPLE);
        recipe.setIngredient('L', Material.NAUTILUS_SHELL);

        try {
            plugin.getServer().addRecipe(recipe);
            plugin.getLogger().info("§a[Recipes] Registered Boot Reroller recipe successfully!");
        } catch (Exception e) {
            plugin.getLogger().warning("§c[Recipes] Failed to register Boot Reroller recipe: " + e.getMessage());
        }
    }

    /** Tier Box recipe */
    private void registerTierBoxRecipe() {
        final ItemStack result = createTierBoxItem();
        final ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "tier_box"), result);
        recipe.shape("EUE", "NHN", "EUE");

        recipe.setIngredient('E', Material.ECHO_SHARD);
        recipe.setIngredient('U', Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        recipe.setIngredient('H', Material.HEAVY_CORE);
        recipe.setIngredient('N', Material.NETHERITE_INGOT);

        try {
            plugin.getServer().addRecipe(recipe);
            plugin.getLogger().info("Registered Tier Box recipe successfully!");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to register Tier Box recipe: " + e.getMessage());
        }
    }

    /** Repair Box recipe (BEB / RTR / NEN) */
    private void registerRepairBoxRecipe() {
        final ItemStack result = createRepairBoxItem();
        final ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "repair_box"), result);
        recipe.shape("BEB", "RTR", "NEN");

        // Use exact match for custom items via RecipeChoice
        final RecipeChoice.ExactChoice bootShardChoice = new RecipeChoice.ExactChoice(
            plugin.getLivesManager().getBootShardManager().createBootShard()
        );
        final RecipeChoice.ExactChoice rerollerChoice = new RecipeChoice.ExactChoice(createRerollItem());
        final RecipeChoice.ExactChoice tierBoxChoice = new RecipeChoice.ExactChoice(createTierBoxItem());

        recipe.setIngredient('B', Material.HEAVY_CORE);
        recipe.setIngredient('E', Material.ENCHANTED_GOLDEN_APPLE);
        recipe.setIngredient('R', rerollerChoice);
        recipe.setIngredient('T', tierBoxChoice);
        recipe.setIngredient('N', Material.NETHERITE_INGOT);

        try {
            plugin.getServer().addRecipe(recipe);
            plugin.getLogger().info("§a[Recipes] Registered Repair Box recipe successfully!");
        } catch (Exception e) {
            plugin.getLogger().warning("§c[Recipes] Failed to register Repair Box recipe: " + e.getMessage());
        }
    }

    /* ---------------------------------------------
       ITEM PROTECTION & USAGE
    --------------------------------------------- */

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        ItemStack item = e.getItemInHand();
        if (item == null) return;
        if (plugin.getLivesManager().getBootShardManager().isBootShard(item) || 
            isRerollItem(item) || isTierBoxItem(item) || isRepairBoxItem(item)) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§c✦ You cannot place this sacred item!");
        }
    }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        ItemStack item = e.getItem();
        if (item == null || !isRepairBoxItem(item)) return;
        if (e.getClickedBlock() == null) return;

        Block block = e.getClickedBlock();
        if (block.getType() != Material.BEACON) return;

        e.setCancelled(true);
        Player player = e.getPlayer();

        PedestalManager pedestal = plugin.getPedestalManager();
        if (pedestal == null || !pedestal.isActive()) {
            player.sendMessage("§c✦ The pedestal is not active!");
            return;
        }

        item.setAmount(item.getAmount() - 1);
        pedestal.handleRepairBoxUse(player, item);
    }

    /* ---------------------------------------------
       IDENTIFIERS
    --------------------------------------------- */

    public boolean isRerollItem(ItemStack item) {
        return ItemUtils.hasCustomName(item, "§6§l✦ Boot Reroller ✦");
    }

    public boolean isTierBoxItem(ItemStack item) {
        return ItemUtils.hasCustomName(item, "§5§l✦ Ascension Box ✦");
    }

    public boolean isRepairBoxItem(ItemStack item) {
        return ItemUtils.hasCustomName(item, "§c§l✦ Restoration Catalyst ✦");
    }

    public boolean isBootShardItem(ItemStack item) {
        return plugin.getLivesManager().getBootShardManager().isBootShard(item);
    }
}
