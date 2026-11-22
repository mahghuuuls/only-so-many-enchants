package com.mahghuuuls.onlysomanyenchants.core;

public final class AnvilComputationGuard {

    private static final ThreadLocal<Integer> DEPTH = new ThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return Integer.valueOf(0);
        }
    };

    private AnvilComputationGuard() {
    }

    public static void enter() {
        DEPTH.set(Integer.valueOf(DEPTH.get().intValue() + 1));
    }

    public static void exit() {
        int current = DEPTH.get().intValue();
        if (current <= 1) {
            DEPTH.remove();
        } else {
            DEPTH.set(Integer.valueOf(current - 1));
        }
    }

    public static boolean isInsideAnvilComputation() {
        return DEPTH.get().intValue() > 0;
    }

    public static void reset() {
        DEPTH.remove();
    }
}
