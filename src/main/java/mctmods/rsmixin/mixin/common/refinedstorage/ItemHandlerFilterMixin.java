package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;

import com.raoulvdberge.refinedstorage.RSItems;
import com.raoulvdberge.refinedstorage.api.network.grid.IGridTab;
import com.raoulvdberge.refinedstorage.api.util.IFilter;
import com.raoulvdberge.refinedstorage.apiimpl.network.grid.GridTab;
import com.raoulvdberge.refinedstorage.apiimpl.util.FilterFluid;
import com.raoulvdberge.refinedstorage.apiimpl.util.FilterItem;
import com.raoulvdberge.refinedstorage.inventory.fluid.FluidInventoryFilter;
import com.raoulvdberge.refinedstorage.inventory.item.ItemHandlerFilter;
import com.raoulvdberge.refinedstorage.inventory.item.ItemHandlerFilterItems;
import com.raoulvdberge.refinedstorage.item.ItemFilter;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.ArrayList;
import java.util.List;

@Mixin(value = ItemHandlerFilter.class, remap = false) public abstract class ItemHandlerFilterMixin {
    @Shadow private List<IFilter<?>> filters;
    @Shadow private List<IGridTab> tabs;

    @SuppressWarnings({"unchecked", "rawtypes"}) @Inject(method = "addFilter", at = @At("HEAD"), cancellable = true) private void rsmixin$nestedAwareAddFilter(ItemStack filter, CallbackInfo ci) {
        if (!Config.enableNestedFilterFix) { return; }

        List<IFilter<?>> parsed = new ArrayList<>();
        rsmixin$parseFilter(filter, parsed);

        ItemStack icon = ItemFilter.getIcon(filter);
        FluidStack fluidIcon = ItemFilter.getFluidIcon(filter);

        if (icon.isEmpty() && fluidIcon == null) { filters.addAll(parsed); }
        else { tabs.add(new GridTab((List) parsed, ItemFilter.getName(filter), icon, fluidIcon)); }

        ci.cancel();
    }

    @Unique private static void rsmixin$parseFilter(ItemStack filter, List<IFilter<?>> out) {
        int compare = ItemFilter.getCompare(filter);
        int mode = ItemFilter.getMode(filter);
        boolean modFilter = ItemFilter.isModFilter(filter);

        for (ItemStack stack : new ItemHandlerFilterItems(filter).getFilteredItems()) {
            if (stack.getItem() == RSItems.FILTER) { rsmixin$parseFilter(stack, out); }
            else if (!stack.isEmpty()) { out.add(new FilterItem(stack, compare, mode, modFilter)); }
        }

        for (FluidStack stack : new FluidInventoryFilter(filter).getFilteredFluids()) { out.add(new FilterFluid(stack, compare, mode, modFilter)); }
    }
}
