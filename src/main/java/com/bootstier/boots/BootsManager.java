package com.bootstier.boots;

import com.bootstier.BootsTierPlugin;
import com.bootstier.boots.abilities.AbilityManager;
import com.bootstier.player.PlayerData;
import com.bootstier.utils.ItemUtils;
import com.bootstier.utils.MessageUtils;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages boots-related operations with v4 enhancements
 */
public class BootsManager {

    private final BootsTierPlugin plugin;

    public BootsManager(final BootsTierPlugin plugin) {
        this.plugin = plugin;
    }

    public void giveBoots(final Player player, final BootType bootType) {
        final PlayerData data = this.plugin.getPlayerManager().getPlayerData(player);
        
        if (data.getBootsData() == null) {
            data.setBootsData(new BootsData(bootType));
        } else {
            data.getBootsData().setBootType(bootType);
        }
        
        final ItemStack boots = this.createBootsItem(player, data.getBootsData());
        player.getInventory().setBoots(boots);
        
        this.plugin.getPlayerManager().savePlayerData(data);
    }

    public ItemStack createBootsItem(final Player player, final BootsData bootsData) {
        final PlayerData playerData = this.plugin.getPlayerManager().getPlayerData(player);
        return this.createBootsItemInternal(bootsData, playerData.getLives());
    }

    private ItemStack createBootsItemInternal(final BootsData bootsData, final int lives) {
        final ItemStack boots = new ItemStack(Material.DIAMOND_BOOTS);
        final ItemMeta meta = boots.getItemMeta();
        
        if (meta != null) {
            // Set display name with v4 styling
            meta.setDisplayName(bootsData.getBootType().getStyledName() + " " + bootsData.getTier().getRoman());
            
            // Apply armor trim
            this.applyArmorTrim(meta, bootsData.getBootType());
            
            // Add enchantments based on lives and tier
            this.addBootsEnchantments(meta, bootsData, lives);
            
            // Make unbreakable
            meta.setUnbreakable(true);
            
            // Hide attributes and enchants for cleaner look
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE);
            
            List<String> lore = new ArrayList<>();
            
            // Enchants section (auto-populated from ItemMeta)
            lore.add("§eEnchantments:");
            lore.add("§7• Protection IV");
            if (lives >= 6) lore.add("§7• Mending");
            if (lives >= 7) lore.add("§7• Unbreaking III");
            if (lives >= 8) lore.add("§7• Depth Strider III");
            if (lives >= 9) lore.add("§7• Soul Speed III");
            if (lives >= 10) lore.add("§7• Aqua Affinity");
            
            lore.add("");
            lore.add("§bPassives:");
            // Add boot-specific passives
            switch (bootsData.getBootType()) {
                case SPEED:
                    lore.add("§7• Speed I/II");
                    break;
                case STRENGTH:
                    lore.add("§7• Strength I");
                    break;
                case WARD:
                    lore.add("§7• Echo Sense");
                    break;
                case SPIDER:
                    lore.add("§7• Web Walking");
                    break;
                case FROST:
                    lore.add("§7• Frost Aura");
                    break;
                case WIND:
                    lore.add("§7• Speed Boost");
                    lore.add("§7• Double Jump");
                    break;
                case ASTRAL:
                    lore.add("§7• Cosmic Shield");
                    break;
                case LIFE:
                    lore.add("§7• Life Force");
                    break;
                case WATER:
                    lore.add("§7• Water Breathing");
                    break;
                case FIRE:
                    lore.add("§7• Fire Resistance");
                    break;
            }
            
            lore.add("");
            lore.add("§cAbilities:");
            this.addMinimalAbilitiesLore(lore, bootsData);
            
            meta.setLore(lore);
            boots.setItemMeta(meta);
        }
        
