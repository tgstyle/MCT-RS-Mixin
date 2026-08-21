package mctmods.rsmixin.helper.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.RSMixin;

import com.refinedmods.refinedstorage.api.network.node.INetworkNode;
import com.refinedmods.refinedstorage.util.NetworkUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkNodeDiscovery {
    private static final Map<ResourceKey<Level>, Map<ChunkPos, Long>> PENDING = new ConcurrentHashMap<>();

    @SubscribeEvent public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) { return; }
        if (!Config.ENABLE_CHUNK_LOAD_DISCOVERY.get()) { return; }
        PENDING.computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>()).put(chunk.getPos(), level.getGameTime() + Config.LOAD_RESCAN_DELAY.get());
    }

    @SubscribeEvent public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide()) { return; }
        Map<ChunkPos, Long> pending = PENDING.get(event.level.dimension());
        if (pending == null || pending.isEmpty()) { return; }
        long gameTime = event.level.getGameTime();
        Iterator<Map.Entry<ChunkPos, Long>> it = pending.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ChunkPos, Long> entry = it.next();
            if (gameTime < entry.getValue()) { continue; }
            it.remove();
            discoverChunk((ServerLevel) event.level, entry.getKey());
        }
    }

    private static void discoverChunk(ServerLevel level, ChunkPos pos) {
        if (!level.hasChunk(pos.x, pos.z)) { return; }
        LevelChunk chunk = level.getChunk(pos.x, pos.z);
        for (BlockEntity blockEntity : new ArrayList<>(chunk.getBlockEntities().values())) {
            INetworkNode node = NetworkUtils.getNodeFromBlockEntity(blockEntity);
            if (node == null || node.getNetwork() != null) { continue; }
            for (Direction direction : Direction.values()) {
                BlockPos neighborPos = blockEntity.getBlockPos().relative(direction);
                if (!level.isLoaded(neighborPos)) { continue; }
                INetworkNode neighbor = NetworkUtils.getNodeFromBlockEntity(level.getBlockEntity(neighborPos));
                if (neighbor != null && neighbor.getNetwork() != null) {
                    GraphRescanScheduler.schedule(neighbor.getNetwork());
                    if (Config.ENABLE_DEBUG_LOGGING.get()) { RSMixin.LOGGER.debug("RSMixin: Chunk {} loaded with disconnected node at {}, scheduled rescan of network at {}", pos, blockEntity.getBlockPos(), neighbor.getNetwork().getPosition()); }
                    break;
                }
            }
        }
    }
}
