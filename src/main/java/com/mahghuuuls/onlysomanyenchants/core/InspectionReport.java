package com.mahghuuuls.onlysomanyenchants.core;

import java.util.Collections;
import java.util.List;

public final class InspectionReport {

    public static final class Entry {
        private final int id;
        private final String registryName;
        private final short level;
        private final String sourceTag;

        public Entry(int id, String registryName, short level, String sourceTag) {
            this.id = id;
            this.registryName = registryName;
            this.level = level;
            this.sourceTag = sourceTag;
        }

        public int getId() {
            return id;
        }

        public String getRegistryName() {
            return registryName;
        }

        public short getLevel() {
            return level;
        }

        public String getSourceTag() {
            return sourceTag;
        }

        public String formatCompact() {
            return registryName + "@" + level;
        }
    }

    private final String itemId;
    private final int distinctCount;
    private final int activeCap;
    private final List<Entry> entries;

    public InspectionReport(String itemId, int distinctCount, int activeCap, List<Entry> entries) {
        this.itemId = itemId;
        this.distinctCount = distinctCount;
        this.activeCap = activeCap;
        this.entries = Collections.unmodifiableList(entries);
    }

    public String getItemId() {
        return itemId;
    }

    public int getDistinctCount() {
        return distinctCount;
    }

    public int getActiveCap() {
        return activeCap;
    }

    public boolean isOverCap() {
        return distinctCount > activeCap;
    }

    public List<Entry> getEntries() {
        return entries;
    }
}
