package com.dermoha.networkstorage.listeners;

import com.dermoha.networkstorage.NetworkStoragePlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class RecipeDiscoveryListener implements Listener {

    private final NetworkStoragePlugin plugin;

    public RecipeDiscoveryListener(NetworkStoragePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.discoverPluginRecipes(event.getPlayer());
    }
}
