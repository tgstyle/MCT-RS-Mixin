package mctmods.rsmixin.mixin.enderio;

import com.enderio.api.conduit.ConduitType;
import com.enderio.conduits.common.conduit.ConduitBundle;
import com.enderio.conduits.common.conduit.block.ConduitBlockEntity;

import mctmods.rsmixin.Config;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;
import java.util.List;

import static mctmods.rsmixin.RSMixin.MODID;

@Mixin(ConduitBlockEntity.class)
public abstract class ConduitBlockEntityTypedBackupMixin {
    @Unique private static final Logger rsmixin$LOGGER = LogManager.getLogger(MODID);

    @Shadow @Final private ConduitBundle bundle;

    @Unique
    private CompoundTag rsmixin$typedExtra;

    @Unique
    private static Method rsmixin$getTypesMethod;

    @Unique
    private static Method rsmixin$getNodeForMethod;

    @Unique
    private static void rsmixin$initMethods() {
        if (rsmixin$getTypesMethod == null) {
            try {
                rsmixin$getTypesMethod = ConduitBundle.class.getMethod("getTypes");
                rsmixin$getNodeForMethod = ConduitBundle.class.getMethod("getNodeFor", ConduitType.class);
            } catch (NoSuchMethodException ignored) {}
        }
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void rsmixin$saveTypedExtra(CompoundTag tag, CallbackInfo ci) {
        if (!Config.ENABLE_ENDERIO_CONDUIT_TYPED_BACKUP.get() || !ModList.get().isLoaded("enderio")) return;
        if (bundle == null) return;

        try {
            rsmixin$initMethods();
            if (rsmixin$getTypesMethod == null || rsmixin$getNodeForMethod == null) return;

            @SuppressWarnings("unchecked")
            List<ConduitType<?>> types = (List<ConduitType<?>>) rsmixin$getTypesMethod.invoke(bundle);
            if (types.isEmpty()) return;

            CompoundTag typed = new CompoundTag();
            int count = 0;

            for (ConduitType<?> type : types) {
                ResourceLocation rl = ConduitType.getKey(type);
                if (rl == null) continue;

                Object node = rsmixin$getNodeForMethod.invoke(bundle, type);
                if (node == null) continue;

                Method dataMethod = node.getClass().getMethod("getConduitData");
                Object dataObj = dataMethod.invoke(node);
                if (dataObj == null) continue;

                Method serializeMethod = dataObj.getClass().getMethod("serializeNBT");
                CompoundTag dataTag = (CompoundTag) serializeMethod.invoke(dataObj);

                if (!dataTag.isEmpty()) count++;
                typed.put(rl.toString(), dataTag);
            }

            if (!typed.isEmpty()) {
                tag.put("rsmixin:conduit_typed_extra", typed);
                if (Config.ENABLE_DEBUG_LOGGING.get()) {
                    rsmixin$LOGGER.info("Saved typed conduit backup for {} types at {}", count, bundle);
                }
            }
        } catch (Exception ignored) {}
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void rsmixin$loadTypedExtra(CompoundTag tag, CallbackInfo ci) {
        if (!Config.ENABLE_ENDERIO_CONDUIT_TYPED_BACKUP.get() || !ModList.get().isLoaded("enderio")) {
            rsmixin$typedExtra = null;
            return;
        }
        rsmixin$typedExtra = tag.contains("rsmixin:conduit_typed_extra")
                ? tag.getCompound("rsmixin:conduit_typed_extra")
                : null;

        if (Config.ENABLE_DEBUG_LOGGING.get() && rsmixin$typedExtra != null) {
            rsmixin$LOGGER.info("Loaded typed conduit backup with {} entries", rsmixin$typedExtra.size());
        }
    }

    @Inject(method = "setLevel", at = @At("TAIL"))
    private void rsmixin$applyTypedOverride(CallbackInfo ci) {
        if (!Config.ENABLE_ENDERIO_CONDUIT_TYPED_BACKUP.get() || !ModList.get().isLoaded("enderio")) return;
        if (rsmixin$typedExtra == null || bundle == null) return;

        try {
            rsmixin$initMethods();
            if (rsmixin$getTypesMethod == null || rsmixin$getNodeForMethod == null) return;

            @SuppressWarnings("unchecked")
            List<ConduitType<?>> types = (List<ConduitType<?>>) rsmixin$getTypesMethod.invoke(bundle);
            if (types.isEmpty()) return;

            int applied = 0;

            for (ConduitType<?> type : types) {
                ResourceLocation rl = ConduitType.getKey(type);
                if (rl == null) continue;

                String key = rl.toString();
                if (!rsmixin$typedExtra.contains(key)) continue;

                Object node = rsmixin$getNodeForMethod.invoke(bundle, type);
                if (node == null) continue;

                Method dataMethod = node.getClass().getMethod("getConduitData");
                Object dataObj = dataMethod.invoke(node);
                if (dataObj == null) continue;

                Method deserializeMethod = dataObj.getClass().getMethod("deserializeNBT", CompoundTag.class);
                deserializeMethod.invoke(dataObj, rsmixin$typedExtra.getCompound(key));
                applied++;
            }

            if (Config.ENABLE_DEBUG_LOGGING.get() && applied > 0) {
                rsmixin$LOGGER .info("Applied typed conduit backup to {} types", applied);
            }
        } catch (Exception ignored) {}
    }
}
