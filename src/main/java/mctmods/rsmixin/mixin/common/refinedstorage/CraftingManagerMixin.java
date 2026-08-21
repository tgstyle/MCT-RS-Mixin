package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.RSMixin;
import mctmods.rsmixin.core.interfaces.ICraftingRebuild;
import mctmods.rsmixin.core.interfaces.IGraphBatch;
import mctmods.rsmixin.helper.refinedstorage.CraftingTicker;

import com.raoulvdberge.refinedstorage.api.autocrafting.ICraftingPattern;
import com.raoulvdberge.refinedstorage.api.autocrafting.task.ICraftingTask;
import com.raoulvdberge.refinedstorage.api.autocrafting.registry.ICraftingTaskFactory;
import com.raoulvdberge.refinedstorage.api.util.IComparer;
import com.raoulvdberge.refinedstorage.apiimpl.API;
import com.raoulvdberge.refinedstorage.apiimpl.autocrafting.CraftingManager;
import com.raoulvdberge.refinedstorage.tile.TileController;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mixin(value = CraftingManager.class, remap = false) public abstract class CraftingManagerMixin implements ICraftingRebuild {
    @Shadow private TileController network;
    @Shadow private Map<UUID, ICraftingTask> tasks;
    @Shadow private NBTTagList tasksToRead;
    @Shadow private List<ICraftingPattern> patterns;
    @Shadow private List<ICraftingTask> tasksToAdd;
    @Unique private boolean rsmixin$rebuildQueued = false;
    @Unique private Map<Item, List<ICraftingPattern>> rsmixin$itemPatternIndex;
    @Unique private Map<Fluid, List<ICraftingPattern>> rsmixin$fluidPatternIndex;
    @Unique private long rsmixin$lastDirtyMark = Long.MIN_VALUE;
    @Unique private static final Logger rsmixin$LOGGER = LogManager.getLogger(RSMixin.MODID);

    @Shadow public abstract void update();

    @Unique private void rsmixin$drainPendingTasks() {
        if (tasksToRead == null || !network.hasWorld() || !network.canRun()) { return; }

        for (int i = 0; i < tasksToRead.tagCount(); ++i) {
            NBTTagCompound taskTag = tasksToRead.getCompoundTagAt(i);
            ICraftingTaskFactory factory = API.instance().getCraftingTaskRegistry().get(taskTag.getString("Type"));
            if (factory == null) { continue; }
            try {
                ICraftingTask task = factory.createFromNbt(network, taskTag.getCompoundTag("Task"));
                tasks.put(task.getId(), task);
            }
            catch (Exception e) { rsmixin$LOGGER.error("RSMixin: Dropped a saved crafting task that could not be restored for the network at {}", network.getPosition(), e); }
        }
        tasksToRead = null;
    }

    @Inject(method = "update", at = @At("HEAD")) private void rsmixin$guardedRestore(CallbackInfo ci) {
        if (Config.enableCraftingCrashGuard) { rsmixin$drainPendingTasks(); }
    }

    @Inject(method = {"request(Ljava/lang/Object;Lnet/minecraft/item/ItemStack;I)Lcom/raoulvdberge/refinedstorage/api/autocrafting/task/ICraftingTask;", "request(Ljava/lang/Object;Lnet/minecraftforge/fluids/FluidStack;I)Lcom/raoulvdberge/refinedstorage/api/autocrafting/task/ICraftingTask;"}, at = @At("HEAD")) private void rsmixin$restoreBeforeRequest(CallbackInfoReturnable<ICraftingTask> cir) {
        if (Config.enableRestoredTaskDedup) { rsmixin$drainPendingTasks(); }
    }

    @Redirect(method = {"request(Ljava/lang/Object;Lnet/minecraft/item/ItemStack;I)Lcom/raoulvdberge/refinedstorage/api/autocrafting/task/ICraftingTask;", "request(Ljava/lang/Object;Lnet/minecraftforge/fluids/FluidStack;I)Lcom/raoulvdberge/refinedstorage/api/autocrafting/task/ICraftingTask;"}, at = @At(value = "INVOKE", target = "Lcom/raoulvdberge/refinedstorage/apiimpl/autocrafting/CraftingManager;getTasks()Ljava/util/Collection;")) private Collection<ICraftingTask> rsmixin$tasksIncludingPending(CraftingManager self) {
        Collection<ICraftingTask> current = self.getTasks();
        if (!Config.enableRestoredTaskDedup || tasksToAdd.isEmpty()) { return current; }
        List<ICraftingTask> all = new ArrayList<>(current);
        all.addAll(tasksToAdd);
        return all;
    }

    @Inject(method = "writeToNbt", at = @At("RETURN")) private void rsmixin$preservePendingTasks(NBTTagCompound tag, CallbackInfoReturnable<NBTTagCompound> cir) {
        if (!Config.enableCraftingCrashGuard || tasksToRead == null || tasksToRead.tagCount() == 0) { return; }

        NBTTagList list = tag.getTagList("Tasks", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < tasksToRead.tagCount(); ++i) { list.appendTag(tasksToRead.getCompoundTagAt(i).copy()); }
        tag.setTag("Tasks", list);
        if (Config.enableDebugLogging) { rsmixin$LOGGER.debug("RSMixin: Preserved {} not-yet-restored crafting tasks while saving the network at {}", tasksToRead.tagCount(), network.getPosition()); }
    }

    @Inject(method = "rebuild", at = @At("TAIL")) private void rsmixin$rebuildPatternIndex(CallbackInfo ci) {
        if (!Config.enablePatternLookupIndex) {
            rsmixin$itemPatternIndex = null;
            rsmixin$fluidPatternIndex = null;
            return;
        }

        Map<Item, List<ICraftingPattern>> itemIndex = new HashMap<>();
        Map<Fluid, List<ICraftingPattern>> fluidIndex = new HashMap<>();
        for (ICraftingPattern pattern : patterns) {
            for (ItemStack output : pattern.getOutputs()) {
                itemIndex.computeIfAbsent(output.getItem(), k -> new ArrayList<>()).add(pattern);
            }
            for (FluidStack output : pattern.getFluidOutputs()) {
                fluidIndex.computeIfAbsent(output.getFluid(), k -> new ArrayList<>()).add(pattern);
            }
        }
        rsmixin$itemPatternIndex = itemIndex;
        rsmixin$fluidPatternIndex = fluidIndex;
        if (Config.enableDebugLogging) { rsmixin$LOGGER.debug("RSMixin: Rebuilt pattern lookup index ({} item keys, {} fluid keys, {} patterns)", itemIndex.size(), fluidIndex.size(), patterns.size()); }
    }

    @Inject(method = "getPattern(Lnet/minecraft/item/ItemStack;)Lcom/raoulvdberge/refinedstorage/api/autocrafting/ICraftingPattern;", at = @At("HEAD"), cancellable = true) private void rsmixin$indexedItemPatternLookup(ItemStack pattern, CallbackInfoReturnable<ICraftingPattern> cir) {
        if (!Config.enablePatternLookupIndex || rsmixin$itemPatternIndex == null) { return; }

        List<ICraftingPattern> candidates = rsmixin$itemPatternIndex.get(pattern.getItem());
        if (candidates == null) {
            cir.setReturnValue(null);
            return;
        }

        for (ICraftingPattern candidate : candidates) {
            for (ItemStack output : candidate.getOutputs()) {
                if (API.instance().getComparer().isEqualNoQuantity(output, pattern)) {
                    cir.setReturnValue(candidate);
                    return;
                }
            }
        }
        cir.setReturnValue(null);
    }

    @Inject(method = "getPattern(Lnet/minecraftforge/fluids/FluidStack;)Lcom/raoulvdberge/refinedstorage/api/autocrafting/ICraftingPattern;", at = @At("HEAD"), cancellable = true) private void rsmixin$indexedFluidPatternLookup(FluidStack pattern, CallbackInfoReturnable<ICraftingPattern> cir) {
        if (!Config.enablePatternLookupIndex || rsmixin$fluidPatternIndex == null) { return; }

        List<ICraftingPattern> candidates = rsmixin$fluidPatternIndex.get(pattern.getFluid());
        if (candidates == null) {
            cir.setReturnValue(null);
            return;
        }

        for (ICraftingPattern candidate : candidates) {
            for (FluidStack output : candidate.getFluidOutputs()) {
                if (API.instance().getComparer().isEqual(output, pattern, IComparer.COMPARE_NBT)) {
                    cir.setReturnValue(candidate);
                    return;
                }
            }
        }
        cir.setReturnValue(null);
    }

    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lcom/raoulvdberge/refinedstorage/tile/TileController;markDirty()V"), remap = true) private void rsmixin$throttleDirtyMark(TileController controller) {
        if (!Config.enableCraftingDirtyThrottle) {
            controller.markDirty();
            return;
        }
        long gameTime = controller.getWorld().getTotalWorldTime();
        if (gameTime - rsmixin$lastDirtyMark >= 20L) {
            rsmixin$lastDirtyMark = gameTime;
            controller.markDirty();
        }
    }

    @Override public boolean rsmixin$consumeRebuildQueued() {
        boolean queued = rsmixin$rebuildQueued;
        rsmixin$rebuildQueued = false;
        return queued;
    }

    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lcom/raoulvdberge/refinedstorage/api/autocrafting/task/ICraftingTask;update()Z")) private boolean rsmixin$guardedTaskUpdate(ICraftingTask task) {
        if (!Config.enableCraftingCrashGuard) { return task.update(); }
        try { return task.update(); }
        catch (Exception e) {
            rsmixin$LOGGER.error("RSMixin: Crafting task {} for {} hit an internal error and was cancelled to prevent a server crash. Its items have been refunded to the network at {}.", task.getId(), task.getRequested(), network.getPosition(), e);
            try { task.onCancelled(); }
            catch (Exception cancelError) { rsmixin$LOGGER.error("RSMixin: Failed to refund cancelled crafting task {}", task.getId(), cancelError); }
            return true;
        }
    }

    @Inject(method = "rebuild", at = @At("HEAD"), cancellable = true) private void rsmixin$deferDuringRescan(CallbackInfo ci) {
        if (!Config.enableCraftingRebuildDebounce) { return; }
        if (network.getNodeGraph() instanceof IGraphBatch && ((IGraphBatch) network.getNodeGraph()).rsmixin$isBatching()) {
            rsmixin$rebuildQueued = true;
            if (Config.enableDebugLogging) { rsmixin$LOGGER.debug("RSMixin: Deferred crafting pattern reindex for network at {} until rescan completes", network.getPosition()); }
            ci.cancel();
        }
    }

    @Inject(method = "update", at = @At("TAIL")) private void dynamicCraftingBypass(CallbackInfo ci) {
        if (!Config.enableDynamicCraftingBypass ||
                !Config.enableThrottle ||
                Config.throttleInterval <= 1 ||
                !Config.enableBypassFastNodes) {
            return;
        }

        if (tasks.isEmpty()) { CraftingTicker.unregister(network); }
        else { CraftingTicker.register(network); }
    }

    @Inject(method = "add", at = @At("TAIL")) private void immediateCraftingStart(ICraftingTask task, CallbackInfo ci) {
        if (!Config.enableDynamicCraftingBypass ||
                !Config.enableThrottle ||
                Config.throttleInterval <= 1 ||
                !Config.enableBypassFastNodes) {
            return;
        }

        update();
        update();
        update();

        if (!tasks.isEmpty()) { CraftingTicker.register(network); }
    }

    @Inject(method = "readFromNbt", at = @At("TAIL")) private void immediateCraftingLoad(NBTTagCompound tag, CallbackInfo ci) {
        if (!Config.enableDynamicCraftingBypass ||
                !Config.enableThrottle ||
                Config.throttleInterval <= 1 ||
                !Config.enableBypassFastNodes) {
            return;
        }

        if (!network.hasWorld()) { return; }

        if (tasksToRead != null && tasksToRead.tagCount() > 0) {
            update();

            if (!tasks.isEmpty()) { CraftingTicker.register(network); }
        }
    }
}
