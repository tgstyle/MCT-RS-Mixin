package mctmods.rsmixin.mixin.common.rebornstorage;

import mctmods.rsmixin.Config;

import me.modmuss50.rebornstorage.client.gui.ContainerMultiCrafter;
import me.modmuss50.rebornstorage.multiblocks.MultiBlockCrafter;
import me.modmuss50.rebornstorage.tiles.CraftingNode;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ContainerMultiCrafter.class, remap = false) public abstract class ContainerMultiCrafterMixin {
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lme/modmuss50/rebornstorage/multiblocks/MultiBlockCrafter;getInvForPage(I)Lme/modmuss50/rebornstorage/tiles/CraftingNode$CachingItemHandler;")) private CraftingNode.CachingItemHandler rsmixin$refreshClientBeforeSlots(MultiBlockCrafter instance, int page) {
        if (Config.enableRebornstorageCrafterFix && FMLCommonHandler.instance().getEffectiveSide().isClient()) { instance.updateInfo("rsmixin client refresh"); }
        return instance.getInvForPage(page);
    }
}
