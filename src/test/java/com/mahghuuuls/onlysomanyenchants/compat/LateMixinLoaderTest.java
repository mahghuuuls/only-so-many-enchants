package com.mahghuuuls.onlysomanyenchants.compat;

import org.junit.jupiter.api.Test;
import zone.rong.mixinbooter.Context;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LateMixinLoaderTest {

    @Test
    void returnsEmptyConfigListWhenOptionalModsAreAbsent() {
        LateMixinLoader loader = new LateMixinLoader(modId -> false);
        assertTrue(loader.getMixinConfigs().isEmpty());

        Context absentEasyMagic = new Context(LateMixinLoader.EASY_MAGIC_MIXIN_CONFIG, Collections.<String>emptyList());
        assertFalse(loader.shouldMixinConfigQueue(absentEasyMagic));

        Context unknownConfig = new Context("mixins.onlysomanyenchants.unknown.json", Collections.singletonList(LateMixinLoader.EASY_MAGIC_MOD_ID));
        assertFalse(loader.shouldMixinConfigQueue(unknownConfig));
        assertFalse(loader.shouldMixinConfigQueue((Context) null));
    }

    @Test
    void queuesEasyMagicConfigOnlyWhenEasyMagicIsLoaded() {
        Set<String> loaded = new HashSet<String>(Collections.singletonList(LateMixinLoader.EASY_MAGIC_MOD_ID));
        LateMixinLoader loader = new LateMixinLoader(loaded::contains);

        List<String> configs = loader.getMixinConfigs();
        assertEquals(Collections.singletonList(LateMixinLoader.EASY_MAGIC_MIXIN_CONFIG), configs);

        Context presentEasyMagic = new Context(LateMixinLoader.EASY_MAGIC_MIXIN_CONFIG, loaded);
        assertTrue(loader.shouldMixinConfigQueue(presentEasyMagic));

        Context unknownConfig = new Context("mixins.onlysomanyenchants.unknown.json", loaded);
        assertFalse(loader.shouldMixinConfigQueue(unknownConfig));
    }
}
