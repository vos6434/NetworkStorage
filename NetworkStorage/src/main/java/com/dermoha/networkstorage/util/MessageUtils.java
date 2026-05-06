package com.dermoha.networkstorage.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class MessageUtils {

    private MessageUtils() {
    }

    public static void sendActionBar(Player player, String message) {
        player.sendActionBar(Component.text(ChatColor.stripColor(message), NamedTextColor.WHITE));
    }

    public static void sendFeedback(Player player, String message) {
        player.sendMessage(message);
    }

    public static void sendFeedback(CommandSender sender, String message) {
        sender.sendMessage(message);
    }
}
