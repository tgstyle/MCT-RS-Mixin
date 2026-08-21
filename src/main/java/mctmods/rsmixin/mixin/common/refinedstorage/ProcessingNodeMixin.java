package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;

import com.refinedmods.refinedstorage.api.autocrafting.ICraftingPatternContainer;
import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.storage.disk.IStorageDisk;
import com.refinedmods.refinedstorage.api.util.Action;
import com.refinedmods.refinedstorage.apiimpl.autocrafting.task.v6.node.NodeList;
import com.refinedmods.refinedstorage.apiimpl.autocrafting.task.v6.node.NodeListener;
import com.refinedmods.refinedstorage.apiimpl.autocrafting.task.v6.node.ProcessingNode;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.util.Collection;

import static mctmods.rsmixin.RSMixin.MODID;

@Mixin(value = ProcessingNode.class, remap = false) public abstract class ProcessingNodeMixin {
    @Unique private static final Logger rsmixin$LOGGER = LogManager.getLogger(MODID);

    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lcom/refinedmods/refinedstorage/api/autocrafting/ICraftingPatternContainer;insertItemsIntoInventory(Ljava/util/Collection;Lcom/refinedmods/refinedstorage/api/util/Action;)Z", ordinal = 1))
    private boolean rsmixin$refundItemShortfall(ICraftingPatternContainer container, Collection<ItemStack> toInsert, Action action, INetwork network, int ticks, NodeList nodes, IStorageDisk<ItemStack> internalStorage, IStorageDisk<FluidStack> internalFluidStorage, NodeListener listener) {
        if (!Config.ENABLE_PROCESSING_VOID_GUARD.get()) { return container.insertItemsIntoInventory(toInsert, action); }
        if (toInsert.isEmpty()) { return true; }
        IItemHandler dest = container.getConnectedInventory();
        boolean complete = true;
        for (ItemStack stack : toInsert) {
            ItemStack remainder = dest == null ? stack : ItemHandlerHelper.insertItem(dest, stack.copy(), false);
            if (!remainder.isEmpty()) {
                complete = false;
                network.insertItem(remainder, remainder.getCount(), Action.PERFORM);
                rsmixin$LOGGER.warn("RSMixin: Machine at crafter {} did not accept {} x{} during autocrafting; refunded to the network instead of voiding", container.getPosition(), remainder.getItem(), remainder.getCount());
            }
        }
        return complete;
    }

    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lcom/refinedmods/refinedstorage/api/autocrafting/ICraftingPatternContainer;insertFluidsIntoInventory(Ljava/util/Collection;Lcom/refinedmods/refinedstorage/api/util/Action;)Z", ordinal = 1))
    private boolean rsmixin$refundFluidShortfall(ICraftingPatternContainer container, Collection<FluidStack> toInsert, Action action, INetwork network, int ticks, NodeList nodes, IStorageDisk<ItemStack> internalStorage, IStorageDisk<FluidStack> internalFluidStorage, NodeListener listener) {
        if (!Config.ENABLE_PROCESSING_VOID_GUARD.get()) { return container.insertFluidsIntoInventory(toInsert, action); }
        if (toInsert.isEmpty()) { return true; }
        IFluidHandler dest = container.getConnectedFluidInventory();
        boolean complete = true;
        for (FluidStack stack : toInsert) {
            int filled = dest == null ? 0 : dest.fill(stack, IFluidHandler.FluidAction.EXECUTE);
            if (filled != stack.getAmount()) {
                complete = false;
                FluidStack refund = stack.copy();
                refund.setAmount(stack.getAmount() - filled);
                network.insertFluid(refund, refund.getAmount(), Action.PERFORM);
                rsmixin$LOGGER.warn("RSMixin: Machine at crafter {} did not accept {} mB of {} during autocrafting; refunded to the network instead of voiding", container.getPosition(), refund.getAmount(), refund.getDisplayName().getString());
            }
        }
        return complete;
    }
}
