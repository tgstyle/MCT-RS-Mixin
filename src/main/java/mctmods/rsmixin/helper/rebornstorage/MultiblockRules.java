package mctmods.rsmixin.helper.rebornstorage;

import net.gigabit101.rebornstorage.init.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITagManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class MultiblockRules {

    public static final TagKey<Block> INNER_BASE = TagKey.create(Registries.BLOCK,
            Objects.requireNonNull(ResourceLocation.tryBuild("rebornstorage", "multiblock_inner_base")));

    private MultiblockRules() {
    }

    public static boolean isCrafterBlock(BlockState state) {
        return state.is(ModBlocks.BLOCK_MULTI_FRAME.get())
                || state.is(ModBlocks.BLOCK_MULTI_HEAT.get())
                || state.is(ModBlocks.BLOCK_MULTI_CPU.get())
                || state.is(ModBlocks.BLOCK_MULTI_STORAGE.get());
    }

    public static boolean allows(MultiblockRegion region, BlockState state) {
        if (region == MultiblockRegion.INTERIOR) {
            return state.isAir() || isCrafterBlock(state) || state.is(INNER_BASE);
        }
        return isCrafterBlock(state);
    }

    public static Component allowedList(MultiblockRegion region) {
        List<Component> parts = new ArrayList<>();
        if (region == MultiblockRegion.INTERIOR) {
            parts.add(Blocks.AIR.getName());
        }
        for (Block block : allowedBlocks(region)) {
            parts.add(block.getName());
        }
        MutableComponent out = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                out.append(", ");
            }
            out.append(parts.get(i));
        }
        return out;
    }

    private static List<Block> allowedBlocks(MultiblockRegion region) {
        List<Block> blocks = new ArrayList<>();
        blocks.add(ModBlocks.BLOCK_MULTI_FRAME.get());
        blocks.add(ModBlocks.BLOCK_MULTI_HEAT.get());
        blocks.add(ModBlocks.BLOCK_MULTI_CPU.get());
        blocks.add(ModBlocks.BLOCK_MULTI_STORAGE.get());
        if (region == MultiblockRegion.INTERIOR) {
            ITagManager<Block> tags = ForgeRegistries.BLOCKS.tags();
            if (tags != null) {
                for (Block tagged : tags.getTag(INNER_BASE)) {
                    if (!blocks.contains(tagged)) {
                        blocks.add(tagged);
                    }
                }
            }
        }
        return blocks;
    }
}
