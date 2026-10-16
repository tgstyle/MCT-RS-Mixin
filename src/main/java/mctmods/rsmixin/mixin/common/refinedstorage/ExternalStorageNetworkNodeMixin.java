package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.core.accessor.IExternalStorageFacingAccessor;

import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.storage.StorageType;
import com.refinedmods.refinedstorage.api.storage.cache.InvalidateCause;
import com.refinedmods.refinedstorage.api.storage.externalstorage.IExternalStorageProvider;
import com.refinedmods.refinedstorage.apiimpl.API;
import com.refinedmods.refinedstorage.apiimpl.network.node.ExternalStorageNetworkNode;
import com.refinedmods.refinedstorage.blockentity.config.IType;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ExternalStorageNetworkNode.class, remap = false) public abstract class ExternalStorageNetworkNodeMixin implements IExternalStorageFacingAccessor {
    @Unique private BlockEntity rsmixin$lastFacing;
    @Unique private IExternalStorageProvider<?> rsmixin$lastProvider;

    @Inject(method = "updateStorage", at = @At("TAIL")) private void rsmixin$rememberFacing(INetwork network, InvalidateCause cause, CallbackInfo ci) {
        rsmixin$lastFacing = rsmixin$self().getFacingBlockEntity();
        rsmixin$lastProvider = rsmixin$providerFor(rsmixin$lastFacing);
    }

    @Override public boolean rsmixin$facingUnchanged() {
        BlockEntity facing = rsmixin$self().getFacingBlockEntity();
        return facing == rsmixin$lastFacing && rsmixin$providerFor(facing) == rsmixin$lastProvider;
    }

    @Unique private IExternalStorageProvider<?> rsmixin$providerFor(BlockEntity facing) {
        if (facing == null) { return null; }
        ExternalStorageNetworkNode self = rsmixin$self();
        StorageType storageType = self.getType() == IType.FLUIDS ? StorageType.FLUID : StorageType.ITEM;
        for (IExternalStorageProvider<?> provider : API.instance().getExternalStorageProviders(storageType)) {
            if (provider.canProvide(facing, self.getDirection())) { return provider; }
        }
        return null;
    }

    @Unique private ExternalStorageNetworkNode rsmixin$self() { return (ExternalStorageNetworkNode) (Object) this; }
}
