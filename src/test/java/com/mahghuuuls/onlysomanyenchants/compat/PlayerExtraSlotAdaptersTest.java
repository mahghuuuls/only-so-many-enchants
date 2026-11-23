package com.mahghuuuls.onlysomanyenchants.compat;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerExtraSlotAdaptersTest {

    @Test
    void nullPlayerAndAbsentModsAreSafeNoOps() {
        PlayerExtraSlotAdapters adapters = new PlayerExtraSlotAdapters(modId -> false);
        assertEquals(0, adapters.sanitize(null));
        assertFalse(adapters.isBaublesDisabled());
        assertFalse(adapters.isIntrinsicBackpackDisabled());
        assertFalse(adapters.isToolSlotsDisabled());
        assertFalse(adapters.isCosmeticArmorDisabled());
    }

    @Test
    void failingOneOptionalAdapterDisablesOnlyThatAdapterWithoutThrowing() {
        PlayerExtraSlotAdapters adapters = new PlayerExtraSlotAdapters(PlayerExtraSlotAdapters.MOD_BAUBLES::equals);
        assertEquals(0, adapters.sanitizeBaubles(null, new Random(1L)));
        assertTrue(adapters.isBaublesDisabled());
        assertFalse(adapters.isIntrinsicBackpackDisabled());
        assertFalse(adapters.isToolSlotsDisabled());
        assertFalse(adapters.isCosmeticArmorDisabled());
    }
}
