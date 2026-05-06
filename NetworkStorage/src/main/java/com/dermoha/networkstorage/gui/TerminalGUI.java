package com.dermoha.networkstorage.gui;

import com.dermoha.networkstorage.NetworkStoragePlugin;
import com.dermoha.networkstorage.managers.LanguageManager;
import com.dermoha.networkstorage.storage.Network;
import com.dermoha.networkstorage.util.ItemUtils;
import com.dermoha.networkstorage.util.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.Component;

import java.util.*;
import java.util.stream.Collectors;

public class TerminalGUI implements InventoryHolder {

    private final Player player;
    private final Network network;
    private final NetworkStoragePlugin plugin;
    private final LanguageManager lang;
    private final boolean wirelessAccess;

    private Inventory inventory;
    private int currentPage = 0;
    private SortType sortType = SortType.ALPHABETICAL;
    private List<Map.Entry<ItemStack, Integer>> sortedItems;
    private String searchFilter = "";

    private static final int ITEMS_PER_PAGE = 45;
    private static final int GUI_SIZE = 54;

    private static final int SLOT_PREV_PAGE = 45;
    private static final int SLOT_PAGE_INDICATOR_START = 46;
    private static final int SLOT_PAGE_INDICATOR_END = 47;
    private static final int SLOT_NEXT_PAGE = 48;
    private static final int SLOT_SEARCH = 49;
    private static final int SLOT_SORT = 50;
    private static final int SLOT_REFRESH = 51;
    private static final int SLOT_INFO = 52;
    private static final int SLOT_STATS = 53;
    private static final String CUSTOM_GUI_BACKGROUND = "\uE000\uE100";
    private static final String CUSTOM_GUI_TITLE_OFFSET = "\uE001\uE002";
    private static final String CUSTOM_GUI_PAGE_OFFSET = "\uE003";
    private static final char PAGE_DIGIT_BASE = '\uE110';
    private static final char PAGE_SLASH = '\uE11A';
    private static final Key TERMINAL_FONT = Key.key("networkstorage", "terminal");
    private static final Key DEFAULT_FONT = Key.key("minecraft", "default");
    private static final TextColor VANILLA_INVENTORY_TITLE_COLOR = TextColor.color(0x404040);

    public enum SortType {
        ALPHABETICAL,
        COUNT_DESC,
        COUNT_ASC
    }

    public TerminalGUI(Player player, Network network, NetworkStoragePlugin plugin) {
        this(player, network, plugin, false);
    }

    public TerminalGUI(Player player, Network network, NetworkStoragePlugin plugin, boolean wirelessAccess) {
        this.player = player;
        this.network = network;
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
        this.wirelessAccess = wirelessAccess;
        this.sortType = parseSortType(plugin.getNetworkManager().getTerminalSortMode(player));
        this.searchFilter = plugin.getNetworkManager().getTerminalSearchFilter(player);
        updateInventory();
    }

    private SortType parseSortType(String storedSortType) {
        if (storedSortType == null || storedSortType.isBlank()) {
            return SortType.ALPHABETICAL;
        }

        try {
            return SortType.valueOf(storedSortType);
        } catch (IllegalArgumentException e) {
            return SortType.ALPHABETICAL;
        }
    }

    private Inventory createInventory(int totalPages) {
        if (plugin.getConfigManager().isCustomTerminalGuiEnabled()) {
            String pageText = getPageIndicatorText(totalPages);
            Component title = Component.text(CUSTOM_GUI_BACKGROUND)
                    .font(TERMINAL_FONT)
                    .color(NamedTextColor.WHITE)
                    .append(Component.text(CUSTOM_GUI_TITLE_OFFSET).font(TERMINAL_FONT))
                    .append(Component.text("Storage Network Terminal").font(DEFAULT_FONT).color(VANILLA_INVENTORY_TITLE_COLOR))
                    .append(Component.text(CUSTOM_GUI_PAGE_OFFSET).font(TERMINAL_FONT).color(VANILLA_INVENTORY_TITLE_COLOR))
                    .append(Component.text(toPageGlyphs(pageText)).font(TERMINAL_FONT).color(NamedTextColor.WHITE));
            return Bukkit.createInventory(this, GUI_SIZE, title);
        }
        return Bukkit.createInventory(this, GUI_SIZE, lang.getMessage("terminal.title"));
    }

