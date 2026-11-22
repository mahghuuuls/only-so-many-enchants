package com.mahghuuuls.onlysomanyenchants.core;

import java.util.Collections;
import java.util.List;

public final class TrimResult {

    public static final TrimResult UNCHANGED = new TrimResult(
            false,
            0,
            0,
            Collections.<String>emptyList(),
            Collections.<String>emptyList(),
            Collections.<String>emptyList()
    );

    private final boolean modified;
    private final int beforeCount;
    private final int afterCount;
    private final List<String> beforeEntries;
    private final List<String> afterEntries;
    private final List<String> removedEntries;

    public TrimResult(
            boolean modified,
            int beforeCount,
            int afterCount,
            List<String> beforeEntries,
            List<String> afterEntries,
            List<String> removedEntries
    ) {
        this.modified = modified;
        this.beforeCount = beforeCount;
        this.afterCount = afterCount;
        this.beforeEntries = Collections.unmodifiableList(beforeEntries);
        this.afterEntries = Collections.unmodifiableList(afterEntries);
        this.removedEntries = Collections.unmodifiableList(removedEntries);
    }

    public boolean isModified() {
        return modified;
    }

    public int getBeforeCount() {
        return beforeCount;
    }

    public int getAfterCount() {
        return afterCount;
    }

    public List<String> getBeforeEntries() {
        return beforeEntries;
    }

    public List<String> getAfterEntries() {
        return afterEntries;
    }

    public List<String> getRemovedEntries() {
        return removedEntries;
    }
}
