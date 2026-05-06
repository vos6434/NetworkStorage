package com.dermoha.networkstorage.listeners;

import com.dermoha.networkstorage.NetworkStoragePlugin;
import com.dermoha.networkstorage.gui.NetworkSelectGUI;
import com.dermoha.networkstorage.gui.StatsGUI;
import com.dermoha.networkstorage.gui.TerminalGUI;
import com.dermoha.networkstorage.gui.WirelessNetworkSelectGUI;
import com.dermoha.networkstorage.managers.LanguageManager;
import com.dermoha.networkstorage.storage.Network;
import com.dermoha.networkstorage.util.ItemUtils;
import com.dermoha.networkstorage.util.MessageUtils;
import com.dermoha.networkstorage.util.TerminalBlockUtils;
import com.dermoha.networkstorage.util.TerminalItemUtils;
import org.bukkit.Location;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ChestInteractListener implements Listener {

    private final NetworkStoragePlugin plugin;
    private final Map<UUID, TerminalGUI> openTerminals;
    private final Set<UUID> transitioningToStats = new HashSet<>();
    private final Set<UUID> transitioningToSearch = new HashSet<>();
    private final LanguageManager lang;

    public ChestInteractListener(NetworkStoragePlugin plugin) {
        this.plugin = plugin;
        this.openTerminals = new HashMap<>();
        this.lang = plugin.getLanguageManager();
    }

    public void addOpenTerminal(UUID playerId, TerminalGUI gui) {
        openTerminals.put(playerId, gui);
    }

    public void setTransitioningToStats(UUID playerId) {
        transitioningToStats.add(playerId);
    }

    public void setTransitioningToSearch(UUID playerId) {
        transitioningToSearch.add(playerId);
    }

    public void clearRuntimeState() {
        openTerminals.clear();
        transitioningToStats.clear();
        transitioningToSearch.clear();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();

        if (clickedBlock == null) {
            return;
        }

        ItemStack itemInHand = event.getItem();
        if (WandListener.isStorageWand(itemInHand, plugin)) {
            return;
        }

        if (plugin.getConfigManager().isNetworkContainerBlock(clickedBlock.getType())) {
            Network network = plugin.getNetworkManager().getNetworkByLocation(clickedBlock.getLocation());
            Location normalizedLoc = network != null ? network.getNormalizedLocation(clickedBlock.getLocation()) : null;

            if (network != null && (network.isTerminalInNetwork(clickedBlock.getLocation()) || network.isTerminalInNetwork(normalizedLoc))) {
                event.setCancelled(true);

                if (!network.canAccess(player)) {
                    MessageUtils.sendActionBar(player, lang.getMessage("trust.no_permission_access"));
                    return;
                }

                if (player.isSneaking() && itemInHand != null && itemInHand.getType() != Material.AIR) {
                    handleQuickDeposit(player, network, itemInHand, event.getHand());
                    return;
                }

                TerminalGUI gui = new TerminalGUI(player, network, plugin);
                addOpenTerminal(player.getUniqueId(), gui);
                player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1.0f, 1.0f);
                gui.open();

                MessageUtils.sendActionBar(player, lang.getMessage("network.access"));
            }
        }
    }

    @EventHandler
    public void onTerminalBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (!TerminalItemUtils.isTerminalItem(item, plugin)) {
            return;
        }

        Player player = event.getPlayer();
        if (!plugin.getConfigManager().hasPermission(player, "networkstorage.terminal")) {
            MessageUtils.sendActionBar(player, lang.getMessage("no_permission_terminal"));
            event.setCancelled(true);
            return;
        }

        Network network = plugin.getNetworkManager().getOrCreatePlayerNetwork(player);
        if (network == null) {
            MessageUtils.sendActionBar(player, lang.getMessage("network.error.create"));
            event.setCancelled(true);
            return;
        }

        Block placedBlock = event.getBlockPlaced();
        Location location = placedBlock.getLocation();
        if (plugin.getNetworkManager().getNetworkByLocation(location) != null) {
            MessageUtils.sendActionBar(player, lang.getMessage("wand.chest.already_assigned"));
            event.setCancelled(true);
            return;
        }

        if (network.getTerminalLocations().size() >= plugin.getConfigManager().getMaxTerminalsPerNetwork()) {
            MessageUtils.sendActionBar(player, String.format(lang.getMessage("wand.terminal.limit_reached"), plugin.getConfigManager().getMaxTerminalsPerNetwork()));
            event.setCancelled(true);
            return;
        }

        network.addTerminal(location);
        TerminalBlockUtils.applyTerminalShelfState(placedBlock);
        plugin.getNetworkManager().addToLocationIndex(location, network);
        MessageUtils.sendActionBar(player, String.format(lang.getMessage("terminal_block.linked"), network.getTerminalLocations().size()));
    }

    private void handleQuickDeposit(Player player, Network network, ItemStack itemInHand, EquipmentSlot hand) {
        int originalAmount = itemInHand.getAmount();
        ItemStack remaining = network.addToNetwork(itemInHand.clone());

        if (remaining == null || remaining.getAmount() == 0) {
            setItemInHand(player, hand, null);
            MessageUtils.sendActionBar(player, String.format(lang.getMessage("network.deposit.success"), originalAmount, ItemUtils.getItemDisplayName(itemInHand)));
            network.recordItemsDeposited(player, originalAmount);
        } else {
            int depositedAmount = originalAmount - remaining.getAmount();
            if (depositedAmount > 0) {
                MessageUtils.sendActionBar(player, String.format(lang.getMessage("network.deposit.partial"), depositedAmount, ItemUtils.getItemDisplayName(itemInHand), remaining.getAmount()));
                network.recordItemsDeposited(player, depositedAmount);
            }
            setItemInHand(player, hand, remaining);
        }
    }

    private void setItemInHand(Player player, EquipmentSlot hand, ItemStack item) {
        if (hand == EquipmentSlot.OFF_HAND) {
            player.getInventory().setItemInOffHand(item);
            return;
        }
        player.getInventory().setItemInMainHand(item);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof NetworkSelectGUI selectGUI) {
            event.setCancelled(true);
            selectGUI.handleClick(event.getSlot());
            return;
        }

        if (holder instanceof WirelessNetworkSelectGUI selectGUI) {
            event.setCancelled(true);
            selectGUI.handleClick(event.getSlot());
            return;
        }

        if (holder instanceof StatsGUI statsGUI) {
            event.setCancelled(true);
            statsGUI.handleClick(event.getSlot());
            return;
        }

        if (holder instanceof TerminalGUI terminal) {
            if (!terminal.equals(openTerminals.get(player.getUniqueId()))) {
                return;
            }

            event.setCancelled(true);

            int rawSlot = event.getRawSlot();
            int slot = event.getSlot();
            boolean isRightClick = event.isRightClick();
            boolean isLeftClick = event.isLeftClick();
            boolean isShiftClick = event.isShiftClick();

            if (rawSlot >= terminal.getInventory().getSize()) {
                if (isRightClick && isShiftClick) {
                    handleInventoryClickDepositAllSimilar(event, terminal, player);
                } else if (isRightClick) {
                    handleInventoryClickDeposit(event, terminal, player, getHalfStackSize(event.getCurrentItem()));
                } else if (isShiftClick) {
                    handleInventoryClickDeposit(event, terminal, player, Integer.MAX_VALUE);
                } else if (isLeftClick) {
                    handleInventoryClickDeposit(event, terminal, player, 1);
                }
                return;
            }

            if (rawSlot < terminal.getInventory().getSize()) {
                terminal.handleClick(slot, isRightClick, isShiftClick, isLeftClick);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof NetworkSelectGUI
                || holder instanceof WirelessNetworkSelectGUI
                || holder instanceof StatsGUI
                || holder instanceof TerminalGUI)) {
            return;
        }

        int topSize = event.getView().getTopInventory().getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private void handleInventoryClickDeposit(InventoryClickEvent event, TerminalGUI terminal, Player player, int requestedAmount) {
        if (!terminal.getNetwork().canAccess(player)) {
            player.closeInventory();
            MessageUtils.sendActionBar(player, lang.getMessage("trust.no_permission_access"));
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        int playerSlot = event.getSlot();
        ItemStack itemToDeposit = player.getInventory().getItem(playerSlot);
        if (itemToDeposit == null || itemToDeposit.getType() == Material.AIR) {
            return;
        }
        if (!canDepositItem(player, itemToDeposit, terminal)) {
            return;
        }

        int originalAmount = itemToDeposit.getAmount();
        int amountToDeposit = Math.min(originalAmount, requestedAmount);
        ItemStack depositStack = itemToDeposit.clone();
        depositStack.setAmount(amountToDeposit);
        ItemStack remaining = terminal.getNetwork().addToNetwork(depositStack);
        int remainingAmount = remaining == null ? 0 : remaining.getAmount();
        int depositedAmount = amountToDeposit - remainingAmount;

        if (depositedAmount <= 0) {
            return;
        }

        ItemStack updatedStack = itemToDeposit.clone();
        updatedStack.setAmount(originalAmount - depositedAmount);
        player.getInventory().setItem(playerSlot, updatedStack.getAmount() <= 0 ? null : updatedStack);
        terminal.getNetwork().recordItemsDeposited(player, depositedAmount);

        if (remainingAmount == 0) {
            MessageUtils.sendActionBar(player, String.format(lang.getMessage("network.deposit.success"), depositedAmount, terminal.getItemDisplayName(itemToDeposit)));
        } else {
            MessageUtils.sendActionBar(player, String.format(lang.getMessage("network.deposit.partial"), depositedAmount, terminal.getItemDisplayName(itemToDeposit), remainingAmount));
        }

        plugin.getServer().getScheduler().runTask(plugin, terminal::updateInventory);
    }

    private void handleInventoryClickDepositAllSimilar(InventoryClickEvent event, TerminalGUI terminal, Player player) {
        if (!terminal.getNetwork().canAccess(player)) {
            player.closeInventory();
            MessageUtils.sendActionBar(player, lang.getMessage("trust.no_permission_access"));
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        if (!canDepositItem(player, clickedItem, terminal)) {
            return;
        }

        int totalMatchingAmount = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && item.isSimilar(clickedItem)) {
                totalMatchingAmount += item.getAmount();
            }
        }

        int totalDeposited = 0;
        for (int slot = 0; slot < player.getInventory().getStorageContents().length; slot++) {
            ItemStack itemToDeposit = player.getInventory().getItem(slot);
            if (itemToDeposit == null || !itemToDeposit.isSimilar(clickedItem)) {
                continue;
            }

            int originalAmount = itemToDeposit.getAmount();
            ItemStack remaining = terminal.getNetwork().addToNetwork(itemToDeposit.clone());
            int remainingAmount = remaining == null ? 0 : remaining.getAmount();
            int depositedAmount = originalAmount - remainingAmount;

            if (depositedAmount <= 0) {
                break;
            }

            totalDeposited += depositedAmount;
            if (remainingAmount == 0) {
                player.getInventory().setItem(slot, null);
            } else {
                itemToDeposit.setAmount(remainingAmount);
                player.getInventory().setItem(slot, itemToDeposit);
                break;
            }
        }

        if (totalDeposited <= 0) {
            return;
        }

        terminal.getNetwork().recordItemsDeposited(player, totalDeposited);
        int totalRemaining = totalMatchingAmount - totalDeposited;
        if (totalRemaining <= 0) {
            MessageUtils.sendActionBar(player, String.format(lang.getMessage("network.deposit.success"), totalDeposited, terminal.getItemDisplayName(clickedItem)));
        } else {
            MessageUtils.sendActionBar(player, String.format(lang.getMessage("network.deposit.partial"), totalDeposited, totalRemaining));
        }

        plugin.getServer().getScheduler().runTask(plugin, terminal::updateInventory);
    }

    private int getHalfStackSize(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return 0;
        }

        return Math.max(1, item.getMaxStackSize() / 2);
    }

    private boolean canDepositItem(Player player, ItemStack item, TerminalGUI terminal) {
        if (terminal.isWirelessAccess() && WirelessTerminalListener.isWirelessTerminal(item, plugin)) {
            MessageUtils.sendActionBar(player, lang.getMessage("terminal.cannot_store_wireless"));
            return false;
        }

        return true;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof StatsGUI) {
            return;
        }

        if (holder instanceof TerminalGUI) {
            if (transitioningToSearch.remove(player.getUniqueId())) {
                openTerminals.remove(player.getUniqueId());
                return;
            }
            if (transitioningToStats.remove(player.getUniqueId())) {
                if (plugin.getSearchManager().isSearching(player)) {
                    plugin.getSearchManager().cancelSearch(player);
                }
                openTerminals.remove(player.getUniqueId());
                return;
            }
            if (plugin.getSearchManager().isSearching(player)) {
                plugin.getSearchManager().cancelSearch(player);
            }
            openTerminals.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (plugin.getConfigManager().isNetworkContainerBlock(block.getType())) {
            Location chestLoc = block.getLocation();
            Network network = plugin.getNetworkManager().getNetworkByLocation(chestLoc);

            if (network != null) {
                Player breaker = event.getPlayer();
                boolean isOwner = network.getOwner().equals(breaker.getUniqueId());
                boolean isAdmin = plugin.getConfigManager().hasPrivilege(breaker, "networkstorage.admin");

                if (!isOwner && !isAdmin) {
                    event.setCancelled(true);
                    return;
                }

                Location normalizedLoc = network.getNormalizedLocation(chestLoc);
                boolean isTerminal = network.isTerminalInNetwork(chestLoc) || network.isTerminalInNetwork(normalizedLoc);
                boolean changed = removeTrackedLocation(network, chestLoc);
                if (!normalizedLoc.equals(chestLoc)) {
                    changed = removeTrackedLocation(network, normalizedLoc) || changed;
                }

                if (isTerminal && breaker.getGameMode() != GameMode.CREATIVE) {
                    event.setDropItems(false);
                    block.getWorld().dropItemNaturally(block.getLocation(), TerminalItemUtils.createTerminalItem(plugin));
                }

                if (changed) {
                    plugin.getNetworkManager().saveNetworks();
                }
            }
        }
    }

    private boolean removeTrackedLocation(Network network, Location location) {
        boolean changed = false;

        if (network.isChestInNetwork(location)) {
            network.removeChest(location);
            changed = true;
        }
        if (network.isTerminalInNetwork(location)) {
            network.removeTerminal(location);
            changed = true;
        }
        if (network.isSenderChestInNetwork(location)) {
            network.removeSenderChest(location);
            changed = true;
        }

        if (changed) {
            plugin.getNetworkManager().removeFromLocationIndex(location);
        }

        return changed;
    }
}
