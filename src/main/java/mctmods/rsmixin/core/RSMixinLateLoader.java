package mctmods.rsmixin.core;

import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused") public class RSMixinLateLoader implements ILateMixinLoader {
    @Override public List<String> getMixinConfigs() { return Collections.singletonList("mixins.rsmixin.json"); }
}
