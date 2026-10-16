package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.core.interfaces.IItemTrackerTouch;
import mctmods.rsmixin.helper.refinedstorage.TrackerTouch;

import com.raoulvdberge.refinedstorage.api.storage.IStorageTracker.IStorageTrackerEntry;
import com.raoulvdberge.refinedstorage.apiimpl.storage.StorageTrackerItem;
import com.raoulvdberge.refinedstorage.util.StackUtils;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

@Mixin(value = StorageTrackerItem.class, remap = false) public abstract class StorageTrackerItemMixin implements IItemTrackerTouch {
    @Unique private static final String rsmixin$NBT_ID = "Id";
    @Unique private static final String rsmixin$NBT_TYPE = "Type";
    @Shadow private Map<ItemStack, IStorageTrackerEntry> changes;
    @Shadow private Runnable listener;

    @Override public void rsmixin$touchItem(ItemStack stack) { TrackerTouch.touch(changes, listener, stack, ItemStack::copy); }

    @Redirect(method = "serializeNbt", at = @At(value = "INVOKE", target = "Lcom/raoulvdberge/refinedstorage/util/StackUtils;serializeStackToNbt(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/nbt/NBTTagCompound;")) private NBTTagCompound rsmixin$writeRegistryName(ItemStack stack) {
        NBTTagCompound tag = StackUtils.serializeStackToNbt(stack);
        ResourceLocation id = stack.getItem().getRegistryName();
        if (Config.enableLastModifiedTrackerFix && id != null) { tag.setString(rsmixin$NBT_ID, id.toString()); }
        return tag;
    }

    @Redirect(method = "readFromNbt", at = @At(value = "INVOKE", target = "Lcom/raoulvdberge/refinedstorage/util/StackUtils;deserializeStackFromNbt(Lnet/minecraft/nbt/NBTTagCompound;)Lnet/minecraft/item/ItemStack;")) private ItemStack rsmixin$readRegistryName(NBTTagCompound tag) {
        if (!Config.enableLastModifiedTrackerFix || !tag.hasKey(rsmixin$NBT_ID, Constants.NBT.TAG_STRING)) { return StackUtils.deserializeStackFromNbt(tag); }
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(tag.getString(rsmixin$NBT_ID)));
        if (item == null) { return ItemStack.EMPTY; }
        NBTTagCompound remapped = tag.copy();
        remapped.setInteger(rsmixin$NBT_TYPE, Item.getIdFromItem(item));
        return StackUtils.deserializeStackFromNbt(remapped);
    }
}
