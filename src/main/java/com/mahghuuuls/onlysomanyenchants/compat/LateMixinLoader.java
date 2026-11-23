package com.mahghuuuls.onlysomanyenchants.compat;

import net.minecraftforge.fml.common.Loader;
import zone.rong.mixinbooter.Context;
import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public final class LateMixinLoader implements ILateMixinLoader {

    public static final String EASY_MAGIC_MOD_ID = "easymagic";
    public static final String EASY_MAGIC_MIXIN_CONFIG = "mixins.onlysomanyenchants.easymagic.json";
    public static final String STORAGE_NETWORK_MOD_ID = "storagenetwork";
    public static final String STORAGE_NETWORK_MIXIN_CONFIG = "mixins.onlysomanyenchants.ssn.json";

    private final Predicate<String> modLoadedCheck;

    public LateMixinLoader() {
        this(Loader::isModLoaded);
    }

    LateMixinLoader(Predicate<String> modLoadedCheck) {
        this.modLoadedCheck = modLoadedCheck;
    }

    @Override
    public List<String> getMixinConfigs() {
        List<String> configs = new ArrayList<String>(2);
        if (isModPresent(EASY_MAGIC_MOD_ID)) {
            configs.add(EASY_MAGIC_MIXIN_CONFIG);
        }
        if (isModPresent(STORAGE_NETWORK_MOD_ID)) {
            configs.add(STORAGE_NETWORK_MIXIN_CONFIG);
        }
        return Collections.unmodifiableList(configs);
    }

    @Override
    public boolean shouldMixinConfigQueue(Context context) {
        if (context == null) {
            return false;
        }
        if (EASY_MAGIC_MIXIN_CONFIG.equals(context.mixinConfig())) {
            return context.isModPresent(EASY_MAGIC_MOD_ID);
        }
        if (STORAGE_NETWORK_MIXIN_CONFIG.equals(context.mixinConfig())) {
            return context.isModPresent(STORAGE_NETWORK_MOD_ID);
        }
        return false;
    }

    private boolean isModPresent(String modId) {
        return modLoadedCheck != null && modLoadedCheck.test(modId);
    }
}
