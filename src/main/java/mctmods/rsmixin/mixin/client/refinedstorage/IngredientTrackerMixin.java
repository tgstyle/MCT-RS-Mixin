package mctmods.rsmixin.mixin.client.refinedstorage;

import mctmods.rsmixin.Config;

import com.refinedmods.refinedstorage.container.GridContainerMenu;
import com.refinedmods.refinedstorage.integration.jei.IngredientTracker;
import com.refinedmods.refinedstorage.screen.grid.GridScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = IngredientTracker.class, remap = false) public abstract class IngredientTrackerMixin {
    @Shadow private static IngredientTracker INSTANCE;
    @Unique private static GridContainerMenu rsmixin$builtFor;
    @Unique private static int rsmixin$builtStackCount;

    @Inject(method = "getTracker", at = @At("HEAD")) private static void rsmixin$refreshStaleTracker(GridContainerMenu gridContainer, CallbackInfoReturnable<IngredientTracker> cir) {
        if (!Config.ENABLE_JEI_TRACKER_REFRESH.get()) { return; }
        int stackCount = -1;
        if (gridContainer.getScreenInfoProvider() instanceof GridScreen gridScreen) {
            stackCount = gridContainer.getGrid().isGridActive() ? gridScreen.getView().getAllStacks().size() : 0;
        }
        if (INSTANCE != null && (rsmixin$builtFor != gridContainer || (rsmixin$builtStackCount == 0 && stackCount > 0))) { INSTANCE = null; }
        if (INSTANCE == null) {
            rsmixin$builtFor = gridContainer;
            rsmixin$builtStackCount = stackCount;
        }
    }
}
