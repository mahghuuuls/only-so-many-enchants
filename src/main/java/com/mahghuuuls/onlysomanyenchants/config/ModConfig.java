package com.mahghuuuls.onlysomanyenchants.config;

import com.mahghuuuls.onlysomanyenchants.Tags;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.function.Consumer;

public final class ModConfig {

    public static final int DEFAULT_MAX_DISTINCT_ENCHANTMENTS = 1;
    public static final int MIN_MAX_DISTINCT_ENCHANTMENTS = 1;
    public static final boolean DEFAULT_DIAGNOSTIC_MODE = false;

    private static final Logger LOGGER = LogManager.getLogger(Tags.MOD_ID);

    private static Configuration configuration;
    private static File configFile;
    private static volatile int effectiveMaxDistinctEnchantments = DEFAULT_MAX_DISTINCT_ENCHANTMENTS;
    private static volatile boolean diagnosticMode = DEFAULT_DIAGNOSTIC_MODE;

    private ModConfig() {
    }

    public static void init(File file) {
        init(file, LOGGER::error);
    }

    public static void init(File file, Consumer<String> errorLogger) {
        configFile = file;
        configuration = new Configuration(file);
        syncFromConfiguration(configuration, file, errorLogger);
    }

    public static void reloadFromDisk(Configuration config, File sourceFile, Consumer<String> errorLogger) {
        if (config == null) {
            return;
        }
        config.load();
        syncFromConfiguration(config, sourceFile, errorLogger);
    }

    public static void syncFromConfiguration(Configuration config, File sourceFile, Consumer<String> errorLogger) {
        if (config == null) {
            return;
        }
        config.setCategoryComment(
                Configuration.CATEGORY_GENERAL,
                "General enforcement and diagnostic settings for Only So Many Enchants."
        );

        Property capProperty = config.get(
                Configuration.CATEGORY_GENERAL,
                "maxDistinctEnchantments",
                DEFAULT_MAX_DISTINCT_ENCHANTMENTS,
                "Maximum distinct enchantments allowed on any item or enchanted book (minimum: 1, default: 1). Lowering this value trims existing over-limit items on next gameplay view or use."
        );

        int configuredCap = capProperty.getInt(DEFAULT_MAX_DISTINCT_ENCHANTMENTS);
        if (configuredCap < MIN_MAX_DISTINCT_ENCHANTMENTS) {
            String pathLabel = sourceFile != null ? sourceFile.getPath() : "onlysomanyenchants.cfg";
            if (errorLogger != null) {
                errorLogger.accept(
                        "Invalid maxDistinctEnchantments value " + configuredCap
                                + " in " + pathLabel
                                + "; enforcing safe minimum " + MIN_MAX_DISTINCT_ENCHANTMENTS + "."
                );
            }
            effectiveMaxDistinctEnchantments = MIN_MAX_DISTINCT_ENCHANTMENTS;
        } else {
            effectiveMaxDistinctEnchantments = configuredCap;
        }

        Property diagProperty = config.get(
                Configuration.CATEGORY_GENERAL,
                "diagnosticMode",
                DEFAULT_DIAGNOSTIC_MODE,
                "Enable bounded, rate-limited diagnostic logging at enforcement and rejection boundaries (default: false)."
        );
        diagnosticMode = diagProperty.getBoolean(DEFAULT_DIAGNOSTIC_MODE);

        if (config.hasChanged()) {
            config.save();
        }
    }

    public static int getEffectiveCap() {
        return effectiveMaxDistinctEnchantments;
    }

    public static boolean isDiagnosticMode() {
        return diagnosticMode;
    }

    public static Configuration getConfiguration() {
        return configuration;
    }

    public static final class ReloadHandler {
        private final Consumer<String> errorLogger;

        public ReloadHandler() {
            this(LOGGER::error);
        }

        public ReloadHandler(Consumer<String> errorLogger) {
            this.errorLogger = errorLogger;
        }

        @SubscribeEvent
        public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (Tags.MOD_ID.equals(event.getModID()) && configuration != null) {
                syncFromConfiguration(configuration, configFile, errorLogger);
            }
        }
    }
}
