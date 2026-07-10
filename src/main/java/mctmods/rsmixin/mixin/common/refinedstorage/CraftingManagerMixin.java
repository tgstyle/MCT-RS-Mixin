package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.core.accessor.ICraftingRebuildAccessor;
import mctmods.rsmixin.core.accessor.IGraphBatchAccessor;
import mctmods.rsmixin.helper.refinedstorage.CraftingTicker;

import com.refinedmods.refinedstorage.api.autocrafting.task.ICraftingTask;
import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.apiimpl.autocrafting.CraftingManager;
import net.minecraft.nbt.ListTag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Map;
import java.util.UUID;

import static mctmods.rsmixin.RSMixin.MODID;

@Mixin(value = CraftingManager.class, remap = false) public abstract class CraftingManagerMixin implements ICraftingRebuildAccessor {
    @Shadow @Final private INetwork network;
    @Shadow @Final private Map<UUID, ICraftingTask> tasks;
    @Shadow private ListTag tasksToRead;
    @Unique private boolean rsmixin$rebuildQueued = false;
    @Unique private static final Logger rsmixin$LOGGER = LogManager.getLogger(MODID);

    @Shadow public void update() {}

    @Override public boolean rsmixin$consumeRebuildQueued() {
        boolean queued = rsmixin$rebuildQueued;
        rsmixin$rebuildQueued = false;
        return queued;
    }

    @Redirect(method = "updateTasks", at = @At(value = "INVOKE", target = "Lcom/refinedmods/refinedstorage/api/autocrafting/task/ICraftingTask;update()Z")) private boolean rsmixin$guardedTaskUpdate(ICraftingTask task) {
        if (!Config.ENABLE_CRAFTING_CRASH_GUARD.get()) { return task.update(); }
        try { return task.update(); }
        catch (Exception e) {
            rsmixin$LOGGER.error("RSMixin: Crafting task {} for {} hit an internal error and was cancelled to prevent a server crash. Its items have been refunded to the network at {}.", task.getId(), task.getRequested(), network.getPosition(), e);
            try { task.onCancelled(); }
            catch (Exception cancelError) { rsmixin$LOGGER.error("RSMixin: Failed to refund cancelled crafting task {}", task.getId(), cancelError); }
            return true;
        }
    }

    @Inject(method = "invalidate", at = @At("HEAD"), cancellable = true) private void rsmixin$deferDuringRescan(CallbackInfo ci) {
        if (!Config.ENABLE_CRAFTING_REBUILD_DEBOUNCE.get()) { return; }
        if (network.getNodeGraph() instanceof IGraphBatchAccessor graph && graph.rsmixin$isBatching()) {
            rsmixin$rebuildQueued = true;
            if (Config.ENABLE_DEBUG_LOGGING.get()) { rsmixin$LOGGER.debug("RSMixin: Deferred crafting pattern reindex for network at {} until rescan completes", network.getPosition()); }
            ci.cancel();
        }
    }

    @Inject(method = "update", at = @At("TAIL")) private void dynamicCraftingBypass(CallbackInfo ci) {
        if (!Config.ENABLE_DYNAMIC_CRAFTING_BYPASS.get() ||
                !Config.ENABLE_THROTTLE.get() ||
                Config.THROTTLE_INTERVAL.get() <= 1 ||
                !Config.ENABLE_BYPASS_FAST_NODES.get()) {
            return;
        }

        if (tasks.isEmpty()) { CraftingTicker.unregister(network); }
        else { CraftingTicker.register(network); }
    }

    @Inject(method = "start", at = @At("TAIL")) private void immediateCraftingStart(ICraftingTask task, CallbackInfo ci) {
        if (!Config.ENABLE_DYNAMIC_CRAFTING_BYPASS.get() ||
                !Config.ENABLE_THROTTLE.get() ||
                Config.THROTTLE_INTERVAL.get() <= 1 ||
                !Config.ENABLE_BYPASS_FAST_NODES.get()) {
            return;
        }

        update();
        update();
        update();

        if (!tasks.isEmpty()) { CraftingTicker.register(network); }
    }

    @Inject(method = "readFromNbt", at = @At("TAIL")) private void immediateCraftingLoad(CallbackInfo ci) {
        if (!Config.ENABLE_DYNAMIC_CRAFTING_BYPASS.get() ||
                !Config.ENABLE_THROTTLE.get() ||
                Config.THROTTLE_INTERVAL.get() <= 1 ||
                !Config.ENABLE_BYPASS_FAST_NODES.get()) {
            return;
        }

        if (tasksToRead != null && !tasksToRead.isEmpty()) {
            update();

            if (!tasks.isEmpty()) { CraftingTicker.register(network); }
        }
    }
}
