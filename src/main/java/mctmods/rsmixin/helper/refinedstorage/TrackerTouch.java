package mctmods.rsmixin.helper.refinedstorage;

import com.raoulvdberge.refinedstorage.api.storage.IStorageTracker.IStorageTrackerEntry;
import com.raoulvdberge.refinedstorage.apiimpl.storage.StorageTrackerEntry;
import net.minecraft.server.MinecraftServer;

import java.util.Map;
import java.util.function.UnaryOperator;

public final class TrackerTouch {
    private static final long REFRESH_MILLIS = 1000;

    private TrackerTouch() {}

    public static <T> void touch(Map<T, IStorageTrackerEntry> changes, Runnable listener, T stack, UnaryOperator<T> copier) {
        long now = MinecraftServer.getCurrentTimeMillis();
        IStorageTrackerEntry entry = changes.get(stack);
        if (entry != null) {
            long age = now - entry.getTime();
            if (age >= 0 && age < REFRESH_MILLIS) { return; }
        }
        changes.put(copier.apply(stack), new StorageTrackerEntry(now, entry != null ? entry.getName() : ""));
        listener.run();
    }
}
