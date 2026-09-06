package mctmods.rsmixin.core;

import zone.rong.mixinbooter.Context;
import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused") public class RSMixinLateLoader implements ILateMixinLoader {
    private static final String REBORNSTORAGE_CONFIG = "mixins.rsmixin.rebornstorage.json";
    private static final String REQUESTIFY_CONFIG = "mixins.rsmixin.requestify.json";
    private static final Map<String, String> OPTIONAL_CONFIGS = new HashMap<>();

    static {
        OPTIONAL_CONFIGS.put(REBORNSTORAGE_CONFIG, "rebornstorage");
        OPTIONAL_CONFIGS.put(REQUESTIFY_CONFIG, "refinedstoragerequestify");
    }

    @Override public List<String> getMixinConfigs() { return Arrays.asList("mixins.rsmixin.json", REBORNSTORAGE_CONFIG, REQUESTIFY_CONFIG); }

    @Override public boolean shouldMixinConfigQueue(Context context) {
        String modid = OPTIONAL_CONFIGS.get(context.mixinConfig());
        return modid == null || context.isModPresent(modid);
    }
}