    private String getPageIndicatorText(int totalPages) {
        return (currentPage + 1) + "/" + Math.max(1, totalPages);
    }

    private String toPageGlyphs(String text) {
        StringBuilder glyphs = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            if (c >= '0' && c <= '9') {
                glyphs.append((char) (PAGE_DIGIT_BASE + (c - '0')));
            } else if (c == '/') {
                glyphs.append(PAGE_SLASH);
            }
        }
        return glyphs.toString();
    }

    public void updateInventory() {
        Map<ItemStack, Integer> networkItems = network.getNetworkItems();
        long totalNetworkItems = networkItems.values().stream().mapToLong(Integer::longValue).sum();
        int uniqueTypes = networkItems.size();
        double capacity = network.getCapacityPercent();
        sortedItems = new ArrayList<>(networkItems.entrySet());

        if (!searchFilter.isEmpty()) {
            sortedItems = sortedItems.stream()
                    .filter(entry -> {
                        ItemStack item = entry.getKey();
                        String lowerCaseFilter = searchFilter.toLowerCase();

                        // Check custom display name
                        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                            if (item.getItemMeta().getDisplayName().toLowerCase().contains(lowerCaseFilter)) {
                                return true;
                            }
                        }

                        // Check internal material name (e.g., "diamond_sword")
                        if (item.getType().getKey().getKey().toLowerCase().contains(lowerCaseFilter)) {
                            return true;
                        }

                        // Fallback check for formatted English name
                        return getItemDisplayName(item).toLowerCase().contains(lowerCaseFilter);
                    })
                    .collect(Collectors.toList());
        }

        switch (sortType) {
            case ALPHABETICAL:
                sortedItems.sort(Comparator.comparing(a -> getItemDisplayName(a.getKey()), String.CASE_INSENSITIVE_ORDER));
                break;
            case COUNT_DESC:
                sortedItems.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
                break;
            case COUNT_ASC:
                sortedItems.sort(Map.Entry.comparingByValue());
                break;
        }

        int totalPages = (int) Math.ceil((double) sortedItems.size() / ITEMS_PER_PAGE);
        if (currentPage >= Math.max(1, totalPages)) {
            currentPage = Math.max(0, totalPages - 1);
        }

        Inventory previousInventory = inventory;
        inventory = createInventory(totalPages);

        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, sortedItems.size());

        for (int i = startIndex; i < endIndex; i++) {
            int slot = i - startIndex;
            Map.Entry<ItemStack, Integer> entry = sortedItems.get(i);
            ItemStack displayItem = createDisplayItem(entry.getKey(), entry.getValue(), totalNetworkItems);
            inventory.setItem(slot, displayItem);
        }

        addControlButtons(currentPage, totalPages, totalNetworkItems, uniqueTypes, capacity);
        reopenIfPlayerIsViewing(previousInventory);
    }

    private void reopenIfPlayerIsViewing(Inventory previousInventory) {
        if (previousInventory == null || player.getOpenInventory().getTopInventory().getHolder() != this) {
            return;
        }

        player.openInventory(inventory);
        plugin.getChestInteractListener().addOpenTerminal(player.getUniqueId(), this);
    }

    private void addControlButtons(int page, int totalPages, long totalItems, int uniqueTypes, double capacity) {
        int displayTotalPages = Math.max(1, totalPages);

        ItemStack prevButton = createGuiControlItem(
                Material.ARROW,
                lang.getMessage("terminal.prev_page"),
                Collections.singletonList(String.format(lang.getMessage("terminal.page"), page + 1, displayTotalPages)),
                "custom-model-data.gui.terminal.prev-page"
        );
        inventory.setItem(SLOT_PREV_PAGE, prevButton);

        ItemStack pageIndicator = createGuiControlItem(
                Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                String.format("§f%s/%s", page + 1, displayTotalPages),
                Collections.singletonList(String.format(lang.getMessage("terminal.page"), page + 1, displayTotalPages)),
                "custom-model-data.gui.terminal.page-indicator"
        );
        inventory.setItem(SLOT_PAGE_INDICATOR_START, pageIndicator);
        inventory.setItem(SLOT_PAGE_INDICATOR_END, pageIndicator.clone());

        ItemStack nextButton = createGuiControlItem(
                Material.ARROW,
                lang.getMessage("terminal.next_page"),
                Collections.singletonList(String.format(lang.getMessage("terminal.page"), Math.min(page + 2, displayTotalPages), displayTotalPages)),
                "custom-model-data.gui.terminal.next-page"
        );
        inventory.setItem(SLOT_NEXT_PAGE, nextButton);

        String searchTitle;
        List<String> searchLore;
        if (searchFilter.isEmpty()) {
            searchTitle = lang.getMessage("terminal.search.title");
            searchLore = Arrays.asList(
                    lang.getMessage("terminal.search.lore1"),
                    lang.getMessage("terminal.search.lore2")
            );
        } else {
            searchTitle = String.format(lang.getMessage("terminal.search.active"), searchFilter);
            searchLore = Arrays.asList(
                    lang.getMessage("terminal.search.filtered"),
                    lang.getMessage("terminal.search.change"),
                    lang.getMessage("terminal.search.clear")
            );
        }
        ItemStack searchButton = createGuiControlItem(Material.SPYGLASS, searchTitle, searchLore, "custom-model-data.gui.terminal.search");
        inventory.setItem(SLOT_SEARCH, searchButton);

        ItemStack sortButton = createGuiControlItem(
                Material.COMPARATOR,
                String.format(lang.getMessage("terminal.sort.title"), getSortDisplayName()),
                Arrays.asList(
                lang.getMessage("terminal.sort.lore1"),
                String.format(lang.getMessage("terminal.sort.lore2"), getSortDisplayName())
                ),
                "custom-model-data.gui.terminal.sort"
        );
        inventory.setItem(SLOT_SORT, sortButton);

        ItemStack infoButton = createGuiControlItem(
                Material.BOOK,
                lang.getMessage("terminal.info.title"),
                Arrays.asList(
                String.format(lang.getMessage("terminal.info.items"), uniqueTypes),
                String.format(lang.getMessage("total_items"), formatNumber(totalItems)),
                String.format(lang.getMessage("terminal.info.chests"), network.getChestLocations().size()),
                String.format(lang.getMessage("terminal.info.terminals"), network.getTerminalLocations().size()),
                String.format(lang.getMessage("terminal.info.capacity"), String.format("%.1f%%", capacity)),
                "",
                lang.getMessage("terminal.info.lore1"),
                lang.getMessage("terminal.info.lore2"),
                lang.getMessage("terminal.info.lore3")
                ),
                "custom-model-data.gui.terminal.info"
        );
        inventory.setItem(SLOT_INFO, infoButton);

        ItemStack statsButton = createGuiControlItem(
                Material.EMERALD,
                lang.getMessage("terminal.stats.title"),
                Collections.singletonList(lang.getMessage("terminal.stats.lore")),
                "custom-model-data.gui.terminal.stats"
        );
        inventory.setItem(SLOT_STATS, statsButton);

        ItemStack refreshButton = createGuiControlItem(
                Material.CLOCK,
                lang.getMessage("terminal.refresh.title"),
                Collections.singletonList(lang.getMessage("terminal.refresh.lore")),
                "custom-model-data.gui.terminal.refresh"
        );
        inventory.setItem(SLOT_REFRESH, refreshButton);
    }

    private ItemStack createGuiControlItem(Material material, String displayName, List<String> lore, String customModelDataPath) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(lore);
            if (customModelDataPath != null) {
                ItemUtils.applyCustomModelData(meta, plugin.getConfigManager().getOptionalCustomModelData(customModelDataPath));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createDisplayItem(ItemStack original, int totalCount, long totalNetworkItems) {
        ItemStack display = original.clone();
        display.setAmount(1);
        ItemMeta meta = display.getItemMeta();
        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(display.getType());
        }

        if (!original.hasItemMeta() || !original.getItemMeta().hasDisplayName()) {
            meta.setDisplayName(null);
        }

        List<String> lore = new ArrayList<>();
        lore.add(String.format(lang.getMessage("terminal.item.lore.total"), formatNumber(totalCount)));
        if (totalNetworkItems > 0) {
            double percentage = (double) totalCount / totalNetworkItems * 100.0;
            lore.add(String.format(lang.getMessage("terminal.item.lore.capacity_percentage"), percentage));
        }
        lore.add(String.format(lang.getMessage("terminal.item.lore.stacks"), totalCount / original.getMaxStackSize()));
        if (totalCount % original.getMaxStackSize() > 0) {
            lore.add(String.format(lang.getMessage("terminal.item.lore.partial"), totalCount % original.getMaxStackSize()));
        }
        lore.add("");
        lore.add(String.format(lang.getMessage("terminal.item.lore.take_stack"), original.getMaxStackSize()));
        lore.add(lang.getMessage("terminal.item.lore.take_all"));
        lore.add(lang.getMessage("terminal.item.lore.take_half"));
        lore.add(lang.getMessage("terminal.item.lore.take_one"));
        if (original.hasItemMeta() && original.getItemMeta().hasLore()) {
            lore.add("");
            lore.add(lang.getMessage("terminal.item.lore.properties"));
            if (original.getItemMeta().getLore() != null) {
                lore.addAll(original.getItemMeta().getLore());
            }
        }
        meta.setLore(lore);
        display.setItemMeta(meta);
        return display;
    }

    public static String getItemDisplayName(ItemStack item) {
        return ItemUtils.getItemDisplayName(item);
    }

    private String getSortDisplayName() {
        switch (sortType) {
            case ALPHABETICAL:
                return lang.getMessage("terminal.sort.alpha");
            case COUNT_DESC:
                return lang.getMessage("terminal.sort.desc");
            case COUNT_ASC:
                return lang.getMessage("terminal.sort.asc");
            default:
                return lang.getMessage("terminal.sort.unknown");
        }
    }

    public String formatNumber(long number) {
        if (number >= 1000000) {
            return String.format("%.1fM", number / 1000000.0);
        } else if (number >= 1000) {
            return String.format("%.1fK", number / 1000.0);
        } else {
            return String.valueOf(number);
        }
    }

    public void handleClick(int slot, boolean isRightClick, boolean isShiftClick, boolean isLeftClick) {
        if (!ensureAccess()) {
            return;
        }

        if (slot == SLOT_PREV_PAGE && currentPage > 0) {
            currentPage--;
            updateInventory();
            return;
        }

        if (slot == SLOT_PAGE_INDICATOR_START || slot == SLOT_PAGE_INDICATOR_END) {
            return;
        }

        if (slot == SLOT_NEXT_PAGE) {
            int totalPages = (int) Math.ceil((double) sortedItems.size() / ITEMS_PER_PAGE);
            if (currentPage < totalPages - 1) {
                currentPage++;
                updateInventory();
            }
            return;
        }

        if (slot == SLOT_SEARCH) {
            if (isRightClick && !searchFilter.isEmpty()) {
                searchFilter = "";
                plugin.getNetworkManager().setTerminalSearchFilter(player, searchFilter);
                currentPage = 0;
                updateInventory();
                player.sendMessage(lang.getMessage("terminal.search.cleared"));
            } else {
                plugin.getSearchManager().startSearch(player, this);
                plugin.getChestInteractListener().setTransitioningToSearch(player.getUniqueId());
                player.closeInventory();
                player.sendMessage(lang.getMessage("terminal.search.prompt"));
                player.sendMessage(lang.getMessage("terminal.search.cancel_hint"));
            }
            return;
        }

        if (slot == SLOT_SORT) {
            cycleSortType();
            currentPage = 0;
            updateInventory();
            return;
        }

        if (slot == SLOT_STATS) {
            plugin.getChestInteractListener().setTransitioningToStats(player.getUniqueId());
            StatsGUI statsGUI = new StatsGUI(player, network, plugin, this);
            statsGUI.open();
            return;
        }

        if (slot == SLOT_REFRESH) {
            updateInventory();
            MessageUtils.sendActionBar(player, lang.getMessage("terminal.refreshed"));
            return;
        }

        if (slot >= 0 && slot < ITEMS_PER_PAGE) {
            int itemIndex = (currentPage * ITEMS_PER_PAGE) + slot;
            if (itemIndex < sortedItems.size()) {
                Map.Entry<ItemStack, Integer> entry = sortedItems.get(itemIndex);
                ItemStack originalItem = entry.getKey();
                int availableAmount = entry.getValue();
                int amountToTake = 0;

                if (isLeftClick && !isShiftClick) { // Left-click: take 1
                    amountToTake = 1;
                } else if (isLeftClick && isShiftClick) { // Shift-left-click: take stack
                    amountToTake = originalItem.getMaxStackSize();
                } else if (isRightClick && isShiftClick) { // Shift-right-click: take as much as fits
                    amountToTake = Math.min(availableAmount, getInventorySpaceFor(originalItem));
                } else if (isRightClick) { // Right-click: take half stack
                    amountToTake = Math.max(1, originalItem.getMaxStackSize() / 2);
                }

                if (amountToTake > 0) {
                    handleItemExtraction(originalItem, availableAmount, amountToTake);
                }
            }
        }
    }

    private void handleItemExtraction(ItemStack itemType, int availableAmount, int amountToTake) {
        int requestedAmount = Math.min(availableAmount, amountToTake);

        if (requestedAmount <= 0) {
            MessageUtils.sendActionBar(player, lang.getMessage("terminal.no_items"));
            return;
        }

        ItemStack requestedItem = itemType.clone();
        requestedItem.setAmount(requestedAmount);
        ItemStack removedItem = network.removeFromNetwork(requestedItem, requestedAmount);

        if (removedItem == null || removedItem.getAmount() <= 0) {
            MessageUtils.sendActionBar(player, lang.getMessage("terminal.no_items"));
            plugin.getServer().getScheduler().runTask(plugin, this::updateInventory);
            return;
        }

        HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(removedItem.clone());
        int returnedToNetworkAmount = 0;
        boolean droppedItems = false;

        for (ItemStack overflowItem : overflow.values()) {
            int overflowAmount = overflowItem.getAmount();
            ItemStack remaining = network.addToNetwork(overflowItem.clone());

            if (remaining == null || remaining.getAmount() == 0) {
                returnedToNetworkAmount += overflowAmount;
                continue;
            }

            returnedToNetworkAmount += overflowAmount - remaining.getAmount();
            if (remaining.getAmount() > 0) {
                player.getWorld().dropItemNaturally(player.getLocation(), remaining);
                droppedItems = true;
            }
        }

        int withdrawnAmount = removedItem.getAmount() - returnedToNetworkAmount;
        if (withdrawnAmount <= 0) {
            MessageUtils.sendActionBar(player, lang.getMessage("terminal.inventory_full_returned"));
            plugin.getServer().getScheduler().runTask(plugin, this::updateInventory);
            return;
        }

        network.recordItemsWithdrawn(player, withdrawnAmount);
        MessageUtils.sendActionBar(player, String.format(lang.getMessage("terminal.took_items"), withdrawnAmount, getItemDisplayName(itemType)));
        if (!overflow.isEmpty() && !droppedItems) {
            MessageUtils.sendActionBar(player, lang.getMessage("terminal.inventory_full_returned"));
        }
        if (droppedItems) {
            MessageUtils.sendActionBar(player, lang.getMessage("terminal.items_dropped"));
        }

        plugin.getServer().getScheduler().runTask(plugin, this::updateInventory);
    }

    private int getInventorySpaceFor(ItemStack itemType) {
        int space = 0;
        int maxStackSize = itemType.getMaxStackSize();

        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null || item.getType() == Material.AIR) {
                space += maxStackSize;
                continue;
            }

            if (item.isSimilar(itemType)) {
                space += Math.max(0, maxStackSize - item.getAmount());
            }
        }

        return space;
    }

    private void cycleSortType() {
        sortType = SortType.values()[(sortType.ordinal() + 1) % SortType.values().length];
        plugin.getNetworkManager().setTerminalSortMode(player, sortType.name());
        MessageUtils.sendActionBar(player, String.format(lang.getMessage("terminal.sort_changed"), getSortDisplayName()));
    }

    public void setSearchFilter(String filter) {
        if (!ensureAccess()) {
            return;
        }

        this.searchFilter = filter == null ? "" : filter;
        plugin.getNetworkManager().setTerminalSearchFilter(player, searchFilter);
        this.currentPage = 0;
        updateInventory();
    }

    public boolean open() {
        if (!ensureAccess()) {
            return false;
        }

        player.openInventory(inventory);
        return true;
    }

    private boolean ensureAccess() {
        if (network.canAccess(player)) {
            return true;
        }

        player.closeInventory();
        MessageUtils.sendActionBar(player, lang.getMessage("trust.no_permission_access"));
        return false;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
    }

    public Network getNetwork() {
        return network;
    }

    public boolean isWirelessAccess() {
        return wirelessAccess;
    }
}
