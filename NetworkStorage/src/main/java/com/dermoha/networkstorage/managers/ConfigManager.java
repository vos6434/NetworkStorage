package com.dermoha.networkstorage.managers;

import com.dermoha.networkstorage.NetworkStoragePlugin;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final NetworkStoragePlugin plugin;
    private FileConfiguration config;

    public enum NetworkMode {
        PLAYER,
        GLOBAL
    }

    public ConfigManager(NetworkStoragePlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
        setDefaults();
        plugin.saveConfig();
    }

    private void setDefaults() {
        config.addDefault("network-mode", "PLAYER");
        config.addDefault("max-chests-per-network", 100);
        config.addDefault("max-terminals-per-network", 100);
        config.addDefault("max-sender-chests-per-network", 100);
        config.addDefault("sender-chest-transfer-interval-seconds", 5);
        config.addDefault("auto-save-interval-minutes", 5);
        config.addDefault("enable-permissions", true);
        config.addDefault("enable-trust-system", true);
        config.addDefault("language", "en");
        config.addDefault("wand-material", "STICK");
        config.addDefault("wireless-terminal-material", "COMPASS");
        config.addDefault("terminal-block-type", "OAK_SHELF");
        config.addDefault("custom-terminal-gui.enabled", true);
        config.addDefault("custom-model-data.wireless-terminal", 10001);
        config.addDefault("custom-model-data.wand", 10002);
        config.addDefault("custom-model-data.terminal-block", 10003);
        config.addDefault("custom-model-data.gui.terminal.prev-page", 10101);
        config.addDefault("custom-model-data.gui.terminal.next-page", 10102);
        config.addDefault("custom-model-data.gui.terminal.search", 10103);
        config.addDefault("custom-model-data.gui.terminal.sort", 10104);
        config.addDefault("custom-model-data.gui.terminal.info", 10105);
        config.addDefault("custom-model-data.gui.terminal.stats", 10106);
        config.addDefault("custom-model-data.gui.terminal.refresh", 10107);
        config.addDefault("custom-model-data.gui.terminal.page-indicator", 10108);
        config.addDefault("custom-model-data.gui.stats.back", 10201);
        config.addDefault("custom-model-data.gui.network-select.item", 10301);
        config.addDefault("custom-model-data.gui.wireless-select.item", 10401);
        config.addDefault("storage-wand-recipe.shape", java.util.Arrays.asList(" C", "S "));
        config.addDefault("storage-wand-recipe.ingredients.C", "CHEST");
        config.addDefault("storage-wand-recipe.ingredients.S", "STICK");
        config.addDefault("storage-terminal-recipe.shape", java.util.Arrays.asList("PRP", "CGL", "PRP"));
        config.addDefault("storage-terminal-recipe.ingredients.P", "PLANKS");
        config.addDefault("storage-terminal-recipe.ingredients.R", "REPEATER");
        config.addDefault("storage-terminal-recipe.ingredients.C", "CHEST");
        config.addDefault("storage-terminal-recipe.ingredients.G", "GLOWSTONE");
        config.addDefault("storage-terminal-recipe.ingredients.L", "GLASS");
        config.addDefault("wireless-terminal-recipe.shape", java.util.Arrays.asList("PRP", "CGL", "PEP"));
        config.addDefault("wireless-terminal-recipe.ingredients.P", "PLANKS");
        config.addDefault("wireless-terminal-recipe.ingredients.R", "REPEATER");
        config.addDefault("wireless-terminal-recipe.ingredients.C", "CHEST");
        config.addDefault("wireless-terminal-recipe.ingredients.G", "GLOWSTONE");
        config.addDefault("wireless-terminal-recipe.ingredients.L", "GLASS");
        config.addDefault("wireless-terminal-recipe.ingredients.E", "ENDER_PEARL");
        config.options().copyDefaults(true);
    }

    public NetworkMode getNetworkMode() {
        try {
            return NetworkMode.valueOf(config.getString("network-mode", "PLAYER").toUpperCase());
        } catch (IllegalArgumentException e) {
            return NetworkMode.PLAYER;
        }
    }

    public int getMaxChestsPerNetwork() {
        return config.getInt("max-chests-per-network");
    }

    public int getMaxTerminalsPerNetwork() {
        return config.getInt("max-terminals-per-network");
    }

    public int getMaxSenderChestsPerNetwork() {
        return config.getInt("max-sender-chests-per-network");
    }

    public int getSenderChestTransferInterval() {
        return getClampedInt("sender-chest-transfer-interval-seconds", 5, 1, 86_400);
    }

    public Material getWandMaterial() {
        return parseMaterial("wand-material", Material.STICK);
    }

    public Material getWirelessTerminalMaterial() {
        return parseMaterial("wireless-terminal-material", Material.COMPASS);
    }

    public Material getTerminalBlockType() {
        return parseMaterial("terminal-block-type", Material.OAK_SHELF);
    }

    public boolean isNetworkContainerBlock(Material material) {
        return material == Material.CHEST
                || material == Material.TRAPPED_CHEST
                || material == getTerminalBlockType();
    }

    public Integer getOptionalCustomModelData(String path) {
        if (!config.isSet(path) || !config.isInt(path)) {
            return null;
        }
        return config.getInt(path);
    }

    public Integer getWirelessTerminalCustomModelData() {
        return getOptionalCustomModelData("custom-model-data.wireless-terminal");
    }

    public Integer getStorageWandCustomModelData() {
        return getOptionalCustomModelData("custom-model-data.wand");
    }

    public Integer getTerminalBlockCustomModelData() {
        return getOptionalCustomModelData("custom-model-data.terminal-block");
    }

    public boolean isPermissionsEnabled() {
        return config.getBoolean("enable-permissions");
    }

    public boolean hasPermission(CommandSender sender, String permission) {
        return !isPermissionsEnabled() || sender.hasPermission(permission);
    }

    public boolean hasPrivilege(CommandSender sender, String permission) {
        return isPermissionsEnabled() && sender.hasPermission(permission);
    }

    public boolean isTrustSystemEnabled() {
        return config.getBoolean("enable-trust-system");
    }

    public boolean isCustomTerminalGuiEnabled() {
        return config.getBoolean("custom-terminal-gui.enabled", true);
    }

    public int getAutoSaveInterval() {
        return getClampedInt("auto-save-interval-minutes", 5, 1, 10_080);
    }

    public String getLanguage() {
        return config.getString("language");
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        setDefaults();
        plugin.saveConfig();
    }

    private int getClampedInt(String path, int defaultValue, int min, int max) {
        int value = config.getInt(path, defaultValue);
        int clamped = Math.max(min, Math.min(max, value));
        if (value != clamped) {
            plugin.getLogger().warning("Config value '" + path + "' must be between " + min + " and " + max + "; using " + clamped + ".");
        }
        return clamped;
    }

    private Material parseMaterial(String path, Material fallback) {
        String name = config.getString(path, fallback.name());
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material '" + name + "' for " + path + ", using " + fallback.name() + ".");
            return fallback;
        }
    }
}
