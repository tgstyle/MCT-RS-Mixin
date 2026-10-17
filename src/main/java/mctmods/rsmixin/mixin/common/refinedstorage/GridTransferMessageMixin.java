package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;

import com.refinedmods.refinedstorage.network.grid.GridTransferMessage;
import com.refinedmods.refinedstorage.util.StackUtils;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.ArrayList;
import java.util.List;

@Mixin(value = GridTransferMessage.class, remap = false) public abstract class GridTransferMessageMixin {
    @Unique private static final int rsmixin$MAX_BYTES = 28000;

    @Inject(method = "encode", at = @At("HEAD"), cancellable = true) private static void rsmixin$encodeWithinBudget(GridTransferMessage message, FriendlyByteBuf buf, CallbackInfo ci) {
        if (!Config.ENABLE_JEI_TRANSFER_LIMIT.get()) { return; }
        List<List<ItemStack>> inputs = ((GridTransferMessageAccessor) message).rsmixin$getInputs();
        List<List<byte[]>> encoded = new ArrayList<>(inputs.size());
        for (List<ItemStack> possibilities : inputs) {
            List<byte[]> variants = new ArrayList<>(possibilities.size());
            for (ItemStack possibility : possibilities) { variants.add(rsmixin$encode(possibility)); }
            encoded.add(variants);
        }
        int budget = rsmixin$MAX_BYTES - buf.writerIndex() - Integer.BYTES * (encoded.size() + 1);
        int[] kept = new int[encoded.size()];
        for (int slot = 0; slot < encoded.size(); slot++) {
            List<byte[]> variants = encoded.get(slot);
            if (!variants.isEmpty() && variants.get(0).length <= budget) {
                budget -= variants.get(0).length;
                kept[slot] = 1;
            }
        }
        for (int slot = 0; slot < encoded.size(); slot++) {
            List<byte[]> variants = encoded.get(slot);
            while (kept[slot] > 0 && kept[slot] < variants.size() && variants.get(kept[slot]).length <= budget) {
                budget -= variants.get(kept[slot]).length;
                kept[slot]++;
            }
        }
        buf.writeInt(encoded.size());
        for (int slot = 0; slot < encoded.size(); slot++) {
            buf.writeInt(kept[slot]);
            for (int variant = 0; variant < kept[slot]; variant++) { buf.writeBytes(encoded.get(slot).get(variant)); }
        }
        ci.cancel();
    }

    @Unique private static byte[] rsmixin$encode(ItemStack stack) {
        FriendlyByteBuf scratch = new FriendlyByteBuf(Unpooled.buffer());
        StackUtils.writeItemStack(scratch, stack);
        byte[] bytes = new byte[scratch.readableBytes()];
        scratch.readBytes(bytes);
        scratch.release();
        return bytes;
    }
}
