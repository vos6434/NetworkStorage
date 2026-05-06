package com.dermoha.networkstorage.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.BlockFace;

public final class TerminalBlockUtils {

    private static final Material TERMINAL_SHELF_MATERIAL = Material.OAK_SHELF;

    private TerminalBlockUtils() {
    }

    public static boolean isTerminalShelf(Material material) {
        return material == TERMINAL_SHELF_MATERIAL;
    }

    public static void applyTerminalShelfState(Block block) {
        if (!isTerminalShelf(block.getType())) {
            return;
        }
        block.setBlockData(createShelfData(block, "center"), false);
    }

    public static void clearTerminalShelfState(Block block) {
        if (!isTerminalShelf(block.getType())) {
            return;
        }
        block.setBlockData(createShelfData(block, "unconnected"), false);
    }

    private static BlockData createShelfData(Block block, String sideChain) {
        BlockData currentData = block.getBlockData();
        BlockFace facing = currentData instanceof Directional directional ? directional.getFacing() : BlockFace.NORTH;
        boolean waterlogged = currentData instanceof Waterlogged waterloggable && waterloggable.isWaterlogged();
        String blockData = String.format(
                "minecraft:oak_shelf[facing=%s,powered=false,side_chain=%s,waterlogged=%s]",
                facing.name().toLowerCase(),
                sideChain,
                waterlogged
        );
        return Bukkit.createBlockData(blockData);
    }
}
