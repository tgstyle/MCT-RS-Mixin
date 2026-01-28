package mctmods.rsmixin.client;

import mctmods.rsmixin.RSMixin;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

import com.refinedmods.refinedstorage.RSBlocks;

import java.util.List;

@Mod.EventBusSubscriber(modid = "rsmixin", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class PortableGridModelPreCache {

    @SubscribeEvent
    public static void onModelsBaked(ModelEvent.BakingCompleted event) {
        var modelManager = Minecraft.getInstance().getModelManager();
        RandomSource random = RandomSource.create(42L);
        ModelData modelData = ModelData.EMPTY;

        List<Block> portableGrids = List.of(
                RSBlocks.PORTABLE_GRID.get(),
                RSBlocks.CREATIVE_PORTABLE_GRID.get()
        );

        for (Block block : portableGrids) {
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                BakedModel model = modelManager.getBlockModelShaper().getBlockModel(state);

                for (Direction direction : Direction.values()) {
                    model.getQuads(state, direction, random, modelData, null);
                }
                model.getQuads(state, null, random, modelData, null);
            }
        }
        RSMixin.LOGGER.info("Portable Grid Models baked!");
    }
}
