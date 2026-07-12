package mctmods.rsmixin.helper.refinedstorage;

import mctmods.rsmixin.RSMixin;

import java.lang.reflect.Field;

public class ProcessingStateAccess {
    private static Field stateField;
    private static Object extractedAllConstant;
    private static Object processedConstant;
    private static boolean initialized;
    private static boolean available;

    private static void init() {
        initialized = true;
        try {
            Class<?> processingClass = Class.forName("com.raoulvdberge.refinedstorage.apiimpl.autocrafting.task.Processing");
            stateField = processingClass.getDeclaredField("state");
            stateField.setAccessible(true);
            Class<?> stateType = stateField.getType();
            for (Object constant : stateType.getEnumConstants()) {
                if ("EXTRACTED_ALL".equals(((Enum<?>) constant).name())) { extractedAllConstant = constant; }
                if ("PROCESSED".equals(((Enum<?>) constant).name())) { processedConstant = constant; }
            }
            available = extractedAllConstant != null && processedConstant != null;
        } catch (Exception e) {
            available = false;
            RSMixin.LOGGER.error("RSMixin: Failed to access Processing.state, tracked insert index disabled", e);
        }
    }

    public static boolean isUnAvailable() {
        if (!initialized) { init(); }
        return !available;
    }

    public static boolean isExtractedAll(Object processing) {
        if (isUnAvailable()) { return false; }
        try { return stateField.get(processing) == extractedAllConstant; }
        catch (IllegalAccessException e) { return false; }
    }

    public static void setProcessed(Object processing) {
        if (isUnAvailable()) { return; }
        try { stateField.set(processing, processedConstant); }
        catch (IllegalAccessException ignored) {}
    }
}
