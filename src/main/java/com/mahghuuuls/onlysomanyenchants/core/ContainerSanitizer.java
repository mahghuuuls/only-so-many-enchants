package com.mahghuuuls.onlysomanyenchants.core;

import com.mahghuuuls.onlysomanyenchants.config.ModConfig;
import com.mahghuuuls.onlysomanyenchants.diag.DiagnosticLogger;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityLockableLoot;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Random;
import java.util.Set;

public final class ContainerSanitizer {

    private ContainerSanitizer() {
    }

    public static boolean sanitizeStackAndNested(ItemStack stack, String sourcePath, Random random) {
        return sanitizeStackAndNested(stack, ModConfig.getEffectiveCap(), sourcePath, random, EnchantmentResolver.FORGE, DiagnosticLogger.get(), 0);
    }

    public static boolean sanitizeStackAndNested(ItemStack stack, String sourcePath, Random random, int depth) {
        return sanitizeStackAndNested(stack, ModConfig.getEffectiveCap(), sourcePath, random, EnchantmentResolver.FORGE, DiagnosticLogger.get(), depth);
    }

    public static boolean sanitizeStackAndNested(
            ItemStack stack,
            int maxDistinct,
            String sourcePath,
            Random random,
            EnchantmentResolver resolver,
            DiagnosticLogger logger,
            int depth
    ) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        boolean modified = ItemEnchantmentSanitizer.sanitizeStack(stack, maxDistinct, sourcePath, random, resolver, logger).isModified();
        if (depth < ItemEnchantmentSanitizer.MAX_NESTED_DEPTH) {
            // Sanitize item capability before raw NBT so capability-backed containers sync once without double-trimming.
            if (stack.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
                try {
                    IItemHandler itemHandler = stack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
                    if (itemHandler != null) {
                        modified |= sanitizeItemHandler(itemHandler, null, maxDistinct, sourcePath + ":item_cap", random, resolver, logger, depth + 1) > 0;
                    }
                } catch (RuntimeException ex) {
                    if (logger != null) {
                        logger.logBoundaryWarning(sourcePath, "Nested item capability read failed: " + ex.getClass().getSimpleName());
                    }
                }
            }
            if (stack.hasTagCompound()) {
                modified |= ItemEnchantmentSanitizer.sanitizeNestedNbt(
                        stack.getTagCompound(), maxDistinct, sourcePath, random, resolver, logger, depth
                );
            }
        }
        return modified;
    }

    public static int sanitizeContainer(Container container, String sourcePath, Random random) {
        return sanitizeContainer(container, ModConfig.getEffectiveCap(), sourcePath, random, EnchantmentResolver.FORGE, DiagnosticLogger.get());
    }

    public static int sanitizeContainer(
            Container container,
            int maxDistinct,
            String sourcePath,
            Random random,
            EnchantmentResolver resolver,
            DiagnosticLogger logger
    ) {
        if (container == null || container.inventorySlots == null) {
            return 0;
        }
        int modifiedCount = 0;
        Set<IInventory> dirtyInventories = Collections.newSetFromMap(new IdentityHashMap<IInventory, Boolean>());
        for (int i = 0; i < container.inventorySlots.size(); i++) {
            Slot slot = container.inventorySlots.get(i);
            if (slot == null) {
                continue;
            }
            ItemStack stack = slot.getStack();
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            if (sanitizeStackAndNested(stack, maxDistinct, sourcePath, random, resolver, logger, 0)) {
                modifiedCount++;
                if (slot instanceof SlotItemHandler) {
                    SlotItemHandler handlerSlot = (SlotItemHandler) slot;
                    writeBackToItemHandler(handlerSlot.getItemHandler(), handlerSlot.getSlotIndex(), stack, sourcePath, logger);
                } else {
                    try {
                        slot.onSlotChanged();
                    } catch (RuntimeException ex) {
                        if (logger != null) {
                            logger.logBoundaryWarning(sourcePath, "Slot onSlotChanged failed: " + ex.getClass().getSimpleName());
                        }
                    }
                    if (slot.inventory != null) {
                        dirtyInventories.add(slot.inventory);
                    }
                }
            }
        }
        for (IInventory inventory : dirtyInventories) {
            try {
                inventory.markDirty();
            } catch (RuntimeException ex) {
                if (logger != null) {
                    logger.logBoundaryWarning(sourcePath, "Inventory markDirty failed: " + ex.getClass().getSimpleName());
                }
            }
        }
        return modifiedCount;
    }

    public static int sanitizeInventory(IInventory inventory, String sourcePath, Random random) {
        return sanitizeInventory(inventory, ModConfig.getEffectiveCap(), sourcePath, random, EnchantmentResolver.FORGE, DiagnosticLogger.get());
    }

    public static int sanitizeInventory(
            IInventory inventory,
            int maxDistinct,
            String sourcePath,
            Random random,
            EnchantmentResolver resolver,
            DiagnosticLogger logger
    ) {
        if (inventory == null) {
            return 0;
        }
        int modifiedCount = 0;
        int size = inventory.getSizeInventory();
        for (int i = 0; i < size; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            if (sanitizeStackAndNested(stack, maxDistinct, sourcePath, random, resolver, logger, 0)) {
                modifiedCount++;
            }
        }
        if (modifiedCount > 0) {
            inventory.markDirty();
        }
        return modifiedCount;
    }

    public static int sanitizeItemHandler(IItemHandler handler, TileEntity ownerTile, String sourcePath, Random random) {
        return sanitizeItemHandler(handler, ownerTile, ModConfig.getEffectiveCap(), sourcePath, random, EnchantmentResolver.FORGE, DiagnosticLogger.get(), 0);
    }

    public static int sanitizeItemHandler(
            IItemHandler handler,
            TileEntity ownerTile,
            int maxDistinct,
            String sourcePath,
            Random random,
            EnchantmentResolver resolver,
            DiagnosticLogger logger,
            int depth
    ) {
        if (handler == null || depth > ItemEnchantmentSanitizer.MAX_NESTED_DEPTH) {
            return 0;
        }
        int modifiedCount = 0;
        int slots = handler.getSlots();
        for (int i = 0; i < slots; i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            if (sanitizeStackAndNested(stack, maxDistinct, sourcePath, random, resolver, logger, depth)) {
                modifiedCount++;
                writeBackToItemHandler(handler, i, stack, sourcePath, logger);
            }
        }
        if (modifiedCount > 0 && ownerTile != null) {
            markTileAndChunkDirty(ownerTile);
        }
        return modifiedCount;
    }

    public static int sanitizeChunkTileEntities(Chunk chunk, Random random) {
        if (chunk == null || chunk.getWorld() == null || chunk.getWorld().isRemote) {
            return 0;
        }
        int totalModified = 0;
        for (TileEntity tileEntity : chunk.getTileEntityMap().values()) {
            if (tileEntity == null || tileEntity.isInvalid()) {
                continue;
            }
            // Skip ungenerated loot containers so player Luck and loot-table generation remain untouched until opened.
            if (tileEntity instanceof TileEntityLockableLoot && ((TileEntityLockableLoot) tileEntity).getLootTable() != null) {
                continue;
            }
            boolean handled = false;
            try {
                if (tileEntity.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
                    IItemHandler handler = tileEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
                    if (handler != null) {
                        totalModified += sanitizeItemHandler(handler, tileEntity, "chunk_load", random);
                        handled = true;
                    }
                }
            } catch (RuntimeException ex) {
                DiagnosticLogger.get().logBoundaryWarning("chunk_load", "TileEntity capability read failed on " + tileEntity.getClass().getSimpleName());
            }
            if (!handled && tileEntity instanceof IInventory) {
                int modified = sanitizeInventory((IInventory) tileEntity, "chunk_load", random);
                if (modified > 0) {
                    markTileAndChunkDirty(tileEntity);
                    totalModified += modified;
                }
            }
        }
        return totalModified;
    }

    public static int sanitizeLivingEquipment(EntityLivingBase entity, String sourcePath, Random random) {
        if (entity == null || entity.world == null || entity.world.isRemote) {
            return 0;
        }
        int modifiedCount = 0;
        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
            ItemStack equipped = entity.getItemStackFromSlot(slot);
            if (equipped == null || equipped.isEmpty()) {
                continue;
            }
            if (sanitizeStackAndNested(equipped, sourcePath, random, 0)) {
                modifiedCount++;
            }
        }
        return modifiedCount;
    }

    public static int sanitizeMerchantRecipes(MerchantRecipeList recipes, String sourcePath, Random random) {
        if (recipes == null || recipes.isEmpty()) {
            return 0;
        }
        int modifiedCount = 0;
        for (int i = 0; i < recipes.size(); i++) {
            MerchantRecipe recipe = recipes.get(i);
            if (recipe == null) {
                continue;
            }
            ItemStack sell = recipe.getItemToSell();
            if (sell != null && !sell.isEmpty() && sanitizeStackAndNested(sell, sourcePath, random, 0)) {
                modifiedCount++;
            }
        }
        return modifiedCount;
    }

    private static void writeBackToItemHandler(
            IItemHandler handler,
            int slotIndex,
            ItemStack stack,
            String sourcePath,
            DiagnosticLogger logger
    ) {
        if (handler instanceof IItemHandlerModifiable) {
            try {
                ((IItemHandlerModifiable) handler).setStackInSlot(slotIndex, stack);
            } catch (UnsupportedOperationException ex) {
                if (logger != null) {
                    logger.logBoundaryWarning(sourcePath, "Read-only IItemHandler refused setStackInSlot on " + handler.getClass().getSimpleName());
                }
            } catch (RuntimeException ex) {
                if (logger != null) {
                    logger.logBoundaryWarning(sourcePath, "IItemHandler setStackInSlot failed on " + handler.getClass().getSimpleName() + ": " + ex.getClass().getSimpleName());
                }
            }
        }
    }

    private static void markTileAndChunkDirty(TileEntity tileEntity) {
        tileEntity.markDirty();
        if (tileEntity.hasWorld() && tileEntity.getPos() != null) {
            tileEntity.getWorld().markChunkDirty(tileEntity.getPos(), tileEntity);
        }
    }
}
