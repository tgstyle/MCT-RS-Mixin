package mctmods.rsmixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.helper.rebornstorage.MultiblockFault;
import mctmods.rsmixin.helper.rebornstorage.MultiblockReport;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = "rsmixin", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CrafterBuildHints {

    private static final int MAX_BOXES = 128;
    private static final long HIGHLIGHT_MILLIS = 12_000L;
    private static final double INSET = 0.01D;

    private static final List<MultiblockFault> HIGHLIGHTS = new ArrayList<>();
    private static long highlightUntil;

    // Remembers the last "formed but idle" crafter we spoke about, so opening a working
    // crafter over and over does not repeat the same advice.
    private static ResourceKey<Level> advisedDimension;
    private static BlockPos advisedCrafter;
    private static int advisedCpu;
    private static int advisedStorage;

    private CrafterBuildHints() {
    }

    public static void show(Player player, List<MultiblockFault> faults, BlockPos min, BlockPos max, BlockPos origin,
                            int cpuCount, int storageCount) {
        if (!Config.ENABLE_REBORNSTORAGE_BUILD_HINTS.get()) {
            clear();
            return;
        }

        forgetAdvice();
        player.displayClientMessage(MultiblockReport.actionBar(faults), true);
        for (Component line : MultiblockReport.lines(faults, min, max, origin, cpuCount, storageCount)) {
            player.displayClientMessage(line, false);
        }

        HIGHLIGHTS.clear();
        for (MultiblockFault fault : faults) {
            if (HIGHLIGHTS.size() >= MAX_BOXES) { break; }
            HIGHLIGHTS.add(fault);
        }
        highlightUntil = Util.getMillis() + HIGHLIGHT_MILLIS;
    }

    /**
     * The crafter is assembled, but has no CPU and/or no storage, so it cannot do anything
     * yet. Said once per crafter: repeats only if its CPU/storage count changes, or if you
     * go to a different crafter and come back.
     */
    public static void showFormed(Player player, BlockPos min, BlockPos max, int cpuCount, int storageCount) {
        clear();
        if (!Config.ENABLE_REBORNSTORAGE_BUILD_HINTS.get()) {
            forgetAdvice();
            return;
        }

        ResourceKey<Level> dimension = player.level().dimension();
        if (dimension.equals(advisedDimension) && min != null && min.equals(advisedCrafter)
                && cpuCount == advisedCpu && storageCount == advisedStorage) {
            return;
        }
        advisedDimension = dimension;
        advisedCrafter = min == null ? null : min.immutable();
        advisedCpu = cpuCount;
        advisedStorage = storageCount;

        for (Component line : MultiblockReport.formed(min, max, cpuCount, storageCount)) {
            player.displayClientMessage(line, false);
        }
    }

    /** Forget what we last advised, so the next idle crafter is announced again. */
    public static void forgetAdvice() {
        advisedDimension = null;
        advisedCrafter = null;
        advisedCpu = -1;
        advisedStorage = -1;
    }

    public static void clear() {
        HIGHLIGHTS.clear();
        highlightUntil = 0L;
    }

    @SubscribeEvent public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) { return; }
        if (HIGHLIGHTS.isEmpty()) { return; }
        if (Util.getMillis() > highlightUntil || !Config.ENABLE_REBORNSTORAGE_BUILD_HINTS.get()) {
            clear();
            return;
        }

        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        PoseStack pose = event.getPoseStack();

        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        for (MultiblockFault fault : HIGHLIGHTS) {
            BlockPos pos = fault.pos();
            AABB box = new AABB(pos.getX() + INSET, pos.getY() + INSET, pos.getZ() + INSET,
                    pos.getX() + 1 - INSET, pos.getY() + 1 - INSET, pos.getZ() + 1 - INSET);
            if (fault.missing()) {
                LevelRenderer.renderLineBox(pose, lines, box, 0.2F, 0.9F, 1.0F, 0.9F);
            } else {
                LevelRenderer.renderLineBox(pose, lines, box, 1.0F, 0.25F, 0.25F, 0.9F);
            }
        }
        pose.popPose();
        buffers.endBatch(RenderType.lines());
    }
}
