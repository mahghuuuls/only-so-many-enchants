package com.mahghuuuls.onlysomanyenchants.diag;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagnosticLoggerTest {

    @Test
    void emitsNothingWhenDiagnosticModeIsOff() {
        AtomicBoolean enabled = new AtomicBoolean(false);
        AtomicLong clock = new AtomicLong(1000L);
        List<String> lines = new ArrayList<String>();
        DiagnosticLogger logger = new DiagnosticLogger(enabled::get, clock::get, lines::add, 2000L);

        logger.logTrim(
                "loot_chest",
                "minecraft:diamond_sword",
                1,
                2,
                Arrays.asList("minecraft:sharpness@4", "minecraft:looting@2"),
                1,
                Collections.singletonList("minecraft:sharpness@4"),
                Collections.singletonList("minecraft:looting@2")
        );
        logger.logRejection(
                "anvil",
                "minecraft:diamond_sword",
                1,
                2,
                Arrays.asList("minecraft:sharpness@4", "minecraft:looting@2"),
                "cap_exceeded"
        );

        assertTrue(lines.isEmpty());
    }

    @Test
    void rateLimitsIdenticalRepeatedEventsAndAllowsChangedOrExpiredEvents() {
        AtomicBoolean enabled = new AtomicBoolean(true);
        AtomicLong clock = new AtomicLong(10000L);
        List<String> lines = new ArrayList<String>();
        DiagnosticLogger logger = new DiagnosticLogger(enabled::get, clock::get, lines::add, 2000L);

        List<String> attempted = Arrays.asList("minecraft:sharpness@4", "minecraft:looting@2");
        logger.logRejection("anvil", "minecraft:diamond_sword", 1, 2, attempted, "cap_exceeded");
        clock.addAndGet(500L);
        logger.logRejection("anvil", "minecraft:diamond_sword", 1, 2, attempted, "cap_exceeded");
        assertEquals(1, lines.size());

        // Different event emits immediately.
        logger.logRejection("anvil", "minecraft:diamond_pickaxe", 1, 2, attempted, "cap_exceeded");
        assertEquals(2, lines.size());

        // After rate-limit window expires, original event may emit again.
        clock.addAndGet(2000L);
        logger.logRejection("anvil", "minecraft:diamond_sword", 1, 2, attempted, "cap_exceeded");
        assertEquals(3, lines.size());
        assertTrue(lines.get(0).contains("[OSME][REJECT]"));
        assertTrue(lines.get(0).contains("cap=1"));
        assertTrue(lines.get(0).contains("attempted=2"));
    }
}
