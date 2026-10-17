package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.RSMixin;

import com.raoulvdberge.refinedstorage.network.MessageGridTransfer;
import com.raoulvdberge.refinedstorage.util.StackUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import mezz.jei.api.gui.IGuiIngredient;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(value = MessageGridTransfer.class, remap = false) public abstract class MessageGridTransferMixin {
    @Shadow private Map<Integer, ? extends IGuiIngredient<ItemStack>> inputs;
    @Shadow private List<Slot> slots;
    @Shadow private ItemStack[][] recipe;
    @Unique private static final int rsmixin$MAX_VARIANTS = 16;
    @Unique private static final int rsmixin$MAX_BYTES = 28000;
    @Unique private static final Logger rsmixin$LOGGER = LogManager.getLogger(RSMixin.MODID);

    @Inject(method = "toBytes", at = @At("HEAD"), cancellable = true) private void rsmixin$trimmedToBytes(ByteBuf buf, CallbackInfo ci) {
        if (!Config.enableJeiTransferLimit) { return; }

        List<List<ByteBuf>> encoded = new ArrayList<>();
        for (Slot slot : slots) { encoded.add(rsmixin$encode(inputs.get(slot.getSlotIndex() + 1))); }

        int budget = rsmixin$MAX_BYTES - buf.writerIndex() - 4 - 4 * encoded.size();
        int[] taken = new int[encoded.size()];
        for (int pass = 0; pass < 2; ++pass) {
            for (int i = 0; i < encoded.size(); ++i) {
                List<ByteBuf> variants = encoded.get(i);
                int limit = pass == 0 ? Math.min(1, variants.size()) : variants.size();
                while (taken[i] < limit && variants.get(taken[i]).readableBytes() <= budget) {
                    budget -= variants.get(taken[i]).readableBytes();
                    taken[i]++;
                }
            }
        }

        buf.writeInt(encoded.size());
        for (int i = 0; i < encoded.size(); ++i) {
            buf.writeInt(taken[i]);
            for (int j = 0; j < taken[i]; ++j) { buf.writeBytes(encoded.get(i).get(j)); }
        }

        ci.cancel();
    }

    @Unique private static List<ByteBuf> rsmixin$encode(IGuiIngredient<ItemStack> ingredient) {
        List<ByteBuf> encoded = new ArrayList<>();
        if (ingredient == null) { return encoded; }

        List<ItemStack> ingredients = new ArrayList<>();
        ItemStack displayed = ingredient.getDisplayedIngredient();
        if (displayed != null && !displayed.isEmpty()) { ingredients.add(displayed); }
        for (ItemStack possibleStack : ingredient.getAllIngredients()) {
            if (ingredients.size() >= rsmixin$MAX_VARIANTS) { break; }
            if (possibleStack == null || possibleStack.isEmpty()) { continue; }
            if (displayed != null && ItemStack.areItemStacksEqual(displayed, possibleStack)) { continue; }
            ingredients.add(possibleStack);
        }

        for (ItemStack possibleStack : ingredients) {
            ByteBuf stackBuf = Unpooled.buffer();
            StackUtils.writeItemStack(stackBuf, possibleStack);
            encoded.add(stackBuf);
        }
        return encoded;
    }

    @Inject(method = "fromBytes", at = @At("HEAD"), cancellable = true) private void rsmixin$boundedFromBytes(ByteBuf buf, CallbackInfo ci) {
        if (!Config.enableJeiTransferLimit) { return; }

        int slotCount = buf.readInt();
        if (slotCount < 0 || slotCount > recipe.length) {
            rsmixin$LOGGER.warn("RSMixin: Ignoring a JEI transfer packet with an invalid slot count of {}", slotCount);
            ci.cancel();
            return;
        }

        for (int i = 0; i < slotCount; ++i) {
            int ingredients = buf.readInt();
            if (ingredients < 0 || ingredients > 3000) {
                rsmixin$LOGGER.warn("RSMixin: Ignoring a JEI transfer packet with an invalid ingredient count of {}", ingredients);
                ci.cancel();
                return;
            }

            recipe[i] = new ItemStack[ingredients];
            for (int j = 0; j < ingredients; ++j) { recipe[i][j] = StackUtils.readItemStack(buf); }
        }

        ci.cancel();
    }
}
