package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.RSMixin;

import com.raoulvdberge.refinedstorage.apiimpl.network.node.NetworkNode;
import com.raoulvdberge.refinedstorage.apiimpl.network.node.NetworkNodeFluidInterface;
import com.raoulvdberge.refinedstorage.inventory.item.ItemHandlerBase;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = NetworkNodeFluidInterface.class, remap = false) public abstract class NetworkNodeFluidInterfaceMixin extends NetworkNode {
    @Unique private static final Logger rsmixin$LOGGER = LogManager.getLogger(RSMixin.MODID);

    protected NetworkNodeFluidInterfaceMixin(World world, BlockPos pos) { super(world, pos); }

    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lcom/raoulvdberge/refinedstorage/inventory/item/ItemHandlerBase;setStackInSlot(ILnet/minecraft/item/ItemStack;)V")) private void rsmixin$emptyOneContainer(ItemHandlerBase handler, int slot, ItemStack emptied) {
        ItemStack current = handler.getStackInSlot(slot);
        if (!Config.enableFluidInterfaceStackFix || current.getCount() <= 1) {
            handler.setStackInSlot(slot, emptied);
            return;
        }

        ItemStack rest = current.copy();
        rest.shrink(1);
        handler.setStackInSlot(slot, rest);

        if (emptied == null || emptied.isEmpty()) { return; }
        ItemStack remainder = network != null ? network.insertItemTracked(emptied, emptied.getCount()) : emptied;
        if (remainder != null && !remainder.isEmpty()) {
            InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), remainder);
            if (Config.enableDebugLogging) { rsmixin$LOGGER.debug("RSMixin: Dropped an emptied fluid container at {} because the network had no room for it", pos); }
        }
    }
}
