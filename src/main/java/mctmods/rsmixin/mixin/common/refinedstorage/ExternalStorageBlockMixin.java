package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.RSMixin;
import mctmods.rsmixin.core.accessor.IExternalStorageFacingAccessor;

import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.storage.cache.InvalidateCause;
import com.refinedmods.refinedstorage.apiimpl.network.node.ExternalStorageNetworkNode;
import com.refinedmods.refinedstorage.block.ExternalStorageBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ExternalStorageBlock.class, remap = false) public abstract class ExternalStorageBlockMixin {
    @Redirect(method = "neighborChanged", at = @At(value = "INVOKE", target = "Lcom/refinedmods/refinedstorage/apiimpl/network/node/ExternalStorageNetworkNode;updateStorage(Lcom/refinedmods/refinedstorage/api/network/INetwork;Lcom/refinedmods/refinedstorage/api/storage/cache/InvalidateCause;)V", remap = false), remap = true) private void rsmixin$refreshOnFacingChange(ExternalStorageNetworkNode node, INetwork network, InvalidateCause cause) {
        if (Config.ENABLE_EXTERNAL_STORAGE_REFRESH_FILTER.get() && ((IExternalStorageFacingAccessor) node).rsmixin$facingUnchanged()) {
            if (Config.ENABLE_DEBUG_LOGGING.get()) { RSMixin.LOGGER.debug("RSMixin: Skipped external storage refresh at {} after a neighbor update (faced block unchanged)", node.getPos()); }
            return;
        }
        if (Config.ENABLE_DEBUG_LOGGING.get()) { RSMixin.LOGGER.debug("RSMixin: Refreshed external storage at {} after a neighbor update", node.getPos()); }
        node.updateStorage(network, cause);
    }
}
