package com.dermoha.networkstorage.util;

import com.dermoha.networkstorage.NetworkStoragePlugin;
import com.dermoha.networkstorage.managers.LanguageManager;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;

public final class TerminalItemUtils {

    private TerminalItemUtils() {
    }

    public static ItemStack createTerminalItem(NetworkStoragePlugin plugin) {
        LanguageManager lang = plugin.getLanguageManager();
        ItemStack terminal = new ItemStack(plugin.getConfigManager().getTerminalBlockType());
        ItemMeta meta = terminal.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(lang.getMessage("terminal_block.name"));
            meta.setLore(Arrays.asList(
                    lang.getMessage("terminal_block.lore1"),
                    lang.getMessage("terminal_block.lore2")
            ));
            ItemUtils.applyCustomModelData(meta, plugin.getConfigManager().getTerminalBlockCustomModelData());
            meta.getPersistentDataContainer().set(getTerminalItemKey(plugin), PersistentDataType.BYTE, (byte) 1);
            terminal.setItemMeta(meta);
        }

        return terminal;
    }

    public static boolean isTerminalItem(ItemStack item, NetworkStoragePlugin plugin) {
        if (item == null || item.getType() != plugin.getConfigManager().getTerminalBlockType()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(getTerminalItemKey(plugin), PersistentDataType.BYTE);
    }

    private static NamespacedKey getTerminalItemKey(NetworkStoragePlugin plugin) {
        return new NamespacedKey(plugin, "storage_terminal");
    }
}
