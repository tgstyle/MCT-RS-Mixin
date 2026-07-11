package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.helper.refinedstorage.DamageAwareCrafting;

import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.storage.disk.IStorageDisk;
import com.refinedmods.refinedstorage.api.util.Action;
import com.refinedmods.refinedstorage.api.util.IComparer;
import com.refinedmods.refinedstorage.api.util.IStackList;
import com.refinedmods.refinedstorage.api.util.StackListEntry;
import com.refinedmods.refinedstorage.apiimpl.autocrafting.task.v6.IoUtil;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.ArrayList;
import java.util.List;

@Mixin(value = IoUtil.class, remap = false) public abstract class IoUtilMixin {
    @Inject(method = "extractFromInternalItemStorage", at = @At("HEAD"), cancellable = true) private static void rsmixin$damageAwareInternalExtract(List<ItemStack> list, IStorageDisk<ItemStack> storage, Action action, CallbackInfoReturnable<List<ItemStack>> cir) {
        if (!Config.ENABLE_DAMAGEABLE_INPUT_REUSE.get()) { return; }
        if (list.stream().noneMatch(ItemStack::isDamageableItem)) { return; }

        List<ItemStack> extracted = new ArrayList<>();
        for (ItemStack stack : list) {
            if (!stack.isDamageableItem()) {
                ItemStack result = storage.extract(stack, stack.getCount(), IComparer.COMPARE_NBT, action);
                if (result.isEmpty() || result.getCount() != stack.getCount()) {
                    if (action == Action.PERFORM) { throw new IllegalStateException("The internal crafting inventory reported that " + stack + " was available but we got " + result); }
                    cir.setReturnValue(null);
                    return;
                }
                extracted.add(result);
                continue;
            }

            int remaining = stack.getCount();
            List<ItemStack> candidates = DamageAwareCrafting.collectVariants(new ArrayList<>(storage.getStacks()), stack);
            for (ItemStack candidate : candidates) {
                if (remaining <= 0) { break; }
                ItemStack result = storage.extract(candidate, Math.min(remaining, candidate.getCount()), IComparer.COMPARE_NBT, action);
                if (result.isEmpty()) { continue; }
                remaining -= result.getCount();
                extracted.add(result);
            }
            if (remaining > 0) {
                if (action == Action.PERFORM) { throw new IllegalStateException("The internal crafting inventory reported that " + stack + " was available but " + remaining + " were missing"); }
                cir.setReturnValue(null);
                return;
            }
        }
        cir.setReturnValue(extracted);
    }

    @Inject(method = "extractItemsFromNetwork", at = @At("HEAD"), cancellable = true) private static void rsmixin$damageAwareNetworkExtract(IStackList<ItemStack> toExtractInitial, INetwork network, IStorageDisk<ItemStack> internalStorage, CallbackInfo ci) {
        if (!Config.ENABLE_DAMAGEABLE_INPUT_REUSE.get()) { return; }
        if (toExtractInitial.isEmpty()) { return; }

        boolean anyDamageable = false;
        for (StackListEntry<ItemStack> entry : toExtractInitial.getStacks()) {
            if (entry.getStack().isDamageableItem()) {
                anyDamageable = true;
                break;
            }
        }
        if (!anyDamageable) { return; }

        List<ItemStack> toRemove = new ArrayList<>();
        for (StackListEntry<ItemStack> toExtract : new ArrayList<>(toExtractInitial.getStacks())) {
            ItemStack wanted = toExtract.getStack();
            int remaining = wanted.getCount();
            int totalExtracted = 0;

            if (wanted.isDamageableItem()) {
                List<ItemStack> candidates = DamageAwareCrafting.collectVariantsFromList(network.getItemStorageCache().getList(), wanted);
                for (ItemStack candidate : candidates) {
                    if (remaining <= 0) { break; }
                    ItemStack result = network.extractItem(candidate, Math.min(remaining, candidate.getCount()), Action.PERFORM);
                    if (result.isEmpty()) { continue; }
                    internalStorage.insert(result, result.getCount(), Action.PERFORM);
                    remaining -= result.getCount();
                    totalExtracted += result.getCount();
                }
            }
            if (remaining > 0) {
                ItemStack result = network.extractItem(wanted, remaining, Action.PERFORM);
                if (!result.isEmpty()) {
                    internalStorage.insert(wanted, result.getCount(), Action.PERFORM);
                    totalExtracted += result.getCount();
                }
            }
            if (totalExtracted > 0) {
                ItemStack removal = wanted.copy();
                removal.setCount(totalExtracted);
                toRemove.add(removal);
            }
        }

        for (ItemStack stack : toRemove) { toExtractInitial.remove(stack); }
        if (!toRemove.isEmpty()) { network.getCraftingManager().onTaskChanged(); }
        ci.cancel();
    }
}
