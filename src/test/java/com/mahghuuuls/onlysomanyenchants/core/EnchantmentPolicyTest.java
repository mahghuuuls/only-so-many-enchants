package com.mahghuuuls.onlysomanyenchants.core;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantmentPolicyTest {

    @Test
    void clampCapEnforcesMinimumOne() {
        assertEquals(1, EnchantmentPolicy.clampCap(-10));
        assertEquals(1, EnchantmentPolicy.clampCap(0));
        assertEquals(1, EnchantmentPolicy.clampCap(1));
        assertEquals(3, EnchantmentPolicy.clampCap(3));
    }

    @Test
    void selectIndicesToKeepHasUniformDistributionForCapOne() {
        int trials = 3000;
        int[] counts = new int[3];
        Random rng = new Random(123456789L);

        for (int i = 0; i < trials; i++) {
            List<Integer> kept = EnchantmentPolicy.selectIndicesToKeep(3, 1, rng);
            assertEquals(1, kept.size());
            counts[kept.get(0).intValue()]++;
        }

        for (int idx = 0; idx < 3; idx++) {
            double freq = (double) counts[idx] / trials;
            assertTrue(freq >= 0.28 && freq <= 0.38, "Index " + idx + " frequency out of bounds: " + freq);
        }
    }

    @Test
    void selectIndicesToKeepHasUniformDistributionForCapTwoOfFour() {
        int trials = 3000;
        int[] counts = new int[4];
        Random rng = new Random(987654321L);

        for (int i = 0; i < trials; i++) {
            List<Integer> kept = EnchantmentPolicy.selectIndicesToKeep(4, 2, rng);
            assertEquals(2, kept.size());
            assertTrue(kept.get(0).intValue() < kept.get(1).intValue(), "Selected indices must stay sorted");
            counts[kept.get(0).intValue()]++;
            counts[kept.get(1).intValue()]++;
        }

        for (int idx = 0; idx < 4; idx++) {
            double freq = (double) counts[idx] / trials;
            assertTrue(freq >= 0.45 && freq <= 0.55, "Index " + idx + " frequency out of bounds: " + freq);
        }
    }

    @Test
    void trimEnchantmentListIsDeterministicForEnchantingTableSeed() {
        List<String> rolled = Arrays.asList("sharpness@4", "unbreaking@3", "looting@2", "fire_aspect@2");

        List<String> firstCap1 = EnchantmentPolicy.trimEnchantmentList(
                rolled, 1, EnchantmentPolicy.createEnchantingTableRandom(424242, 2)
        );
        List<String> secondCap1 = EnchantmentPolicy.trimEnchantmentList(
                rolled, 1, EnchantmentPolicy.createEnchantingTableRandom(424242, 2)
        );
        assertEquals(1, firstCap1.size());
        assertEquals(firstCap1, secondCap1);

        List<String> firstCap2 = EnchantmentPolicy.trimEnchantmentList(
                rolled, 2, EnchantmentPolicy.createEnchantingTableRandom(424242, 2)
        );
        List<String> secondCap2 = EnchantmentPolicy.trimEnchantmentList(
                rolled, 2, EnchantmentPolicy.createEnchantingTableRandom(424242, 2)
        );
        assertEquals(2, firstCap2.size());
        assertEquals(firstCap2, secondCap2);
        assertTrue(rolled.containsAll(firstCap2));
        assertFalse(firstCap2.get(0).equals(firstCap2.get(1)), "Trimmed entries must remain distinct");

        // Changing seed or slot must produce distinct RNG streams rather than returning a fixed prefix.
        boolean seedOrSlotChangedOutput = false;
        for (int altSlot = 0; altSlot < 3 && !seedOrSlotChangedOutput; altSlot++) {
            for (int altSeed = 1; altSeed <= 20; altSeed++) {
                List<String> alt = EnchantmentPolicy.trimEnchantmentList(
                        rolled, 1, EnchantmentPolicy.createEnchantingTableRandom(424242 + altSeed, altSlot)
                );
                if (!alt.equals(firstCap1)) {
                    seedOrSlotChangedOutput = true;
                    break;
                }
            }
        }
        assertTrue(seedOrSlotChangedOutput, "Enchanting-table RNG must vary across seeds/slots");
    }

    @Test
    void trimEnchantmentListPassesThroughEmptyAndShortListsAndIsFairAcrossSeeds() {
        assertEquals(null, EnchantmentPolicy.trimEnchantmentList(null, 1, new Random(1L)));
        List<String> empty = Arrays.asList();
        assertEquals(empty, EnchantmentPolicy.trimEnchantmentList(empty, 1, new Random(1L)));
        List<String> single = Arrays.asList("sharpness@4");
        assertEquals(single, EnchantmentPolicy.trimEnchantmentList(single, 1, new Random(1L)));

        List<String> rolled = Arrays.asList("sharpness@4", "unbreaking@3", "looting@2");
        int trials = 3000;
        int[] counts = new int[3];
        for (int seed = 0; seed < trials; seed++) {
            List<String> trimmed = EnchantmentPolicy.trimEnchantmentList(
                    rolled, 1, EnchantmentPolicy.createEnchantingTableRandom(seed * 7919, seed % 3)
            );
            assertEquals(1, trimmed.size());
            counts[rolled.indexOf(trimmed.get(0))]++;
        }
        for (int idx = 0; idx < 3; idx++) {
            double freq = (double) counts[idx] / trials;
            assertTrue(freq >= 0.28 && freq <= 0.38, "Enchanting seed distribution out of bounds for idx " + idx + ": " + freq);
        }
    }

    @Test
    void evaluateAnvilOutputClassifiesWithinAndOverCap() {
        assertFalse(EnchantmentPolicy.evaluateAnvilOutput(0, 1).isRejected());
        assertFalse(EnchantmentPolicy.evaluateAnvilOutput(1, 1).isRejected());
        AnvilCapVerdict rejected = EnchantmentPolicy.evaluateAnvilOutput(2, 1);
        assertTrue(rejected.isRejected());
        assertEquals(2, rejected.getAttemptedCount());
        assertEquals(1, rejected.getMaxAllowed());

        AnvilCapVerdict partialUpgradeRejected = EnchantmentPolicy.evaluateAnvilOutput(3, 2);
        assertTrue(partialUpgradeRejected.isRejected());
        assertEquals(3, partialUpgradeRejected.getAttemptedCount());
        assertEquals(2, partialUpgradeRejected.getMaxAllowed());
    }

    @Test
    void anvilComputationGuardBalancesNestedCalls() {
        AnvilComputationGuard.reset();
        assertFalse(AnvilComputationGuard.isInsideAnvilComputation());
        AnvilComputationGuard.enter();
        assertTrue(AnvilComputationGuard.isInsideAnvilComputation());
        AnvilComputationGuard.enter();
        assertTrue(AnvilComputationGuard.isInsideAnvilComputation());
        AnvilComputationGuard.exit();
        assertTrue(AnvilComputationGuard.isInsideAnvilComputation());
        AnvilComputationGuard.exit();
        assertFalse(AnvilComputationGuard.isInsideAnvilComputation());
        AnvilComputationGuard.exit();
        assertFalse(AnvilComputationGuard.isInsideAnvilComputation());
        AnvilComputationGuard.enter();
        assertTrue(AnvilComputationGuard.isInsideAnvilComputation(), "Extra exit must not underflow guard depth");
        AnvilComputationGuard.reset();
        assertFalse(AnvilComputationGuard.isInsideAnvilComputation());
    }
}
