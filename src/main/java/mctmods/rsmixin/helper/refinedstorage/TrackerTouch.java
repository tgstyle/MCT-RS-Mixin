package mctmods.rsmixin.helper.refinedstorage;

import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.storage.tracker.IStorageTracker;
import com.refinedmods.refinedstorage.api.storage.tracker.StorageTrackerEntry;
import com.refinedmods.refinedstorage.apiimpl.API;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;

import java.util.function.Function;

public final class TrackerTouch {
    private static final long REFRESH_MILLIS = 1000;
    private static final String NBT_STACK = "Stack";
    private static final String NBT_NAME = "Name";
    private static final String NBT_TIME = "Time";

    private TrackerTouch() {}

    public static <T> void touch(INetwork network, IStorageTracker<T> tracker, T stack, Function<T, CompoundTag> writer) {
        if (!(network.getLevel() instanceof ServerLevel level)) { return; }
        long now = System.currentTimeMillis();
        StorageTrackerEntry entry = tracker.get(stack);
        if (entry != null) {
            long age = now - entry.getTime();
            if (age >= 0 && age < REFRESH_MILLIS) { return; }
        }
        CompoundTag tag = new CompoundTag();
        tag.putLong(NBT_TIME, now);
        tag.putString(NBT_NAME, entry != null ? entry.getName() : "");
        tag.put(NBT_STACK, writer.apply(stack));
        ListTag list = new ListTag();
        list.add(tag);
        tracker.readFromNbt(list);
        API.instance().getStorageTrackerManager(level).markForSaving();
    }
}
