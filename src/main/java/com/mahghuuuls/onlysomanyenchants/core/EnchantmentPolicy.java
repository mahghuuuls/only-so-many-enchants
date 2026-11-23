package com.mahghuuuls.onlysomanyenchants.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class EnchantmentPolicy {

    private EnchantmentPolicy() {
    }

    public static int clampCap(int configuredCap) {
        return Math.max(1, configuredCap);
    }

    public static List<Integer> selectIndicesToKeep(int totalCount, int maxDistinct, Random random) {
        int cap = clampCap(maxDistinct);
        if (totalCount <= 0) {
            return Collections.emptyList();
        }
        if (totalCount <= cap) {
            List<Integer> all = new ArrayList<Integer>(totalCount);
            for (int i = 0; i < totalCount; i++) {
                all.add(i);
            }
            return all;
        }
        if (random == null) {
            throw new IllegalArgumentException("random must not be null when trimming is required");
        }

        int[] pool = new int[totalCount];
        for (int i = 0; i < totalCount; i++) {
            pool[i] = i;
        }
        // Partial Fisher-Yates shuffle gives every index equal probability cap / totalCount.
        for (int i = 0; i < cap; i++) {
            int swapIndex = i + random.nextInt(totalCount - i);
            int tmp = pool[i];
            pool[i] = pool[swapIndex];
            pool[swapIndex] = tmp;
        }

        List<Integer> selected = new ArrayList<Integer>(cap);
        for (int i = 0; i < cap; i++) {
            selected.add(pool[i]);
        }
        Collections.sort(selected);
        return selected;
    }

    public static Random createEnchantingTableRandom(int xpSeed, int slot) {
        long seed = ((long) xpSeed * 0x9E3779B97F4A7C15L)
                ^ (((long) slot + 1L) * 0xBF58476D1CE4E5B9L)
                ^ 0x94D049BB133111EBL;
        return new Random(seed);
    }

    public static <T> List<T> trimEnchantmentList(List<T> list, int maxDistinct, Random random) {
        if (list == null || list.isEmpty()) {
            return list;
        }
        int cap = clampCap(maxDistinct);
        if (list.size() <= cap) {
            return list;
        }
        List<Integer> keepIndices = selectIndicesToKeep(list.size(), cap, random);
        List<T> trimmed = new ArrayList<T>(keepIndices.size());
        for (int idx : keepIndices) {
            trimmed.add(list.get(idx));
        }
        return trimmed;
    }

    public static AnvilCapVerdict evaluateAnvilOutput(int outputDistinctCount, int maxDistinct) {
        int cap = clampCap(maxDistinct);
        if (outputDistinctCount > cap) {
            return AnvilCapVerdict.rejected(outputDistinctCount, cap);
        }
        return AnvilCapVerdict.allowed(Math.max(0, outputDistinctCount), cap);
    }

    public static String formatCapRejectionMessage(int attemptedCount, int maxAllowed) {
        return "This would give the item " + attemptedCount + " enchantments. The limit is " + maxAllowed + ".";
    }
}
