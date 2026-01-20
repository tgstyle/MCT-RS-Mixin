package mctmods.rsmixin.nodes;

import com.refinedmods.refinedstorage.RS;
import com.refinedmods.refinedstorage.apiimpl.network.node.NetworkNode;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class EnderIORsConduitNode extends NetworkNode {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("enderio", "rs_conduit");

    public EnderIORsConduitNode(Level level, BlockPos pos) {
        super(level, pos);
    }

    @Override
    public int getEnergyUsage() {
        return RS.SERVER_CONFIG.getCable().getUsage();
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public @NotNull ItemStack getItemStack() {
        return new ItemStack(Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("enderio", "refined_storage_conduit"))));
    }
}
