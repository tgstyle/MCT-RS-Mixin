package mctmods.rsmixin.mixin.refinedstorage;

import com.refinedmods.refinedstorage.api.autocrafting.task.ICraftingTask;
import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.apiimpl.autocrafting.CraftingManager;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.helper.refinedstorage.CraftingTicker;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;

@Mixin(value = CraftingManager.class, remap = false)
public abstract class CraftingManagerMixin {

    @Shadow @Final private INetwork network;

    @Shadow @Final private Map<UUID, ICraftingTask> tasks;

    @Inject(method = "update", at = @At("TAIL"))
    private void dynamicCraftingBypass(CallbackInfo ci) {
        if (!Config.ENABLE_DYNAMIC_CRAFTING_BYPASS.get() ||
                !Config.ENABLE_THROTTLE.get() ||
                Config.THROTTLE_INTERVAL.get() <= 1 ||
                !Config.ENABLE_BYPASS_FAST_NODES.get()) {
            return;
        }

        if (tasks.isEmpty()) {
            CraftingTicker.unregister(network);
        } else {
            CraftingTicker.register(network);
        }
    }
}
