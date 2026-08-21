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

        buf.writeInt(slots.size());

        for (Slot slot : slots) {
            IGuiIngredient<ItemStack> ingredient = inputs.get(slot.getSlotIndex() + 1);

            List<ItemStack> ingredients = new ArrayList<>();
            if (ingredient != null) {
                ItemStack displayed = ingredient.getDisplayedIngredient();
                if (displayed != null && !displayed.isEmpty()) { ingredients.add(displayed); }
                for (ItemStack possibleStack : ingredient.getAllIngredients()) {
                    if (ingredients.size() >= rsmixin$MAX_VARIANTS) { break; }
                    if (possibleStack == null || possibleStack.isEmpty()) { continue; }
                    if (displayed != null && ItemStack.areItemStacksEqual(displayed, possibleStack)) { continue; }
                    ingredients.add(possibleStack);
                }
            }

            ByteBuf slotBuf = Unpooled.buffer();
            int written = 0;
            for (ItemStack possibleStack : ingredients) {
                int before = slotBuf.writerIndex();
                StackUtils.writeItemStack(slotBuf, possibleStack);
                if (written > 0 && buf.writerIndex() + slotBuf.writerIndex() > rsmixin$MAX_BYTES) {
                    slotBuf.writerIndex(before);
                    break;
                }
                written++;
            }

            buf.writeInt(written);
            buf.writeBytes(slotBuf);
        }

        ci.cancel();
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
