package mctmods.rsmixin.mixin.common.refinedstorage;

import com.refinedmods.refinedstorage.network.grid.GridTransferMessage;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.List;

@Mixin(value = GridTransferMessage.class, remap = false) public interface GridTransferMessageAccessor {
    @Accessor("inputs") List<List<ItemStack>> rsmixin$getInputs();
}
