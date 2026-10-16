package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.RSMixin;
import mctmods.rsmixin.core.interfaces.IExternalStorageFacing;

import com.raoulvdberge.refinedstorage.api.network.INetwork;
import com.raoulvdberge.refinedstorage.apiimpl.network.node.NetworkNodeExternalStorage;
import com.raoulvdberge.refinedstorage.block.BlockExternalStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = BlockExternalStorage.class, remap = false) public abstract class BlockExternalStorageMixin {
    @Redirect(method = "neighborChanged", at = @At(value = "INVOKE", target = "Lcom/raoulvdberge/refinedstorage/apiimpl/network/node/NetworkNodeExternalStorage;updateStorage(Lcom/raoulvdberge/refinedstorage/api/network/INetwork;)V", remap = false), remap = true) private void rsmixin$refreshOnFacingChange(NetworkNodeExternalStorage node, INetwork network) {
        if (Config.enableExternalStorageRefreshFilter && ((IExternalStorageFacing) node).rsmixin$facingUnchanged()) {
            if (Config.enableDebugLogging) { RSMixin.LOGGER.debug("RSMixin: Skipped external storage refresh at {} after a neighbor update (faced block unchanged)", node.getPos()); }
            return;
        }
        if (Config.enableDebugLogging) { RSMixin.LOGGER.debug("RSMixin: Refreshed external storage at {} after a neighbor update", node.getPos()); }
        node.updateStorage(network);
    }
}
