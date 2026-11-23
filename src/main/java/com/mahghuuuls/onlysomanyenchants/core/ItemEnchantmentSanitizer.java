package com.mahghuuuls.onlysomanyenchants.core;

import com.mahghuuuls.onlysomanyenchants.config.ModConfig;
import com.mahghuuuls.onlysomanyenchants.diag.DiagnosticLogger;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class ItemEnchantmentSanitizer {

    public static final String TAG_ENCH = "ench";
    public static final String TAG_STORED_ENCHANTMENTS = "StoredEnchantments";
    public static final String TAG_BLOCK_ENTITY = "BlockEntityTag";
    public static final String TAG_INVENTORY = "Inventory";
    public static final String TAG_ITEMS = "Items";
    public static final int MAX_NESTED_DEPTH = 4;

    private static final Random DEFAULT_RANDOM = new Random();

    private static final class RawEnchantEntry {
        final String sourceTag;
        final int id;
        final short level;
        final String registryName;
        final NBTTagCompound originalCompound;

        RawEnchantEntry(String sourceTag, int id, short level, String registryName, NBTTagCompound originalCompound) {
            this.sourceTag = sourceTag;
            this.id = id;
            this.level = level;
            this.registryName = registryName;
            this.originalCompound = originalCompound;
        }
    }

    private ItemEnchantmentSanitizer() {
    }

    public static Random defaultRandom() {
        return DEFAULT_RANDOM;
    }

    public static int countDistinctEnchantments(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) {
            return 0;
        }
        return countDistinctEnchantments(stack.getTagCompound(), EnchantmentResolver.FORGE);
    }

    public static boolean hasEnchantment(ItemStack stack, Enchantment enchantment) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound() || enchantment == null) {
            return false;
        }
        return hasEnchantment(stack.getTagCompound(), Enchantment.getEnchantmentID(enchantment));
    }

    public static boolean hasEnchantment(NBTTagCompound tag, int enchantmentId) {
        if (tag == null) {
            return false;
        }
        if (tag.hasKey(TAG_ENCH, Constants.NBT.TAG_LIST)
                && listContainsId(tag.getTagList(TAG_ENCH, Constants.NBT.TAG_COMPOUND), enchantmentId)) {
            return true;
        }
        return tag.hasKey(TAG_STORED_ENCHANTMENTS, Constants.NBT.TAG_LIST)
                && listContainsId(tag.getTagList(TAG_STORED_ENCHANTMENTS, Constants.NBT.TAG_COMPOUND), enchantmentId);
    }

    public static List<String> formatEnchantmentEntries(ItemStack stack) {
        InspectionReport report = inspectStack(stack);
        if (report.getEntries().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> formatted = new ArrayList<String>(report.getEntries().size());
        for (InspectionReport.Entry entry : report.getEntries()) {
            formatted.add(entry.formatCompact());
        }
        return formatted;
    }

    public static int countDistinctEnchantments(NBTTagCompound tag, EnchantmentResolver resolver) {
        if (tag == null) {
            return 0;
        }
        boolean hasEnch = tag.hasKey(TAG_ENCH, Constants.NBT.TAG_LIST);
        boolean hasStored = tag.hasKey(TAG_STORED_ENCHANTMENTS, Constants.NBT.TAG_LIST);
        if (!hasEnch && !hasStored) {
            return 0;
        }
        Set<Integer> distinctIds = new HashSet<Integer>(4);
        if (hasEnch) {
            collectValidIds(tag.getTagList(TAG_ENCH, Constants.NBT.TAG_COMPOUND), resolver, distinctIds);
        }
        if (hasStored) {
            collectValidIds(tag.getTagList(TAG_STORED_ENCHANTMENTS, Constants.NBT.TAG_COMPOUND), resolver, distinctIds);
        }
        return distinctIds.size();
    }

    public static InspectionReport inspectStack(ItemStack stack) {
        int cap = ModConfig.getEffectiveCap();
        if (stack == null || stack.isEmpty()) {
            return new InspectionReport("minecraft:air", 0, cap, Collections.<InspectionReport.Entry>emptyList());
        }
        String itemId = describeItem(stack);
        return inspectTagCompound(stack.getTagCompound(), itemId, cap, EnchantmentResolver.FORGE);
    }

    public static InspectionReport inspectTagCompound(
            NBTTagCompound tag,
            String itemId,
            int activeCap,
            EnchantmentResolver resolver
    ) {
        int cap = EnchantmentPolicy.clampCap(activeCap);
        if (tag == null) {
            return new InspectionReport(itemId, 0, cap, Collections.<InspectionReport.Entry>emptyList());
        }
        Map<Integer, RawEnchantEntry> distinctValid = new LinkedHashMap<Integer, RawEnchantEntry>();
        List<RawEnchantEntry> ignoredUnregistered = new ArrayList<RawEnchantEntry>();
        collectEntries(tag, resolver, distinctValid, ignoredUnregistered);

        List<InspectionReport.Entry> entries = new ArrayList<InspectionReport.Entry>(distinctValid.size());
        for (RawEnchantEntry entry : distinctValid.values()) {
            entries.add(new InspectionReport.Entry(entry.id, entry.registryName, entry.level, entry.sourceTag));
        }
        return new InspectionReport(itemId, entries.size(), cap, entries);
    }

    public static TrimResult sanitizePassiveSetEnchantments(ItemStack stack, Random random) {
        if (stack == null || stack.isEmpty() || AnvilComputationGuard.isInsideAnvilComputation()) {
            return TrimResult.UNCHANGED;
        }
        return sanitizeStack(stack, "set_enchantments", random != null ? random : DEFAULT_RANDOM);
    }

    public static TrimResult sanitizeStack(ItemStack stack, String sourcePath, Random random) {
        return sanitizeStack(stack, ModConfig.getEffectiveCap(), sourcePath, random, EnchantmentResolver.FORGE, DiagnosticLogger.get());
    }

    public static TrimResult sanitizeStack(
            ItemStack stack,
            int maxDistinct,
            String sourcePath,
            Random random,
            EnchantmentResolver resolver,
            DiagnosticLogger logger
    ) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) {
            return TrimResult.UNCHANGED;
        }
        NBTTagCompound tag = stack.getTagCompound();
        return sanitizeTagCompound(tag, maxDistinct, describeItem(stack), sourcePath, random, resolver, logger);
    }

    public static TrimResult sanitizeTagCompound(
            NBTTagCompound tag,
            int maxDistinct,
            String itemId,
            String sourcePath,
            Random random,
            EnchantmentResolver resolver,
            DiagnosticLogger logger
    ) {
        if (tag == null) {
            return TrimResult.UNCHANGED;
        }
        int cap = EnchantmentPolicy.clampCap(maxDistinct);
        boolean hasEnch = tag.hasKey(TAG_ENCH, Constants.NBT.TAG_LIST);
        boolean hasStored = tag.hasKey(TAG_STORED_ENCHANTMENTS, Constants.NBT.TAG_LIST);
        if (!hasEnch && !hasStored) {
            return TrimResult.UNCHANGED;
        }

        int rawEnchCount = hasEnch ? tag.getTagList(TAG_ENCH, Constants.NBT.TAG_COMPOUND).tagCount() : 0;
        int rawStoredCount = hasStored ? tag.getTagList(TAG_STORED_ENCHANTMENTS, Constants.NBT.TAG_COMPOUND).tagCount() : 0;
        if (rawEnchCount + rawStoredCount <= cap) {
            return TrimResult.UNCHANGED;
        }

        Map<Integer, RawEnchantEntry> distinctValid = new LinkedHashMap<Integer, RawEnchantEntry>();
        List<RawEnchantEntry> preservedUnregistered = new ArrayList<RawEnchantEntry>();
        collectEntries(tag, resolver, distinctValid, preservedUnregistered);

        if (distinctValid.size() <= cap) {
            return TrimResult.UNCHANGED;
        }

        List<RawEnchantEntry> validList = new ArrayList<RawEnchantEntry>(distinctValid.values());
        Random rng = random != null ? random : DEFAULT_RANDOM;
        List<Integer> keepIndices = EnchantmentPolicy.selectIndicesToKeep(validList.size(), cap, rng);
        Set<Integer> keepSet = new HashSet<Integer>(keepIndices);

        List<String> beforeNames = new ArrayList<String>(validList.size());
        List<String> afterNames = new ArrayList<String>(keepIndices.size());
        List<String> removedNames = new ArrayList<String>(validList.size() - keepIndices.size());

        NBTTagList rebuiltEnch = new NBTTagList();
        NBTTagList rebuiltStored = new NBTTagList();

        for (int i = 0; i < validList.size(); i++) {
            RawEnchantEntry entry = validList.get(i);
            String label = entry.registryName + "@" + entry.level;
            beforeNames.add(label);
            if (keepSet.contains(Integer.valueOf(i))) {
                afterNames.add(label);
                appendToTagList(entry, rebuiltEnch, rebuiltStored);
            } else {
                removedNames.add(label);
            }
        }

        for (RawEnchantEntry unregistered : preservedUnregistered) {
            appendToTagList(unregistered, rebuiltEnch, rebuiltStored);
        }

        if (hasEnch) {
            if (rebuiltEnch.tagCount() > 0) {
                tag.setTag(TAG_ENCH, rebuiltEnch);
            } else {
                tag.removeTag(TAG_ENCH);
            }
        }
        if (hasStored) {
            if (rebuiltStored.tagCount() > 0) {
                tag.setTag(TAG_STORED_ENCHANTMENTS, rebuiltStored);
            } else {
                tag.removeTag(TAG_STORED_ENCHANTMENTS);
            }
        }

        if (logger != null) {
            logger.logTrim(
                    sourcePath,
                    itemId,
                    cap,
                    validList.size(),
                    beforeNames,
                    afterNames.size(),
                    afterNames,
                    removedNames
            );
        }

        return new TrimResult(true, validList.size(), afterNames.size(), beforeNames, afterNames, removedNames);
    }

    public static boolean sanitizeNestedNbt(ItemStack stack, String sourcePath, Random random, int depth) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) {
            return false;
        }
        return sanitizeNestedNbt(
                stack.getTagCompound(),
                ModConfig.getEffectiveCap(),
                sourcePath,
                random,
                EnchantmentResolver.FORGE,
                DiagnosticLogger.get(),
                depth
        );
    }

    public static boolean sanitizeNestedNbt(
            NBTTagCompound rootTag,
            int maxDistinct,
            String sourcePath,
            Random random,
            EnchantmentResolver resolver,
            DiagnosticLogger logger,
            int depth
    ) {
        if (rootTag == null || depth >= MAX_NESTED_DEPTH) {
            return false;
        }
        boolean modified = false;
        if (rootTag.hasKey(TAG_BLOCK_ENTITY, Constants.NBT.TAG_COMPOUND)) {
            NBTTagCompound blockEntityTag = rootTag.getCompoundTag(TAG_BLOCK_ENTITY);
            if (blockEntityTag.hasKey(TAG_ITEMS, Constants.NBT.TAG_LIST)) {
                modified |= sanitizeSerializedItemList(
                        blockEntityTag.getTagList(TAG_ITEMS, Constants.NBT.TAG_COMPOUND),
                        maxDistinct,
                        sourcePath + ":nested_block_entity",
                        random,
                        resolver,
                        logger,
                        depth + 1
                );
            }
        }
        if (rootTag.hasKey(TAG_INVENTORY, Constants.NBT.TAG_COMPOUND)) {
            NBTTagCompound inventoryTag = rootTag.getCompoundTag(TAG_INVENTORY);
            if (inventoryTag.hasKey(TAG_ITEMS, Constants.NBT.TAG_LIST)) {
                modified |= sanitizeSerializedItemList(
                        inventoryTag.getTagList(TAG_ITEMS, Constants.NBT.TAG_COMPOUND),
                        maxDistinct,
                        sourcePath + ":nested_inventory",
                        random,
                        resolver,
                        logger,
                        depth + 1
                );
            }
        }
        if (rootTag.hasKey(TAG_ITEMS, Constants.NBT.TAG_LIST)) {
            modified |= sanitizeSerializedItemList(
                    rootTag.getTagList(TAG_ITEMS, Constants.NBT.TAG_COMPOUND),
                    maxDistinct,
                    sourcePath + ":nested_items",
                    random,
                    resolver,
                    logger,
                    depth + 1
            );
        }
        return modified;
    }

    private static boolean sanitizeSerializedItemList(
            NBTTagList itemsList,
            int maxDistinct,
            String sourcePath,
            Random random,
            EnchantmentResolver resolver,
            DiagnosticLogger logger,
            int depth
    ) {
        boolean modified = false;
        int count = itemsList.tagCount();
        for (int i = 0; i < count; i++) {
            NBTTagCompound itemEntry = itemsList.getCompoundTagAt(i);
            if (!itemEntry.hasKey("tag", Constants.NBT.TAG_COMPOUND)) {
                continue;
            }
            NBTTagCompound itemTag = itemEntry.getCompoundTag("tag");
            String nestedItemId = itemEntry.hasKey("id", Constants.NBT.TAG_STRING)
                    ? itemEntry.getString("id")
                    : "unknown";
            TrimResult trimResult = sanitizeTagCompound(itemTag, maxDistinct, nestedItemId, sourcePath, random, resolver, logger);
            if (trimResult.isModified()) {
                modified = true;
            }
            if (depth < MAX_NESTED_DEPTH) {
                modified |= sanitizeNestedNbt(itemTag, maxDistinct, sourcePath, random, resolver, logger, depth);
            }
        }
        return modified;
    }

    private static boolean listContainsId(NBTTagList list, int enchantmentId) {
        int count = list.tagCount();
        for (int i = 0; i < count; i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            if (entry.hasKey("id", Constants.NBT.TAG_ANY_NUMERIC) && (entry.getShort("id") & 0xFFFF) == enchantmentId) {
                return true;
            }
        }
        return false;
    }

    private static void collectValidIds(NBTTagList list, EnchantmentResolver resolver, Set<Integer> distinctIds) {
        int count = list.tagCount();
        for (int i = 0; i < count; i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            if (!entry.hasKey("id", Constants.NBT.TAG_ANY_NUMERIC)) {
                continue;
            }
            int id = entry.getShort("id") & 0xFFFF;
            if (resolver.resolveRegistryName(id) != null) {
                distinctIds.add(Integer.valueOf(id));
            }
        }
    }

    private static void collectEntries(
            NBTTagCompound tag,
            EnchantmentResolver resolver,
            Map<Integer, RawEnchantEntry> distinctValid,
            List<RawEnchantEntry> preservedUnregistered
    ) {
        if (tag.hasKey(TAG_ENCH, Constants.NBT.TAG_LIST)) {
            readListEntries(TAG_ENCH, tag.getTagList(TAG_ENCH, Constants.NBT.TAG_COMPOUND), resolver, distinctValid, preservedUnregistered);
        }
        if (tag.hasKey(TAG_STORED_ENCHANTMENTS, Constants.NBT.TAG_LIST)) {
            readListEntries(TAG_STORED_ENCHANTMENTS, tag.getTagList(TAG_STORED_ENCHANTMENTS, Constants.NBT.TAG_COMPOUND), resolver, distinctValid, preservedUnregistered);
        }
    }

    private static void readListEntries(
            String sourceTag,
            NBTTagList list,
            EnchantmentResolver resolver,
            Map<Integer, RawEnchantEntry> distinctValid,
            List<RawEnchantEntry> preservedUnregistered
    ) {
        int count = list.tagCount();
        for (int i = 0; i < count; i++) {
            NBTTagCompound compound = list.getCompoundTagAt(i);
            if (!compound.hasKey("id", Constants.NBT.TAG_ANY_NUMERIC)) {
                preservedUnregistered.add(new RawEnchantEntry(sourceTag, -1, (short) 0, null, compound.copy()));
                continue;
            }
            int id = compound.getShort("id") & 0xFFFF;
            short level = compound.getShort("lvl");
            String name = resolver.resolveRegistryName(id);
            if (name == null) {
                preservedUnregistered.add(new RawEnchantEntry(sourceTag, id, level, null, compound.copy()));
                continue;
            }
            Integer key = Integer.valueOf(id);
            RawEnchantEntry existing = distinctValid.get(key);
            if (existing == null || level > existing.level) {
                distinctValid.put(key, new RawEnchantEntry(sourceTag, id, level, name, compound.copy()));
            }
        }
    }

    private static void appendToTagList(RawEnchantEntry entry, NBTTagList enchList, NBTTagList storedList) {
        if (TAG_STORED_ENCHANTMENTS.equals(entry.sourceTag)) {
            storedList.appendTag(entry.originalCompound);
        } else {
            enchList.appendTag(entry.originalCompound);
        }
    }

    public static String describeItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "minecraft:air";
        }
        ResourceLocation registryName = stack.getItem().getRegistryName();
        return registryName != null ? registryName.toString() : stack.getItem().getClass().getSimpleName();
    }

    public static List<String> formatEnchantmentDataList(List<EnchantmentData> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> formatted = new ArrayList<String>(list.size());
        for (EnchantmentData data : list) {
            if (data == null || data.enchantment == null) {
                continue;
            }
            formatted.add(formatEnchantmentData(data));
        }
        return formatted;
    }

    public static List<String> formatRemovedEnchantmentDataList(List<EnchantmentData> original, List<EnchantmentData> kept) {
        if (original == null || original.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> removed = new ArrayList<String>();
        for (EnchantmentData data : original) {
            if (data != null && data.enchantment != null && (kept == null || !kept.contains(data))) {
                removed.add(formatEnchantmentData(data));
            }
        }
        return removed;
    }

    private static String formatEnchantmentData(EnchantmentData data) {
        ResourceLocation id = data.enchantment.getRegistryName();
        String name = id != null ? id.toString() : "id:" + Enchantment.getEnchantmentID(data.enchantment);
        return name + "@" + data.enchantmentLevel;
    }
}
