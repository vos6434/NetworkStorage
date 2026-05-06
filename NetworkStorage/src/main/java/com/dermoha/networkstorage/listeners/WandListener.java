package com.dermoha.networkstorage.listeners;

import com.dermoha.networkstorage.NetworkStoragePlugin;
import com.dermoha.networkstorage.managers.LanguageManager;
import com.dermoha.networkstorage.storage.Network;
import com.dermoha.networkstorage.util.ItemUtils;
import com.dermoha.networkstorage.util.MessageUtils;
import com.dermoha.networkstorage.util.TerminalBlockUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;

public class WandListener implements Listener {

    private final NetworkStoragePlugin plugin;
    private final LanguageManager lang;

    public WandListener(NetworkStoragePlugin plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isStorageWand(item, plugin)) {
            return;
        }

        if (!plugin.getConfigManager().hasPermission(player, "networkstorage.wand")) {
            MessageUtils.sendFeedback(player, lang.getMessage("no_permission_wand"));
            event.setCancelled(true);
            return;
        }

        if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }

        event.setCancelled(true);

        if (plugin.getConfigManager().isNetworkContainerBlock(clickedBlock.getType())) {
            handleChestClick(player, clickedBlock, event.getAction());
        } else {
            MessageUtils.sendFeedback(player, lang.getMessage("wand.only_chest"));
        }
    }

    private void handleChestClick(Player player, Block chestBlock, Action action) {
        Network network = plugin.getNetworkManager().getOrCreatePlayerNetwork(player);
        if (network == null) {
            MessageUtils.sendFeedback(player, lang.getMessage("network.error.create"));
            return;
        }
        Location normalizedLoc = network.getNormalizedLocation(chestBlock.getLocation());
        boolean isTerminalShelf = TerminalBlockUtils.isTerminalShelf(chestBlock.getType());

        if (action == Action.LEFT_CLICK_BLOCK) {
            if (isTerminalShelf) {
                MessageUtils.sendFeedback(player, lang.getMessage("wand.only_chest"));
                return;
            }

            if (player.isSneaking()) {
                if (network.isSenderChestInNetwork(normalizedLoc)) {
                    MessageUtils.sendFeedback(player, lang.getMessage("wand.sender_chest.already"));
                    return;
                }

                if (!canAddNetworkRole(player, network, normalizedLoc)) {
                    return;
                }

                if (isOtherHalfInNetwork(chestBlock, network)) {
                    MessageUtils.sendFeedback(player, lang.getMessage("wand.sender_chest.other_half"));
                    MessageUtils.sendFeedback(player, lang.getMessage("wand.sender_chest.unit_hint"));
                    return;
                }

                if (network.getSenderChestLocations().size() >= plugin.getConfigManager().getMaxSenderChestsPerNetwork()) {
                    MessageUtils.sendFeedback(player, String.format(lang.getMessage("wand.sender_chest.limit_reached"), plugin.getConfigManager().getMaxSenderChestsPerNetwork()));
                    return;
                }

                network.addSenderChest(normalizedLoc);
                plugin.getNetworkManager().addToLocationIndex(normalizedLoc, network);

                String chestType = getChestType(chestBlock);
                MessageUtils.sendFeedback(player, String.format(lang.getMessage("wand.sender_chest.added"), chestType, network.getSenderChestLocations().size()));

            } else {
                if (network.isChestInNetwork(normalizedLoc)) {
                    MessageUtils.sendFeedback(player, lang.getMessage("wand.chest.already_in_network"));
                    return;
                }

                if (!canAddNetworkRole(player, network, normalizedLoc)) {
                    return;
                }

                if (isOtherHalfInNetwork(chestBlock, network)) {
                    MessageUtils.sendFeedback(player, lang.getMessage("wand.double_chest.other_half_in_network"));
                    MessageUtils.sendFeedback(player, lang.getMessage("wand.double_chest.unit_hint"));
                    return;
                }

                if (network.getChestLocations().size() >= plugin.getConfigManager().getMaxChestsPerNetwork()) {
                    MessageUtils.sendFeedback(player, String.format(lang.getMessage("wand.chest.limit_reached"), plugin.getConfigManager().getMaxChestsPerNetwork()));
                    return;
                }

                network.addChest(normalizedLoc);
                plugin.getNetworkManager().addToLocationIndex(normalizedLoc, network);

                String chestType = getChestType(chestBlock);
                MessageUtils.sendFeedback(player, String.format(lang.getMessage("wand.chest.added"), chestType, network.getChestLocations().size()));
            }
        } else if (action == Action.RIGHT_CLICK_BLOCK) {
            if (player.isSneaking()) {
                if (network.isTerminalInNetwork(normalizedLoc)) {
                    MessageUtils.sendFeedback(player, lang.getMessage("wand.terminal.already"));
                    return;
                }

                if (!canAddNetworkRole(player, network, normalizedLoc)) {
                    return;
                }

                if (isOtherHalfInNetwork(chestBlock, network)) {
                    MessageUtils.sendFeedback(player, lang.getMessage("wand.terminal.other_half"));
                    MessageUtils.sendFeedback(player, lang.getMessage("wand.terminal.unit_hint"));
                    return;
                }

                if (network.getTerminalLocations().size() >= plugin.getConfigManager().getMaxTerminalsPerNetwork()) {
                    MessageUtils.sendFeedback(player, String.format(lang.getMessage("wand.terminal.limit_reached"), plugin.getConfigManager().getMaxTerminalsPerNetwork()));
                    return;
                }

                network.addTerminal(normalizedLoc);
                TerminalBlockUtils.applyTerminalShelfState(chestBlock);
                plugin.getNetworkManager().addToLocationIndex(normalizedLoc, network);

                String chestType = getChestType(chestBlock);
                MessageUtils.sendFeedback(player, String.format(lang.getMessage("wand.terminal.added"), chestType, network.getTerminalLocations().size()));

            } else {
                boolean wasChest = network.isChestInNetwork(normalizedLoc);
                boolean wasTerminal = network.isTerminalInNetwork(normalizedLoc);
                boolean wasSenderChest = network.isSenderChestInNetwork(normalizedLoc);

                if (!wasChest && !wasTerminal && !wasSenderChest) {
                    MessageUtils.sendFeedback(player, lang.getMessage("wand.chest.not_in_network"));
                    return;
                }

                network.removeChest(normalizedLoc);
                network.removeTerminal(normalizedLoc);
                network.removeSenderChest(normalizedLoc);
                if (wasTerminal) {
                    TerminalBlockUtils.clearTerminalShelfState(chestBlock);
                }
                plugin.getNetworkManager().removeFromLocationIndex(normalizedLoc);

                String chestType = getChestType(chestBlock);

                if (wasChest) {
                    MessageUtils.sendFeedback(player, String.format(lang.getMessage("wand.chest.removed"), chestType, network.getChestLocations().size()));
                } else if (wasTerminal) {
                    MessageUtils.sendFeedback(player, String.format(lang.getMessage("wand.terminal.removed"), chestType, network.getTerminalLocations().size()));
                } else {
                    MessageUtils.sendFeedback(player, String.format(lang.getMessage("wand.sender_chest.removed"), chestType, network.getSenderChestLocations().size()));
                }
            }
        }
    }

    private boolean canAddNetworkRole(Player player, Network network, Location normalizedLoc) {
        Network existingNetwork = plugin.getNetworkManager().getNetworkByLocation(normalizedLoc);
        if (existingNetwork != null && existingNetwork != network) {
            MessageUtils.sendFeedback(player, lang.getMessage("wand.chest.in_other_network"));
            return false;
        }

        if (isAnyRoleInNetwork(network, normalizedLoc)) {
            MessageUtils.sendFeedback(player, lang.getMessage("wand.chest.already_assigned"));
            return false;
        }

        return true;
    }

    private boolean isAnyRoleInNetwork(Network network, Location location) {
        return network.isChestInNetwork(location)
                || network.isTerminalInNetwork(location)
                || network.isSenderChestInNetwork(location);
    }

    private boolean isOtherHalfInNetwork(Block chestBlock, Network network) {
        if (!(chestBlock.getState() instanceof Chest)) {
            return false;
        }
        Chest chest = (Chest) chestBlock.getState();
        Inventory inventory = chest.getInventory();

        if (inventory.getHolder() instanceof org.bukkit.block.DoubleChest) {
            org.bukkit.block.DoubleChest doubleChest = (org.bukkit.block.DoubleChest) inventory.getHolder();
            Chest left = (Chest) doubleChest.getLeftSide();
            Chest right = (Chest) doubleChest.getRightSide();

            Location otherHalf = left.getLocation().equals(chestBlock.getLocation()) ? right.getLocation() : left.getLocation();
            Location normalizedOtherHalf = network.getNormalizedLocation(otherHalf);

            return network.isChestInNetwork(normalizedOtherHalf) 
                    || network.isTerminalInNetwork(normalizedOtherHalf) 
                    || network.isSenderChestInNetwork(normalizedOtherHalf);
        }

        return false;
    }

    private String getChestType(Block chestBlock) {
        if (chestBlock.getState() instanceof Chest) {
            Chest chest = (Chest) chestBlock.getState();
            if (chest.getInventory().getSize() == 54) {
                return "double chest";
            }
        }
        String typeName = chestBlock.getType().name().toLowerCase().replace("_", " ");
        return typeName;
    }

    public static ItemStack createStorageWand(NetworkStoragePlugin plugin) {
        LanguageManager lang = plugin.getLanguageManager();
        ItemStack wand = new ItemStack(plugin.getConfigManager().getWandMaterial());
        ItemMeta meta = wand.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(lang.getMessage("wand.name"));
            meta.setLore(Arrays.asList(
                    lang.getMessage("wand.lore1"),
                    lang.getMessage("wand.lore2"),
                    lang.getMessage("wand.lore3"),
                    lang.getMessage("wand.lore4")
            ));
            ItemUtils.applyCustomModelData(meta, plugin.getConfigManager().getStorageWandCustomModelData());
            meta.setMaxStackSize(1);
            meta.getPersistentDataContainer().set(getStorageWandKey(plugin), PersistentDataType.BYTE, (byte) 1);
            wand.setItemMeta(meta);
        }

        return wand;
    }

    public static boolean isStorageWand(ItemStack item, NetworkStoragePlugin plugin) {
        if (item == null || item.getType() != plugin.getConfigManager().getWandMaterial()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(getStorageWandKey(plugin), PersistentDataType.BYTE);
    }

    private static NamespacedKey getStorageWandKey(NetworkStoragePlugin plugin) {
        return new NamespacedKey(plugin, "storage_wand");
    }
}
