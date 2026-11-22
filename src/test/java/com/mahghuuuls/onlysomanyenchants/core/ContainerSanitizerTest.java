package com.mahghuuuls.onlysomanyenchants.core;

import com.mahghuuuls.onlysomanyenchants.diag.DiagnosticLogger;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContainerSanitizerTest {

    private static final EnchantmentResolver TEST_RESOLVER = new EnchantmentResolver() {
        @Override
        public String resolveRegistryName(int enchantmentId) {
            if (enchantmentId == 16) {
                return "minecraft:sharpness";
            }
            if (enchantmentId == 21) {
                return "minecraft:looting";
            }
            if (enchantmentId == 34) {
                return "minecraft:unbreaking";
            }
            return null;
        }
    };

    @BeforeAll
    static void bootstrapRegistries() {
        Bootstrap.register();
    }

    private static ItemStack overLimitSword() {
        ItemStack stack = new ItemStack(Items.DIAMOND_SWORD);
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList ench = new NBTTagList();
        NBTTagCompound e1 = new NBTTagCompound();
        e1.setShort("id", (short) 16);
        e1.setShort("lvl", (short) 4);
        NBTTagCompound e2 = new NBTTagCompound();
        e2.setShort("id", (short) 21);
        e2.setShort("lvl", (short) 2);
        ench.appendTag(e1);
        ench.appendTag(e2);
        tag.setTag(ItemEnchantmentSanitizer.TAG_ENCH, ench);
        stack.setTagCompound(tag);
        return stack;
    }

    @Test
    void sanitizeContainerWritesBackSlotItemHandlerAndMarksInventoryDirty() {
        final AtomicInteger handlerContentsChanged = new AtomicInteger(0);
        ItemStackHandler handler = new ItemStackHandler(1) {
            @Override
            protected void onContentsChanged(int slot) {
                handlerContentsChanged.incrementAndGet();
            }
        };
        handler.setStackInSlot(0, overLimitSword());
        handlerContentsChanged.set(0);

        final AtomicInteger inventoryDirtyCount = new AtomicInteger(0);
        InventoryBasic vanillaInventory = new InventoryBasic("test", false, 1) {
            @Override
            public void markDirty() {
                super.markDirty();
                inventoryDirtyCount.incrementAndGet();
            }
        };
        vanillaInventory.setInventorySlotContents(0, overLimitSword());
        inventoryDirtyCount.set(0);

        Container container = new Container() {
            {
                addSlotToContainer(new SlotItemHandler(handler, 0, 0, 0));
                addSlotToContainer(new Slot(vanillaInventory, 0, 0, 0));
            }

            @Override
            public boolean canInteractWith(EntityPlayer playerIn) {
                return true;
            }
        };

        int modified = ContainerSanitizer.sanitizeContainer(container, 1, "test_open", new Random(5L), TEST_RESOLVER, null);

        assertEquals(2, modified);
        assertEquals(1, handlerContentsChanged.get(), "SlotItemHandler must persist through setStackInSlot / onContentsChanged");
        assertTrue(inventoryDirtyCount.get() >= 1, "Vanilla IInventory slot must call markDirty()");
        assertEquals(1, ItemEnchantmentSanitizer.countDistinctEnchantments(handler.getStackInSlot(0).getTagCompound(), TEST_RESOLVER));
        assertEquals(1, ItemEnchantmentSanitizer.countDistinctEnchantments(vanillaInventory.getStackInSlot(0).getTagCompound(), TEST_RESOLVER));
    }

    @Test
    void sanitizeItemHandlerHandlesReadOnlyModifiableHandlerWithoutCrashing() {
        final ItemStack backed = overLimitSword();
        IItemHandlerModifiable readOnlyHandler = new IItemHandlerModifiable() {
            @Override
            public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
                throw new UnsupportedOperationException("read-only view");
            }

            @Override
            public int getSlots() {
                return 1;
            }

            @Nonnull
            @Override
            public ItemStack getStackInSlot(int slot) {
                return backed;
            }

            @Nonnull
            @Override
            public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
                return stack;
            }

            @Nonnull
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return 64;
            }
        };

        List<String> logs = new ArrayList<String>();
        DiagnosticLogger diag = new DiagnosticLogger(() -> true, () -> 1000L, logs::add, 0L);

        int modified = ContainerSanitizer.sanitizeItemHandler(
                readOnlyHandler, null, 1, "readonly_test", new Random(13L), TEST_RESOLVER, diag, 0
        );

        assertEquals(1, modified);
        assertEquals(1, ItemEnchantmentSanitizer.countDistinctEnchantments(backed.getTagCompound(), TEST_RESOLVER));
        assertTrue(logs.stream().anyMatch(line -> line.contains("[OSME][WARN]") && line.contains("Read-only IItemHandler")));
    }

    @Test
    void sanitizeMerchantRecipesMutatesSellingStackInPlace() {
        MerchantRecipeList list = new MerchantRecipeList();
        ItemStack sell = overLimitSword();
        list.add(new MerchantRecipe(new ItemStack(Items.EMERALD, 10), sell));

        int modified = ContainerSanitizer.sanitizeMerchantRecipes(list, "villager_test", new Random(21L));

        assertEquals(1, modified);
        assertEquals(1, ItemEnchantmentSanitizer.countDistinctEnchantments(list.get(0).getItemToSell()));
    }
}
