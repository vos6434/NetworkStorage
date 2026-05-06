package com.dermoha.networkstorage.listeners;

import com.dermoha.networkstorage.NetworkStoragePlugin;
import com.dermoha.networkstorage.gui.TerminalGUI;
import com.dermoha.networkstorage.gui.WirelessNetworkSelectGUI;
import com.dermoha.networkstorage.managers.LanguageManager;
import com.dermoha.networkstorage.storage.Network;
import com.dermoha.networkstorage.util.ItemUtils;
import com.dermoha.networkstorage.util.MessageUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class WirelessTerminalListener implements Listener {

    private final NetworkStoragePlugin plugin;

    public WirelessTerminalListener(NetworkStoragePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        LanguageManager lang = plugin.getLanguageManager();

        if (!isWirelessTerminal(item, plugin)) {
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);

            if (!plugin.getConfigManager().hasPermission(player, "networkstorage.wireless")) {
                MessageUtils.sendFeedback(player, lang.getMessage("no_permission_wireless"));
                return;
            }

            List<Network> accessibleNetworks = plugin.getNetworkManager().getAccessibleNetworks(player);
            if (accessibleNetworks.isEmpty()) {
                MessageUtils.sendFeedback(player, lang.getMessage("no_network"));
                return;
            }

            Network rememberedNetwork = plugin.getNetworkManager().getSelectedWirelessNetwork(player);
            if (rememberedNetwork != null) {
                openSelectedNetwork(player, event.getHand(), rememberedNetwork.getName());
                return;
            }

            if (accessibleNetworks.size() == 1) {
                openSelectedNetwork(player, event.getHand(), accessibleNetworks.get(0).getName());
                return;
            }

            new WirelessNetworkSelectGUI(player, accessibleNetworks, plugin, event.getHand()).open();
        }
    }

    public void openSelectedNetwork(Player player, EquipmentSlot hand, String networkName) {
        LanguageManager lang = plugin.getLanguageManager();
        if (!plugin.getConfigManager().hasPermission(player, "networkstorage.wireless")) {
            MessageUtils.sendFeedback(player, lang.getMessage("no_permission_wireless"));
            return;
        }

        Network network = plugin.getNetworkManager().findAccessibleNetwork(player, networkName);
        if (network == null) {
            MessageUtils.sendFeedback(player, String.format(lang.getMessage("wireless.select.not_found"), networkName));
            return;
        }

        ItemStack item = getWirelessTerminalInHand(player, hand);
        if (!isWirelessTerminal(item, plugin)) {
            MessageUtils.sendFeedback(player, lang.getMessage("wireless.select.item_missing"));
            return;
        }

        plugin.getNetworkManager().selectWirelessNetwork(player, network.getName());

        TerminalGUI gui = new TerminalGUI(player, network, plugin, true);
        plugin.getChestInteractListener().addOpenTerminal(player.getUniqueId(), gui);
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1.0f, 1.0f);
        gui.open();
    }

    private ItemStack getWirelessTerminalInHand(Player player, EquipmentSlot hand) {
        if (hand == EquipmentSlot.OFF_HAND) {
            return player.getInventory().getItemInOffHand();
        }
        return player.getInventory().getItemInMainHand();
    }

    public static ItemStack createWirelessTerminal(NetworkStoragePlugin plugin) {
        LanguageManager lang = plugin.getLanguageManager();

        ItemStack terminal = new ItemStack(plugin.getConfigManager().getWirelessTerminalMaterial());
        ItemMeta meta = terminal.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(lang.getMessage("wireless_terminal.name"));
            List<String> lore = new ArrayList<>();
            lore.add(lang.getMessage("wireless_terminal.lore1"));
            lore.add(lang.getMessage("wireless_terminal.lore2"));
            meta.setLore(lore);
            ItemUtils.applyCustomModelData(meta, plugin.getConfigManager().getWirelessTerminalCustomModelData());
            meta.getPersistentDataContainer().set(getWirelessTerminalKey(plugin), PersistentDataType.BYTE, (byte) 1);
            terminal.setItemMeta(meta);
        }
        return terminal;
    }

    public static boolean isWirelessTerminal(ItemStack item, NetworkStoragePlugin plugin) {
        if (item == null || item.getType() != plugin.getConfigManager().getWirelessTerminalMaterial()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(getWirelessTerminalKey(plugin), PersistentDataType.BYTE);
    }

    private static NamespacedKey getWirelessTerminalKey(NetworkStoragePlugin plugin) {
        return new NamespacedKey(plugin, "wireless_terminal");
    }
}
