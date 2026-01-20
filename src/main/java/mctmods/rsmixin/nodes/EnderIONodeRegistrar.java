package mctmods.rsmixin.nodes;

import com.refinedmods.refinedstorage.api.network.node.INetworkNodeFactory;
import com.refinedmods.refinedstorage.api.network.node.INetworkNodeRegistry;
import com.refinedmods.refinedstorage.apiimpl.API;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.RSMixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = RSMixin.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EnderIONodeRegistrar {
    private static final ResourceLocation RS_CONDUIT_ID = ResourceLocation.fromNamespaceAndPath("enderio", "rs_conduit");

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            if (!ModList.get().isLoaded("enderio") || !Config.ENABLE_ENDERIO_RS_FIX.get()) {
                return;
            }

            INetworkNodeRegistry registry = API.instance().getNetworkNodeRegistry();

            INetworkNodeFactory factory = (tag, level, pos) -> {
                EnderIORsConduitNode node = new EnderIORsConduitNode(level, pos);
                node.readConfiguration(tag);
                return node;
            };

            registry.add(RS_CONDUIT_ID, factory);
        });
    }
}
