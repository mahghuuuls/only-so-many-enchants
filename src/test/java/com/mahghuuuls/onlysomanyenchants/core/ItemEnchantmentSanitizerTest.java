package com.mahghuuuls.onlysomanyenchants.core;

import com.mahghuuuls.onlysomanyenchants.diag.DiagnosticLogger;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemEnchantmentSanitizerTest {

    private static final EnchantmentResolver TEST_RESOLVER = new EnchantmentResolver() {
        @Override
        public String resolveRegistryName(int enchantmentId) {
            switch (enchantmentId) {
                case 16:
                    return "minecraft:sharpness";
                case 21:
                    return "minecraft:looting";
                case 34:
                    return "minecraft:unbreaking";
                case 71:
                    return "minecraft:vanishing_curse";
                case 200:
                    return "radenchants:vampiric";
                default:
                    return null;
            }
        }
    };

    private static NBTTagCompound enchEntry(int id, int lvl) {
        NBTTagCompound entry = new NBTTagCompound();
        entry.setShort("id", (short) id);
        entry.setShort("lvl", (short) lvl);
        return entry;
    }

    private static NBTTagCompound swordTagWithThreeEnchantsAndMetadata() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList ench = new NBTTagList();
        ench.appendTag(enchEntry(16, 4));
        ench.appendTag(enchEntry(21, 2));
        ench.appendTag(enchEntry(71, 1));
        tag.setTag(ItemEnchantmentSanitizer.TAG_ENCH, ench);

        NBTTagCompound display = new NBTTagCompound();
        display.setString("Name", "Hero Blade");
        tag.setTag("display", display);

        NBTTagCompound quality = new NBTTagCompound();
        quality.setString("Name", "quality.legendary.name");
        quality.setString("Color", "gold");
        tag.setTag("Quality", quality);

        tag.setInteger("RepairCost", 3);
        return tag;
    }

    @Test
    void withinCapItemIsUnchanged() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList ench = new NBTTagList();
        ench.appendTag(enchEntry(16, 5));
        tag.setTag(ItemEnchantmentSanitizer.TAG_ENCH, ench);

        NBTTagCompound quality = new NBTTagCompound();
        quality.setString("Name", "quality.masterful.name");
        tag.setTag("Quality", quality);

        assertEquals(1, ItemEnchantmentSanitizer.countDistinctEnchantments(tag, TEST_RESOLVER));
        TrimResult result = ItemEnchantmentSanitizer.sanitizeTagCompound(
                tag, 1, "minecraft:diamond_sword", "test", new Random(1L), TEST_RESOLVER, null
        );
        assertFalse(result.isModified());
        assertEquals(1, tag.getTagList(ItemEnchantmentSanitizer.TAG_ENCH, Constants.NBT.TAG_COMPOUND).tagCount());
        assertEquals("quality.masterful.name", tag.getCompoundTag("Quality").getString("Name"));
    }

    @Test
    void trimsThreeDistinctEnchantmentsIncludingCurseWithEqualChanceAndPreservesLevelsAndNbt() {
        int trials = 3000;
        Map<Integer, Integer> survivorCounts = new HashMap<Integer, Integer>();
        survivorCounts.put(Integer.valueOf(16), Integer.valueOf(0));
        survivorCounts.put(Integer.valueOf(21), Integer.valueOf(0));
        survivorCounts.put(Integer.valueOf(71), Integer.valueOf(0));

        Random rng = new Random(20261122L);
        for (int i = 0; i < trials; i++) {
            NBTTagCompound tag = swordTagWithThreeEnchantsAndMetadata();
            NBTTagCompound expectedQuality = tag.getCompoundTag("Quality").copy();
            NBTTagCompound expectedDisplay = tag.getCompoundTag("display").copy();

            TrimResult result = ItemEnchantmentSanitizer.sanitizeTagCompound(
                    tag, 1, "minecraft:diamond_sword", "test_trial", rng, TEST_RESOLVER, null
            );

            assertTrue(result.isModified());
            assertEquals(3, result.getBeforeCount());
            assertEquals(1, result.getAfterCount());
            assertEquals(2, result.getRemovedEntries().size());

            NBTTagList afterList = tag.getTagList(ItemEnchantmentSanitizer.TAG_ENCH, Constants.NBT.TAG_COMPOUND);
            assertEquals(1, afterList.tagCount());
            NBTTagCompound kept = afterList.getCompoundTagAt(0);
            int keptId = kept.getShort("id") & 0xFFFF;
            short keptLvl = kept.getShort("lvl");

            if (keptId == 16) {
                assertEquals((short) 4, keptLvl);
            } else if (keptId == 21) {
                assertEquals((short) 2, keptLvl);
            } else if (keptId == 71) {
                assertEquals((short) 1, keptLvl);
            } else {
                throw new AssertionError("Unexpected surviving enchantment id: " + keptId);
            }
            survivorCounts.put(Integer.valueOf(keptId), Integer.valueOf(survivorCounts.get(Integer.valueOf(keptId)).intValue() + 1));

            assertEquals(expectedQuality, tag.getCompoundTag("Quality"));
            assertEquals(expectedDisplay, tag.getCompoundTag("display"));
            assertEquals(3, tag.getInteger("RepairCost"));
        }

        for (Map.Entry<Integer, Integer> entry : survivorCounts.entrySet()) {
            double freq = (double) entry.getValue().intValue() / trials;
            assertTrue(freq >= 0.28 && freq <= 0.38, "Enchantment " + entry.getKey() + " frequency out of bounds: " + freq);
        }
    }

    @Test
    void trimsStoredEnchantmentsOnBooksAndPlates() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList stored = new NBTTagList();
        stored.appendTag(enchEntry(16, 4));
        stored.appendTag(enchEntry(200, 3));
        stored.appendTag(enchEntry(34, 3));
        tag.setTag(ItemEnchantmentSanitizer.TAG_STORED_ENCHANTMENTS, stored);

        List<String> logs = new ArrayList<String>();
        DiagnosticLogger diag = new DiagnosticLogger(() -> true, () -> 1000L, logs::add, 0L);

        TrimResult result = ItemEnchantmentSanitizer.sanitizeTagCompound(
                tag, 1, "minecraft:enchanted_book", "book_test", new Random(7L), TEST_RESOLVER, diag
        );

        assertTrue(result.isModified());
        assertEquals(3, result.getBeforeCount());
        assertEquals(1, result.getAfterCount());
        assertEquals(1, tag.getTagList(ItemEnchantmentSanitizer.TAG_STORED_ENCHANTMENTS, Constants.NBT.TAG_COMPOUND).tagCount());
        assertEquals(1, logs.size());
        assertTrue(logs.get(0).contains("[OSME][TRIM]"));
        assertTrue(logs.get(0).contains("radenchants:vampiric@3"));
    }

    @Test
    void handlesUnregisteredAndDuplicateEnchantmentIdsSafely() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList ench = new NBTTagList();
        ench.appendTag(enchEntry(16, 3));
        ench.appendTag(enchEntry(16, 5));
        ench.appendTag(enchEntry(9999, 1));
        tag.setTag(ItemEnchantmentSanitizer.TAG_ENCH, ench);
        tag.setString("CustomTag", "keep-me");

        // Only 1 distinct valid enchantment (Sharpness); unregistered id 9999 does not crash or force trim at cap 1.
        assertEquals(1, ItemEnchantmentSanitizer.countDistinctEnchantments(tag, TEST_RESOLVER));
        TrimResult withinCap = ItemEnchantmentSanitizer.sanitizeTagCompound(
                tag, 1, "minecraft:diamond_sword", "corrupt_test", new Random(9L), TEST_RESOLVER, null
        );
        assertFalse(withinCap.isModified());
        assertEquals("keep-me", tag.getString("CustomTag"));

        // Adding a second valid enchantment triggers trim to 1 valid enchantment without crashing on id 9999.
        ench.appendTag(enchEntry(21, 2));
        assertEquals(2, ItemEnchantmentSanitizer.countDistinctEnchantments(tag, TEST_RESOLVER));
        TrimResult trimmed = ItemEnchantmentSanitizer.sanitizeTagCompound(
                tag, 1, "minecraft:diamond_sword", "corrupt_test", new Random(9L), TEST_RESOLVER, null
        );
        assertTrue(trimmed.isModified());
        assertEquals(1, ItemEnchantmentSanitizer.countDistinctEnchantments(tag, TEST_RESOLVER));
        assertEquals("keep-me", tag.getString("CustomTag"));

        // Also verify StoredEnchantments with an unregistered ID during both inspect and trim.
        NBTTagCompound bookTag = new NBTTagCompound();
        NBTTagList stored = new NBTTagList();
        stored.appendTag(enchEntry(16, 4));
        stored.appendTag(enchEntry(200, 2));
        stored.appendTag(enchEntry(9999, 1));
        bookTag.setTag(ItemEnchantmentSanitizer.TAG_STORED_ENCHANTMENTS, stored);
        bookTag.setString("BookMarker", "intact");

        InspectionReport bookReport = ItemEnchantmentSanitizer.inspectTagCompound(
                bookTag, "minecraft:enchanted_book", 1, TEST_RESOLVER
        );
        assertEquals(2, bookReport.getDistinctCount());
        assertTrue(bookReport.isOverCap());

        TrimResult bookTrim = ItemEnchantmentSanitizer.sanitizeTagCompound(
                bookTag, 1, "minecraft:enchanted_book", "corrupt_book_test", new Random(11L), TEST_RESOLVER, null
        );
        assertTrue(bookTrim.isModified());
        assertEquals(1, ItemEnchantmentSanitizer.countDistinctEnchantments(bookTag, TEST_RESOLVER));
        assertEquals("intact", bookTag.getString("BookMarker"));
    }

    @Test
    void sanitizesTopLevelNestedItemsList() {
        NBTTagCompound innerItemTag = swordTagWithThreeEnchantsAndMetadata();
        NBTTagCompound itemEntry = new NBTTagCompound();
        itemEntry.setByte("Slot", (byte) 0);
        itemEntry.setString("id", "minecraft:diamond_sword");
        itemEntry.setTag("tag", innerItemTag);

        NBTTagList items = new NBTTagList();
        items.appendTag(itemEntry);

        NBTTagCompound backpackTag = new NBTTagCompound();
        backpackTag.setTag(ItemEnchantmentSanitizer.TAG_ITEMS, items);

        boolean modified = ItemEnchantmentSanitizer.sanitizeNestedNbt(
                backpackTag, 1, "backpack_test", new Random(17L), TEST_RESOLVER, null, 0
        );
        assertTrue(modified);
        assertEquals(1, ItemEnchantmentSanitizer.countDistinctEnchantments(
                backpackTag.getTagList(ItemEnchantmentSanitizer.TAG_ITEMS, Constants.NBT.TAG_COMPOUND)
                        .getCompoundTagAt(0)
                        .getCompoundTag("tag"),
                TEST_RESOLVER
        ));
    }

    @Test
    void sanitizesNestedShulkerBoxNbtPreservingSlotsAndUnrelatedFields() {
        NBTTagCompound innerItemTag = swordTagWithThreeEnchantsAndMetadata();
        NBTTagCompound itemEntry = new NBTTagCompound();
        itemEntry.setByte("Slot", (byte) 5);
        itemEntry.setByte("Count", (byte) 1);
        itemEntry.setShort("Damage", (short) 42);
        itemEntry.setString("id", "minecraft:diamond_sword");
        itemEntry.setTag("tag", innerItemTag);

        NBTTagList items = new NBTTagList();
        items.appendTag(itemEntry);

        NBTTagCompound blockEntityTag = new NBTTagCompound();
        blockEntityTag.setTag(ItemEnchantmentSanitizer.TAG_ITEMS, items);

        NBTTagCompound shulkerTag = new NBTTagCompound();
        shulkerTag.setTag(ItemEnchantmentSanitizer.TAG_BLOCK_ENTITY, blockEntityTag);

        boolean modified = ItemEnchantmentSanitizer.sanitizeNestedNbt(
                shulkerTag, 1, "shulker_test", new Random(55L), TEST_RESOLVER, null, 0
        );

        assertTrue(modified);
        NBTTagCompound afterEntry = shulkerTag
                .getCompoundTag(ItemEnchantmentSanitizer.TAG_BLOCK_ENTITY)
                .getTagList(ItemEnchantmentSanitizer.TAG_ITEMS, Constants.NBT.TAG_COMPOUND)
                .getCompoundTagAt(0);
        assertEquals((byte) 5, afterEntry.getByte("Slot"));
        assertEquals((short) 42, afterEntry.getShort("Damage"));
        assertEquals(1, ItemEnchantmentSanitizer.countDistinctEnchantments(afterEntry.getCompoundTag("tag"), TEST_RESOLVER));
        assertEquals("quality.legendary.name", afterEntry.getCompoundTag("tag").getCompoundTag("Quality").getString("Name"));
    }

    @Test
    void inspectTagCompoundIsStrictlyReadOnly() {
        NBTTagCompound tag = swordTagWithThreeEnchantsAndMetadata();
        NBTTagCompound snapshot = tag.copy();

        InspectionReport report = ItemEnchantmentSanitizer.inspectTagCompound(
                tag, "minecraft:diamond_sword", 1, TEST_RESOLVER
        );

        assertEquals("minecraft:diamond_sword", report.getItemId());
        assertEquals(3, report.getDistinctCount());
        assertEquals(1, report.getActiveCap());
        assertTrue(report.isOverCap());
        assertEquals(3, report.getEntries().size());
        assertEquals(snapshot, tag);
    }

    @Test
    void sanitizesNestedContainersUpToDepthFourAndStopsSafelyAtDepthLimit() {
        NBTTagCompound depth4Sword = swordTagWithThreeEnchantsAndMetadata();
        NBTTagCompound depth4Nested = wrapInShulkerLevels(depth4Sword, ItemEnchantmentSanitizer.MAX_NESTED_DEPTH);
        boolean modifiedDepth4 = ItemEnchantmentSanitizer.sanitizeNestedNbt(
                depth4Nested, 1, "depth4_test", new Random(61L), TEST_RESOLVER, null, 0
        );
        assertTrue(modifiedDepth4);
        assertEquals(1, ItemEnchantmentSanitizer.countDistinctEnchantments(
                unwrapShulkerLevels(depth4Nested, ItemEnchantmentSanitizer.MAX_NESTED_DEPTH),
                TEST_RESOLVER
        ));

        NBTTagCompound depth5Sword = swordTagWithThreeEnchantsAndMetadata();
        NBTTagCompound depth5Nested = wrapInShulkerLevels(depth5Sword, ItemEnchantmentSanitizer.MAX_NESTED_DEPTH + 1);
        boolean modifiedDepth5 = ItemEnchantmentSanitizer.sanitizeNestedNbt(
                depth5Nested, 1, "depth5_test", new Random(62L), TEST_RESOLVER, null, 0
        );
        assertFalse(modifiedDepth5);
        assertEquals(3, ItemEnchantmentSanitizer.countDistinctEnchantments(
                unwrapShulkerLevels(depth5Nested, ItemEnchantmentSanitizer.MAX_NESTED_DEPTH + 1),
                TEST_RESOLVER
        ));
    }

    private static NBTTagCompound wrapInShulkerLevels(NBTTagCompound innerTag, int levels) {
        NBTTagCompound current = innerTag;
        for (int i = 0; i < levels; i++) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setByte("Slot", (byte) 0);
            entry.setString("id", i == 0 ? "minecraft:diamond_sword" : "minecraft:purple_shulker_box");
            entry.setTag("tag", current);

            NBTTagList items = new NBTTagList();
            items.appendTag(entry);

            NBTTagCompound blockEntityTag = new NBTTagCompound();
            blockEntityTag.setTag(ItemEnchantmentSanitizer.TAG_ITEMS, items);

            NBTTagCompound outer = new NBTTagCompound();
            outer.setTag(ItemEnchantmentSanitizer.TAG_BLOCK_ENTITY, blockEntityTag);
            current = outer;
        }
        return current;
    }

    private static NBTTagCompound unwrapShulkerLevels(NBTTagCompound outer, int levels) {
        NBTTagCompound current = outer;
        for (int i = 0; i < levels; i++) {
            current = current.getCompoundTag(ItemEnchantmentSanitizer.TAG_BLOCK_ENTITY)
                    .getTagList(ItemEnchantmentSanitizer.TAG_ITEMS, Constants.NBT.TAG_COMPOUND)
                    .getCompoundTagAt(0)
                    .getCompoundTag("tag");
        }
        return current;
    }
}
