package mctmods.rsmixin.mixin.client.refinedstorage;

import mctmods.rsmixin.Config;

import com.refinedmods.refinedstorage.item.CoverItem;
import com.refinedmods.refinedstorage.render.model.baked.CableCoverItemBakedModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CableCoverItemBakedModel.class, remap = false) public abstract class CableCoverItemBakedModelMixin {
    @Shadow @Final private ItemStack stack;

    @Inject(method = "getParticleIcon()Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;", at = @At("HEAD"), cancellable = true, remap = true) private void rsmixin$nonNullParticleIcon(CallbackInfoReturnable<TextureAtlasSprite> cir) {
        if (!Config.ENABLE_COVER_PARTICLE_FIX.get()) { return; }
        if (!stack.isEmpty()) {
            ItemStack underlying = CoverItem.getItem(stack);
            if (!underlying.isEmpty() && underlying.getItem() instanceof BlockItem blockItem) {
                cir.setReturnValue(Minecraft.getInstance().getBlockRenderer().getBlockModel(blockItem.getBlock().defaultBlockState()).getParticleIcon(ModelData.EMPTY));
                return;
            }
        }
        cir.setReturnValue(Minecraft.getInstance().getModelManager().getMissingModel().getParticleIcon(ModelData.EMPTY));
    }
}
