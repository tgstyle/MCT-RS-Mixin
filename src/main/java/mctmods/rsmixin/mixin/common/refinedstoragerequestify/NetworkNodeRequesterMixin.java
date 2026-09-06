package mctmods.rsmixin.mixin.common.refinedstoragerequestify;

import mctmods.rsmixin.helper.refinedstorage.FastNodeTicker;

import com.buuz135.refinedstoragerequestify.proxy.block.network.NetworkNodeRequester;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NetworkNodeRequester.class, remap = false) public abstract class NetworkNodeRequesterMixin {
    @Unique private boolean rsmixin$forcedActive = false;

    @Inject(method = "update", at = @At("HEAD")) private void rsmixin$ensureFastTick(CallbackInfo ci) {
        NetworkNodeRequester thiz = (NetworkNodeRequester) (Object) this;
        rsmixin$forcedActive = FastNodeTicker.assertForced(thiz, rsmixin$forcedActive, "Requestify requester");
    }
}
