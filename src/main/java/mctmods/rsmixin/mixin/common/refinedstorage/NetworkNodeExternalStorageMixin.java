package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.core.interfaces.IExternalStorageFacing;

import com.raoulvdberge.refinedstorage.api.network.INetwork;
import com.raoulvdberge.refinedstorage.api.storage.StorageType;
import com.raoulvdberge.refinedstorage.api.storage.externalstorage.IExternalStorageProvider;
import com.raoulvdberge.refinedstorage.apiimpl.API;
import com.raoulvdberge.refinedstorage.apiimpl.network.node.NetworkNodeExternalStorage;
import com.raoulvdberge.refinedstorage.tile.config.IType;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NetworkNodeExternalStorage.class, remap = false) public abstract class NetworkNodeExternalStorageMixin implements IExternalStorageFacing {
    @Unique private TileEntity rsmixin$lastFacing;
    @Unique private IExternalStorageProvider<?> rsmixin$lastProvider;

    @Inject(method = "updateStorage", at = @At("TAIL")) private void rsmixin$rememberFacing(INetwork network, CallbackInfo ci) {
        rsmixin$lastFacing = rsmixin$self().getFacingTile();
        rsmixin$lastProvider = rsmixin$providerFor(rsmixin$lastFacing);
    }

    @Override public boolean rsmixin$facingUnchanged() {
        TileEntity facing = rsmixin$self().getFacingTile();
        return facing == rsmixin$lastFacing && rsmixin$providerFor(facing) == rsmixin$lastProvider;
    }

    @Unique private IExternalStorageProvider<?> rsmixin$providerFor(TileEntity facing) {
        if (facing == null) { return null; }
        NetworkNodeExternalStorage self = rsmixin$self();
        StorageType storageType = self.getType() == IType.FLUIDS ? StorageType.FLUID : StorageType.ITEM;
        for (IExternalStorageProvider<?> provider : API.instance().getExternalStorageProviders(storageType)) {
            if (provider.canProvide(facing, self.getDirection())) { return provider; }
        }
        return null;
    }

    @Unique private NetworkNodeExternalStorage rsmixin$self() { return (NetworkNodeExternalStorage) (Object) this; }
}
