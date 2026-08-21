package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;

import com.refinedmods.refinedstorage.network.PacketSplitter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = PacketSplitter.class, remap = false) public abstract class PacketSplitterMixin {
    @ModifyVariable(method = "registerMessage(IILjava/lang/Class;Ljava/util/function/BiConsumer;Ljava/util/function/Function;Ljava/util/function/BiConsumer;)V", at = @At("HEAD"), ordinal = 1, argsOnly = true) private int rsmixin$raisePacketLimit(int maxNumberOfMessages) {
        if (!Config.SPEC.isLoaded() || !Config.ENABLE_RAISED_PACKET_LIMIT.get()) { return maxNumberOfMessages; }
        return Math.max(maxNumberOfMessages, Config.MAX_SPLIT_PACKETS.get());
    }
}
