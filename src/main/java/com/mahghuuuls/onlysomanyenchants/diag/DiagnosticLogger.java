package com.mahghuuuls.onlysomanyenchants.diag;

import com.mahghuuuls.onlysomanyenchants.Tags;
import com.mahghuuuls.onlysomanyenchants.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

public final class DiagnosticLogger {

    public static final long DEFAULT_RATE_LIMIT_MS = 2000L;
    private static final int MAX_TRACKED_KEYS = 512;
    private static final Logger LOGGER = LogManager.getLogger(Tags.MOD_ID);

    private static final DiagnosticLogger INSTANCE = new DiagnosticLogger(
            ModConfig::isDiagnosticMode,
            System::currentTimeMillis,
            LOGGER::info,
            DEFAULT_RATE_LIMIT_MS
    );

    private final BooleanSupplier enabledSupplier;
    private final LongSupplier clockMillis;
    private final Consumer<String> sink;
    private final long rateLimitMs;
    private final Map<String, Long> recentEvents = new LinkedHashMap<String, Long>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
            return size() > MAX_TRACKED_KEYS;
        }
    };

    public DiagnosticLogger(
            BooleanSupplier enabledSupplier,
            LongSupplier clockMillis,
            Consumer<String> sink,
            long rateLimitMs
    ) {
        this.enabledSupplier = enabledSupplier;
        this.clockMillis = clockMillis;
        this.sink = sink;
        this.rateLimitMs = Math.max(0L, rateLimitMs);
    }

    public static DiagnosticLogger get() {
        return INSTANCE;
    }

    public void logTrim(
            String sourcePath,
            String itemId,
            int cap,
            int beforeCount,
            List<String> beforeEntries,
            int afterCount,
            List<String> afterEntries,
            List<String> removedEntries
    ) {
        if (!enabledSupplier.getAsBoolean()) {
            return;
        }
        String key = "TRIM|" + sourcePath + "|" + itemId + "|" + cap + "|" + beforeEntries + "|" + afterEntries;
        if (!allowEmit(key)) {
            return;
        }
        sink.accept(
                "[OSME][TRIM] path=" + sourcePath
                        + " item=" + itemId
                        + " cap=" + cap
                        + " before=" + beforeCount + formatList(beforeEntries)
                        + " after=" + afterCount + formatList(afterEntries)
                        + " removed=" + formatList(removedEntries)
        );
    }

    public void logRejection(
            String sourcePath,
            String itemId,
            int cap,
            int attemptedCount,
            List<String> attemptedEntries,
            String reason
    ) {
        if (!enabledSupplier.getAsBoolean()) {
            return;
        }
        String key = "REJECT|" + sourcePath + "|" + itemId + "|" + cap + "|" + attemptedCount + "|" + attemptedEntries + "|" + reason;
        if (!allowEmit(key)) {
            return;
        }
        sink.accept(
                "[OSME][REJECT] path=" + sourcePath
                        + " item=" + itemId
                        + " cap=" + cap
                        + " attempted=" + attemptedCount + formatList(attemptedEntries)
                        + " reason=" + reason
        );
    }

    public void logBoundaryWarning(String sourcePath, String detail) {
        if (!enabledSupplier.getAsBoolean()) {
            return;
        }
        String key = "WARN|" + sourcePath + "|" + detail;
        if (!allowEmit(key)) {
            return;
        }
        sink.accept("[OSME][WARN] path=" + sourcePath + " detail=" + detail);
    }

    public synchronized void clearRateLimitCache() {
        recentEvents.clear();
    }

    private synchronized boolean allowEmit(String key) {
        long now = clockMillis.getAsLong();
        Long last = recentEvents.get(key);
        if (last != null && (now - last.longValue()) < rateLimitMs) {
            return false;
        }
        recentEvents.put(key, Long.valueOf(now));
        return true;
    }

    private static String formatList(List<String> entries) {
        if (entries == null || entries.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(entries.get(i));
        }
        sb.append(']');
        return sb.toString();
    }
}
