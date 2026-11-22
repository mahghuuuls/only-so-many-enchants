package com.mahghuuuls.onlysomanyenchants.core;

public final class AnvilCapVerdict {

    private final boolean rejected;
    private final int attemptedCount;
    private final int maxAllowed;

    private AnvilCapVerdict(boolean rejected, int attemptedCount, int maxAllowed) {
        this.rejected = rejected;
        this.attemptedCount = attemptedCount;
        this.maxAllowed = maxAllowed;
    }

    public static AnvilCapVerdict allowed(int count, int maxAllowed) {
        return new AnvilCapVerdict(false, count, maxAllowed);
    }

    public static AnvilCapVerdict rejected(int attemptedCount, int maxAllowed) {
        return new AnvilCapVerdict(true, attemptedCount, maxAllowed);
    }

    public boolean isRejected() {
        return rejected;
    }

    public int getAttemptedCount() {
        return attemptedCount;
    }

    public int getMaxAllowed() {
        return maxAllowed;
    }
}