        return boots;
    }

    private void addMinimalAbilitiesLore(List<String> lore, BootsData bootsData) {
        switch (bootsData.getBootType()) {
            case SPEED:
                if (bootsData.getTier() == BootsTier.TIER_1) {
                    lore.add("§7• Blur");
                } else {
                    lore.add("§7• Thunder Strike");
                }
                break;
            case STRENGTH:
                if (bootsData.getTier() == BootsTier.TIER_1) {
                    lore.add("§7• Critical Hits");
                } else {
                    lore.add("§7• Damage Link");
                }
                break;
            case WARD:
                if (bootsData.getTier() == BootsTier.TIER_1) {
                    lore.add("§7• True Invisibility");
                } else {
                    lore.add("§7• Sculk Sensor");
                }
                break;
            case SPIDER:
                if (bootsData.getTier() == BootsTier.TIER_1) {
                    lore.add("§7• Spider Summon");
                } else {
                    lore.add("§7• Web Fireball");
                }
                break;
            case FROST:
                if (bootsData.getTier() == BootsTier.TIER_1) {
                    lore.add("§7• Damage Shield");
                } else {
                    lore.add("§7• Ice Circle");
                }
                break;
            case WIND:
                if (bootsData.getTier() == BootsTier.TIER_1) {
                    lore.add("§7• Dash");
                } else {
                    lore.add("§7• Tornado");
                }
                break;
            case ASTRAL:
                if (bootsData.getTier() == BootsTier.TIER_1) {
                    lore.add("§7• Rewind");
                } else {
                    lore.add("§7• Boot Disable");
                }
                break;
            case LIFE:
                if (bootsData.getTier() == BootsTier.TIER_1) {
                    lore.add("§7• Opposite Heal");
                } else {
                    lore.add("§7• Weakness Circle");
                }
                break;
            case WATER:
                if (bootsData.getTier() == BootsTier.TIER_1) {
                    lore.add("§7• Whirlpool");
                } else {
                    lore.add("§7• Wave Push");
                }
                break;
            case FIRE:
                if (bootsData.getTier() == BootsTier.TIER_1) {
                    lore.add("§7• Fire Rings");
                } else {
                    lore.add("§7• Marking Dash");
                }
                break;
        }
    }

    private void addPremiumAbilityLore(final List<String> lore, final BootsData bootsData) {
    }

    private String getBootEssenceDescription(final BootType bootType) {
        switch (bootType) {
            case SPEED: return "Lightning's Embrace";
            case STRENGTH: return "Titan's Might";
            case WARD: return "Shadow's Veil";
            case SPIDER: return "Arachnid's Web";
            case FROST: return "Winter's Wrath";
            case WIND: return "Storm's Fury";
            case ASTRAL: return "Cosmic Power";
            case LIFE: return "Nature's Gift";
            case WATER: return "Ocean's Depth";
            case FIRE: return "Inferno's Heart";
            default: return "Unknown Essence";
        }
    }


    private void applyArmorTrim(final ItemMeta meta, final BootType bootType) {
        if (meta instanceof ArmorMeta && this.plugin.getConfigManager().isArmorTrimsEnabled()) {
            final ArmorMeta armorMeta = (ArmorMeta) meta;
            try {
                final ArmorTrim trim = new ArmorTrim(bootType.getTrimMaterial(), bootType.getTrimPattern());
                armorMeta.setTrim(trim);
            } catch (final Exception e) {
                this.plugin.getLogger().warning("Failed to apply armor trim for " + bootType.name() + ": " + e.getMessage());
            }
        }
    }

    private void addBootsEnchantments(final ItemMeta meta, final BootsData bootsData, final int lives) {
        if (bootsData.isBroken()) {
            meta.addEnchant(Enchantment.PROTECTION, 3, true);
        } else {
            if (lives == 1) {
                meta.addEnchant(Enchantment.PROTECTION, 1, true);
            } else if (lives == 2) {
                meta.addEnchant(Enchantment.PROTECTION, 1, true);
            } else if (lives == 3) {
                meta.addEnchant(Enchantment.PROTECTION, 2, true);
            } else if (lives == 4) {
                meta.addEnchant(Enchantment.PROTECTION, 3, true);
            } else if (lives >= 5) {
                // Lives >= 5: full enchants according to dev doc
                meta.addEnchant(Enchantment.PROTECTION, 4, true);
                if (lives >= 6) meta.addEnchant(Enchantment.MENDING, 1, true);
                if (lives >= 7) meta.addEnchant(Enchantment.UNBREAKING, 3, true);
                if (lives >= 8) meta.addEnchant(Enchantment.DEPTH_STRIDER, 3, true);
                if (lives >= 9) meta.addEnchant(Enchantment.SOUL_SPEED, 3, true);
                if (lives >= 10) meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
            }
        }
    }

    public void checkBootBreaking(final Player player) {
        final PlayerData data = this.plugin.getPlayerManager().getPlayerData(player);
        
        if (data.getLives() <= 0 && !data.areBootsBroken()) {
            this.breakBoots(player);
        }
    }

    public void checkLowLifeWarning(final Player player) {
        final PlayerData data = this.plugin.getPlayerManager().getPlayerData(player);
        
        if (data.getLives() == 1 && !data.areBootsBroken() && !data.getBootsData().isLowLifeWarningShown()) {
            MessageUtils.sendMessage(player, this.plugin.getConfigManager().getMessage("boots-warning"));
            data.getBootsData().setLowLifeWarningShown(true);
            this.plugin.getPlayerManager().savePlayerData(data);
        }
    }

    private void breakBoots(final Player player) {
        final PlayerData data = this.plugin.getPlayerManager().getPlayerData(player);
        data.breakBoots();
        
        // Remove boots item with animation
        player.getInventory().setBoots(null);
        
        // Play breaking animation
        this.playBreakingAnimation(player);
        
        // Send breaking message
        MessageUtils.sendMessage(player, this.plugin.getConfigManager().getMessage("boots-broken"));
        
        this.plugin.getPlayerManager().savePlayerData(data);
    }

    private void playBreakingAnimation(final Player player) {
        if (!this.plugin.getConfigManager().isBreakingAnimationEnabled()) {
            return;
        }
        
        final org.bukkit.Location location = player.getLocation().add(0, 1, 0);
        
        // Red particle burst
        player.getWorld().spawnParticle(org.bukkit.Particle.DUST, location, 20, 0.5, 0.5, 0.5, 0.1,
            new org.bukkit.Particle.DustOptions(org.bukkit.Color.RED, 1.0f));
        
        // Totem particles
        player.getWorld().spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, location, 10, 0.3, 0.3, 0.3, 0.1);
        
        // Sound effect
        player.playSound(location, org.bukkit.Sound.ITEM_SHIELD_BREAK, 1.0f, 0.8f);
    }

    public void updateBootsEnchantments(final Player player, final int lives) {
        final PlayerData data = this.plugin.getPlayerManager().getPlayerData(player);
        final ItemStack boots = player.getInventory().getBoots();
        
        if (boots == null || boots.getType() != Material.DIAMOND_BOOTS) {
            return;
        }
        
        final ItemMeta meta = boots.getItemMeta();
        if (meta == null) {
            return;
        }
        
        // Remove all enchantments first
        for (final Enchantment enchant : meta.getEnchants().keySet()) {
            meta.removeEnchant(enchant);
        }
        
        this.addBootsEnchantments(meta, data.getBootsData(), lives);
        
        boots.setItemMeta(meta);
    }

    public boolean hasValidBoots(final Player player) {
        final ItemStack boots = player.getInventory().getBoots();
        return boots != null && boots.getType() == Material.DIAMOND_BOOTS && 
               ItemUtils.hasCustomLore(boots, "Tier:");
    }

    public boolean areBootsBroken(final Player player) {
        final PlayerData data = this.plugin.getPlayerManager().getPlayerData(player);
        return data.areBootsBroken();
    }

    public void repairBoots(final Player player) {
        final PlayerData data = this.plugin.getPlayerManager().getPlayerData(player);
        data.repairBoots(this.plugin.getConfigManager().getDefaultLives());
        
        // Update boots item
        final ItemStack newBoots = this.createBootsItem(player, data.getBootsData());
        player.getInventory().setBoots(newBoots);
        
        MessageUtils.sendMessage(player, this.plugin.getConfigManager().getMessage("boots-repaired"));
        this.plugin.getPlayerManager().savePlayerData(data);
    }

    public void upgradeTier(final Player player) {
        final PlayerData data = this.plugin.getPlayerManager().getPlayerData(player);
        final BootsData bootsData = data.getBootsData();
        
        if (bootsData.getTier() == BootsTier.TIER_1) {
            bootsData.setTier(BootsTier.TIER_2);
            
            // Update boots item
            final ItemStack newBoots = this.createBootsItem(player, bootsData);
            player.getInventory().setBoots(newBoots);
            
            MessageUtils.sendMessage(player, this.plugin.getConfigManager().getMessage("tier-upgraded")
                .replace("{tier}", "II"));
            
            this.plugin.getPlayerManager().savePlayerData(data);
        }
    }

    public void downgradeTier(final Player player) {
        final PlayerData data = this.plugin.getPlayerManager().getPlayerData(player);
        final BootsData bootsData = data.getBootsData();
        
        if (bootsData.getTier() == BootsTier.TIER_2) {
            bootsData.setTier(BootsTier.TIER_1);
            
            // Update boots item
            final ItemStack newBoots = this.createBootsItem(player, bootsData);
            player.getInventory().setBoots(newBoots);
            
            MessageUtils.sendMessage(player, this.plugin.getConfigManager().getMessage("tier-downgraded")
                .replace("{tier}", "I"));
            
            this.plugin.getPlayerManager().savePlayerData(data);
        }
    }

    public void preventBootsRemoval(final Player player) {
        if (!this.hasValidBoots(player)) {
            final PlayerData data = this.plugin.getPlayerManager().getPlayerData(player);
            if (data.getBootsData() != null && !data.areBootsBroken()) {
                final ItemStack boots = this.createBootsItem(player, data.getBootsData());
                player.getInventory().setBoots(boots);
            }
        }
        
        // Prevent boots from being moved to other slots
        final ItemStack boots = player.getInventory().getBoots();
        if (boots != null && ItemUtils.isValidBootsItem(boots)) {
            // Check if boots are in wrong slot and move them back
            for (int i = 0; i < 36; i++) {
                final ItemStack item = player.getInventory().getItem(i);
                if (item != null && ItemUtils.isValidBootsItem(item)) {
                    player.getInventory().setItem(i, null);
                    player.getInventory().setBoots(item);
                    break;
                }
            }
        }
    }
}
