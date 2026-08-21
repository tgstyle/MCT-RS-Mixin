package mctmods.rsmixin.helper.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.RSMixin;

import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class SaveDataRecovery {
    @SubscribeEvent public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        if (!Config.ENABLE_SAFE_DATA_SAVING.get()) { return; }
        Path dataDir = event.getServer().getWorldPath(LevelResource.ROOT).resolve("data");
        if (!Files.isDirectory(dataDir)) { return; }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dataDir, "refinedstorage_*.dat")) {
            for (Path dat : stream) {
                if (Files.size(dat) > 0) { continue; }
                Path temp = dataDir.resolve(dat.getFileName() + ".temp");
                if (Files.exists(temp) && Files.size(temp) > 0) {
                    Files.move(temp, dat, StandardCopyOption.REPLACE_EXISTING);
                    RSMixin.LOGGER.warn("RSMixin: {} was empty (leftover from a failed save); recovered it from {}", dat.getFileName(), temp.getFileName());
                }
                else {
                    Files.delete(dat);
                    RSMixin.LOGGER.warn("RSMixin: {} was empty (leftover from a failed save) and no backup existed; removed it so a fresh file can be created", dat.getFileName());
                }
            }
        }
        catch (IOException e) { RSMixin.LOGGER.error("RSMixin: Failed to check Refined Storage save data for corruption", e); }
    }
}
