package com.mahghuuuls.onlysomanyenchants.config;

import com.mahghuuuls.onlysomanyenchants.Tags;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.relauncher.FMLInjectionData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModConfigTest {

    @TempDir
    Path tempDir;

    private Object previousMinecraftHome;

    @BeforeEach
    void setupMinecraftHome() throws Exception {
        Field field = FMLInjectionData.class.getDeclaredField("minecraftHome");
        field.setAccessible(true);
        previousMinecraftHome = field.get(null);
        field.set(null, tempDir.toFile());
    }

    @AfterEach
    void restoreMinecraftHome() throws Exception {
        Field field = FMLInjectionData.class.getDeclaredField("minecraftHome");
        field.setAccessible(true);
        field.set(null, previousMinecraftHome);
    }

    @Test
    void generatesDefaultConfigWithExpectedDefaults() throws Exception {
        File cfgFile = tempDir.resolve("onlysomanyenchants.cfg").toFile();
        List<String> errors = new ArrayList<String>();

        ModConfig.init(cfgFile, errors::add);

        assertTrue(cfgFile.isFile());
        assertEquals(1, ModConfig.getEffectiveCap());
        assertFalse(ModConfig.isDiagnosticMode());
        assertTrue(errors.isEmpty());

        String text = new String(Files.readAllBytes(cfgFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(text.contains("I:maxDistinctEnchantments=1"));
        assertTrue(text.contains("B:diagnosticMode=false"));
    }

    @Test
    void clampsZeroAndNegativeCapWithErrorLog() throws Exception {
        for (int badValue : new int[]{0, -5}) {
            File cfgFile = tempDir.resolve("bad_" + badValue + ".cfg").toFile();
            String content = "general {\n    I:maxDistinctEnchantments=" + badValue + "\n    B:diagnosticMode=true\n}\n";
            Files.write(cfgFile.toPath(), content.getBytes(StandardCharsets.UTF_8));

            List<String> errors = new ArrayList<String>();
            ModConfig.init(cfgFile, errors::add);

            assertEquals(1, ModConfig.getEffectiveCap());
            assertTrue(ModConfig.isDiagnosticMode());
            assertEquals(1, errors.size());
            assertTrue(errors.get(0).contains(Integer.toString(badValue)));
        }
    }

    @Test
    void reloadFromDiskUpdatesEffectiveCapImmediately() throws Exception {
        File cfgFile = tempDir.resolve("reload.cfg").toFile();
        Files.write(
                cfgFile.toPath(),
                "general {\n    I:maxDistinctEnchantments=2\n    B:diagnosticMode=false\n}\n".getBytes(StandardCharsets.UTF_8)
        );
        Configuration configuration = new Configuration(cfgFile);
        ModConfig.syncFromConfiguration(configuration, cfgFile, msg -> {});
        assertEquals(2, ModConfig.getEffectiveCap());

        Files.write(
                cfgFile.toPath(),
                "general {\n    I:maxDistinctEnchantments=1\n    B:diagnosticMode=false\n}\n".getBytes(StandardCharsets.UTF_8)
        );
        ModConfig.reloadFromDisk(configuration, cfgFile, msg -> {});
        assertEquals(1, ModConfig.getEffectiveCap());
    }

    @Test
    void onConfigChangedPreservesInMemoryEditsAndSavesToDisk() throws Exception {
        File cfgFile = tempDir.resolve("gui_reload.cfg").toFile();
        List<String> errors = new ArrayList<String>();
        ModConfig.init(cfgFile, errors::add);
        assertEquals(1, ModConfig.getEffectiveCap());
        assertFalse(ModConfig.isDiagnosticMode());

        // Simulate Forge GuiConfig mutating in-memory properties before firing OnConfigChangedEvent.
        Configuration liveConfig = ModConfig.getConfiguration();
        Property capProp = liveConfig.getCategory(Configuration.CATEGORY_GENERAL).get("maxDistinctEnchantments");
        Property diagProp = liveConfig.getCategory(Configuration.CATEGORY_GENERAL).get("diagnosticMode");
        capProp.set(3);
        diagProp.set(true);

        ModConfig.ReloadHandler handler = new ModConfig.ReloadHandler(errors::add);
        handler.onConfigChanged(new ConfigChangedEvent.OnConfigChangedEvent(Tags.MOD_ID, null, false, false));

        assertEquals(3, ModConfig.getEffectiveCap());
        assertTrue(ModConfig.isDiagnosticMode());
        assertTrue(errors.isEmpty());

        String saved = new String(Files.readAllBytes(cfgFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(saved.contains("I:maxDistinctEnchantments=3"));
        assertTrue(saved.contains("B:diagnosticMode=true"));

        // Verify invalid in-memory edit clamps to 1 and logs error.
        capProp.set(0);
        handler.onConfigChanged(new ConfigChangedEvent.OnConfigChangedEvent(Tags.MOD_ID, null, false, false));
        assertEquals(1, ModConfig.getEffectiveCap());
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("0"));
    }
}
