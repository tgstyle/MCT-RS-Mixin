package mctmods.rsmixin;

import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.util.Action;
import com.refinedmods.refinedstorage.util.NetworkUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ConduitPlacementFix {

    private static final String ENDERIO_CONDUIT_BLOCK_CLASS = "com.enderio.conduits.common.conduit.block.ConduitBlock";

    private static final Map<ServerLevel, Set<INetwork>> dirtyNetworks = new HashMap<>();
    private static final Map<ServerLevel, Integer> levelTimers = new HashMap<>();

    public static void handleConduitUpdate(ServerLevel level, BlockPos pos) {
        if (!Config.ENABLE_CONDUIT_PLACEMENT_FIX.get()) return;

        boolean foundNetwork = false;
        Set<INetwork> networksForLevel = dirtyNetworks.computeIfAbsent(level, k -> new HashSet<>());

        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = pos.relative(direction);
            BlockEntity be = level.getBlockEntity(adjacentPos);
            INetwork network = NetworkUtils.getNetworkFromNode(NetworkUtils.getNodeFromBlockEntity(be));
            if (network != null && networksForLevel.add(network)) {
                foundNetwork = true;
            }
        }

        if (foundNetwork) {
            levelTimers.put(level, Config.CONDUIT_PLACEMENT_RESCAN_DELAY.get());

            if (Config.ENABLE_DEBUG_LOGGING.get()) {
                RSMixin.LOGGER.info("RSMixin: Marked {} network(s) dirty for delayed rescan ({} ticks) due to EnderIO conduit update at {}", networksForLevel.size(), Config.CONDUIT_PLACEMENT_RESCAN_DELAY.get(), pos);
            }
        }
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!Config.ENABLE_CONDUIT_PLACEMENT_FIX.get()) return;

        LevelAccessor levelAccessor = event.getLevel();
        if (levelAccessor.isClientSide()) return;

        if (!(levelAccessor instanceof ServerLevel level)) return;

        BlockState placedState = event.getPlacedBlock();
        if (!placedState.getBlock().getClass().getName().equals(ENDERIO_CONDUIT_BLOCK_CLASS)) return;

        BlockPos pos = event.getPos();

        handleConduitUpdate(level, pos);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (!Config.ENABLE_CONDUIT_PLACEMENT_FIX.get()) return;

        levelTimers.entrySet().removeIf(entry -> {
            ServerLevel level = entry.getKey();
            int timer = entry.getValue() - 1;
            if (timer <= 0) {
                Set<INetwork> networks = dirtyNetworks.remove(level);
                if (networks != null) {
                    for (INetwork network : networks) {
                        network.getNodeGraph().invalidate(Action.PERFORM, level, network.getPosition());
                    }

                    if (Config.ENABLE_DEBUG_LOGGING.get()) {
                        RSMixin.LOGGER.info("RSMixin: Performed delayed graph rescan for {} network(s) in level {}", networks.size(), level.dimension().location());
                    }
                }
                return true;
            } else {
                entry.setValue(timer);
                return false;
            }
        });
    }
}
