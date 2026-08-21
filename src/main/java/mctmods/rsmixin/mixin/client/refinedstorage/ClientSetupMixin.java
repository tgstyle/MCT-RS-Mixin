package mctmods.rsmixin.mixin.client.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.RSMixin;

import com.refinedmods.refinedstorage.RS;
import com.refinedmods.refinedstorage.render.BakedModelOverrideRegistry;
import com.refinedmods.refinedstorage.setup.ClientSetup;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ModelEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientSetup.class, remap = false) public abstract class ClientSetupMixin {
    @Shadow @Final private static BakedModelOverrideRegistry BAKED_MODEL_OVERRIDE_REGISTRY;

    @Invoker("registerBakedModelOverrides") static void rsmixin$registerBakedModelOverrides() { throw new AssertionError(); }

    @Inject(method = "onModelBake", at = @At("HEAD")) private static void rsmixin$closeModelBakeRace(ModelEvent.ModifyBakingResult e, CallbackInfo ci) {
        if (Config.SPEC.isLoaded() && !Config.ENABLE_MODEL_REGISTRATION_FIX.get()) { return; }
        if (BAKED_MODEL_OVERRIDE_REGISTRY.get(ResourceLocation.tryParse(RS.ID + ":pattern")) == null) { RSMixin.LOGGER.warn("Refined Storage model overrides were missing at model bake (RS bug #3747); registering them now"); }
        rsmixin$registerBakedModelOverrides();
    }
}
