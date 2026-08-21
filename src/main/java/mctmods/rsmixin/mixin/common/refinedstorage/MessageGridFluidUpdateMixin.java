package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.helper.refinedstorage.GridSyncCompression;

import com.raoulvdberge.refinedstorage.network.MessageGridFluidUpdate;
import io.netty.buffer.ByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.function.Consumer;

@Mixin(value = MessageGridFluidUpdate.class, remap = false) public abstract class MessageGridFluidUpdateMixin {
    @Shadow private Consumer<ByteBuf> sendHandler;
    @Shadow private boolean canCraft;

    @Inject(method = "toBytes", at = @At("HEAD"), cancellable = true) private void rsmixin$compressedToBytes(ByteBuf buf, CallbackInfo ci) {
        if (!Config.enableCompressedGridSync) { return; }
        buf.writeBoolean(canCraft);
        GridSyncCompression.writePayload(buf, sendHandler);
        ci.cancel();
    }

    @ModifyVariable(method = "fromBytes", at = @At("HEAD"), argsOnly = true) private ByteBuf rsmixin$inflate(ByteBuf buf) { return GridSyncCompression.inflateMessage(buf); }
}
