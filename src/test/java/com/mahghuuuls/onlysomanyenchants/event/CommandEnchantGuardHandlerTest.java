package com.mahghuuuls.onlysomanyenchants.event;

import com.mahghuuuls.onlysomanyenchants.core.AnvilCapVerdict;
import com.mahghuuuls.onlysomanyenchants.core.ItemEnchantmentSanitizer;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandEnchant;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandResultStats;
import net.minecraft.command.ICommandSender;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.event.CommandEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandEnchantGuardHandlerTest {

    @BeforeAll
    static void bootstrapRegistries() {
        Bootstrap.register();
    }

    private static final class RecordingSender implements ICommandSender {
        final List<ITextComponent> messages = new ArrayList<ITextComponent>();
        CommandResultStats.Type lastStatType;
        int lastStatAmount = -1;

        @Override
        public String getName() {
            return "Tester";
        }

        @Override
        public boolean canUseCommand(int permLevel, String commandName) {
            return true;
        }

        @Override
        public World getEntityWorld() {
            return null;
        }

        @Override
        public MinecraftServer getServer() {
            return null;
        }

        @Override
        public void sendMessage(ITextComponent component) {
            messages.add(component);
        }

        @Override
        public void setCommandStat(CommandResultStats.Type type, int amount) {
            this.lastStatType = type;
            this.lastStatAmount = amount;
        }
    }

    private static ItemStack makeSwordWith(Enchantment... enchantments) {
        ItemStack stack = new ItemStack(Items.DIAMOND_SWORD);
        for (Enchantment enchantment : enchantments) {
            stack.addEnchantment(enchantment, 4);
        }
        return stack;
    }

    private static CommandEvent cancelableCommandEvent(ICommandSender sender, String... args) {
        return new CommandEvent(new CommandEnchant(), sender, args) {
            @Override
            public boolean isCancelable() {
                return true;
            }
        };
    }

    private static CommandEnchantGuardHandler handlerWithTargets(final List<ItemStack> heldItems, final int cap) {
        return new CommandEnchantGuardHandler(
                new CommandEnchantGuardHandler.CommandContextResolver() {
                    @Override
                    public List<ItemStack> resolveTargetHeldItems(ICommandSender sender, String targetArg) {
                        return heldItems;
                    }

                    @Override
                    public Enchantment resolveEnchantment(String raw) {
                        return Enchantment.getEnchantmentByLocation(raw);
                    }

                    @Override
                    public void validateOptionalLevel(Enchantment ench, String[] args) throws CommandException {
                        if (args.length >= 3) {
                            CommandBase.parseInt(args[2], ench.getMinLevel(), ench.getMaxLevel());
                        }
                    }

                    @Override
                    public boolean canApplyToStack(Enchantment ench, ItemStack held) {
                        return ench.canApply(held);
                    }

                    @Override
                    public boolean hasEnchantmentOnStack(ItemStack held, Enchantment ench) {
                        return ItemEnchantmentSanitizer.hasEnchantment(held, ench);
                    }

                    @Override
                    public boolean isCompatibleWithExisting(ItemStack held, Enchantment candidate) {
                        return CommandEnchantGuardHandler.isCompatibleWithExistingEnchantments(held, candidate);
                    }
                },
                () -> cap
        );
    }

    @Test
    void blocksNewEnchantmentOverCapAndAllowsSameEnchantmentOrUnderCap() {
        AnvilCapVerdict firstEnchant = CommandEnchantGuardHandler.evaluateCommandEnchant(0, false, 1);
        assertFalse(firstEnchant.isRejected());

        AnvilCapVerdict sameEnchantAtCap = CommandEnchantGuardHandler.evaluateCommandEnchant(1, true, 1);
        assertFalse(sameEnchantAtCap.isRejected());

        AnvilCapVerdict newEnchantOverCap = CommandEnchantGuardHandler.evaluateCommandEnchant(1, false, 1);
        assertTrue(newEnchantOverCap.isRejected());
        assertEquals(2, newEnchantOverCap.getAttemptedCount());
        assertEquals(1, newEnchantOverCap.getMaxAllowed());

        AnvilCapVerdict secondEnchantUnderCapTwo = CommandEnchantGuardHandler.evaluateCommandEnchant(1, false, 2);
        assertFalse(secondEnchantUnderCapTwo.isRejected());

        AnvilCapVerdict thirdEnchantOverCapTwo = CommandEnchantGuardHandler.evaluateCommandEnchant(2, false, 2);
        assertTrue(thirdEnchantOverCapTwo.isRejected());
        assertEquals(3, thirdEnchantOverCapTwo.getAttemptedCount());
        assertEquals(2, thirdEnchantOverCapTwo.getMaxAllowed());
    }

    @Test
    void onCommandRejectsOverCapAndNotifiesSenderDirectly() {
        RecordingSender sender = new RecordingSender();
        ItemStack sword = makeSwordWith(Enchantments.SHARPNESS);
        CommandEnchantGuardHandler handler = handlerWithTargets(Collections.singletonList(sword), 1);

        CommandEvent event = cancelableCommandEvent(sender, "@p", "minecraft:unbreaking", "3");
        handler.onCommand(event);

        assertTrue(event.isCanceled());
        assertNotNull(event.getException());
        assertEquals("This would give the item 2 enchantments. The limit is 1.", event.getException().getMessage());
        assertEquals(1, sender.messages.size(), "Must send error message directly so CommandHandler does not fail silently");
        assertEquals("This would give the item 2 enchantments. The limit is 1.", sender.messages.get(0).getUnformattedText());
        assertEquals(TextFormatting.RED, sender.messages.get(0).getStyle().getColor());
        assertEquals(CommandResultStats.Type.AFFECTED_ITEMS, sender.lastStatType);
        assertEquals(0, sender.lastStatAmount);
    }

    @Test
    void onCommandCatchesOverCapTargetInMultiEntitySelector() {
        RecordingSender sender = new RecordingSender();
        ItemStack unenchanted = new ItemStack(Items.DIAMOND_SWORD);
        ItemStack capped = makeSwordWith(Enchantments.SHARPNESS);
        CommandEnchantGuardHandler handler = handlerWithTargets(Arrays.asList(unenchanted, capped), 1);

        CommandEvent event = cancelableCommandEvent(sender, "@a", "minecraft:unbreaking", "1");
        handler.onCommand(event);

        assertTrue(event.isCanceled());
        assertEquals(1, sender.messages.size());
    }

    @Test
    void onCommandLetsOrdinarilyIllegalLevelOrIncompatibleCombinationPassToVanilla() {
        RecordingSender sender = new RecordingSender();
        ItemStack sword = makeSwordWith(Enchantments.SHARPNESS);
        CommandEnchantGuardHandler handler = handlerWithTargets(Collections.singletonList(sword), 1);

        // Out-of-range level (Unbreaking max is 3) must pass through so vanilla reports invalid level.
        CommandEvent badLevel = cancelableCommandEvent(sender, "@p", "minecraft:unbreaking", "99");
        handler.onCommand(badLevel);
        assertFalse(badLevel.isCanceled());
        assertTrue(sender.messages.isEmpty());

        // Incompatible enchantment (Smite on Sharpness sword) must pass through so vanilla reports cantCombine.
        CommandEvent incompatible = cancelableCommandEvent(sender, "@p", "minecraft:smite", "1");
        handler.onCommand(incompatible);
        assertFalse(incompatible.isCanceled());
        assertTrue(sender.messages.isEmpty());

        // Already-present enchantment must pass through.
        CommandEvent sameEnchant = cancelableCommandEvent(sender, "@p", "minecraft:sharpness", "5");
        handler.onCommand(sameEnchant);
        assertFalse(sameEnchant.isCanceled());
        assertTrue(sender.messages.isEmpty());
    }

    @Test
    void hasEnchantmentDetectsBothEnchAndStoredEnchantments() {
        NBTTagCompound swordTag = new NBTTagCompound();
        NBTTagList ench = new NBTTagList();
        NBTTagCompound entry = new NBTTagCompound();
        entry.setShort("id", (short) 16);
        entry.setShort("lvl", (short) 4);
        ench.appendTag(entry);
        swordTag.setTag(ItemEnchantmentSanitizer.TAG_ENCH, ench);

        assertTrue(ItemEnchantmentSanitizer.hasEnchantment(swordTag, 16));
        assertFalse(ItemEnchantmentSanitizer.hasEnchantment(swordTag, 21));

        NBTTagCompound bookTag = new NBTTagCompound();
        NBTTagList stored = new NBTTagList();
        NBTTagCompound storedEntry = new NBTTagCompound();
        storedEntry.setShort("id", (short) 34);
        storedEntry.setShort("lvl", (short) 3);
        stored.appendTag(storedEntry);
        bookTag.setTag(ItemEnchantmentSanitizer.TAG_STORED_ENCHANTMENTS, stored);

        assertTrue(ItemEnchantmentSanitizer.hasEnchantment(bookTag, 34));
        assertFalse(ItemEnchantmentSanitizer.hasEnchantment(bookTag, 16));
    }
}
