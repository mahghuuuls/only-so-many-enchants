package com.mahghuuuls.onlysomanyenchants.event;

import com.mahghuuuls.onlysomanyenchants.config.ModConfig;
import com.mahghuuuls.onlysomanyenchants.core.AnvilCapVerdict;
import com.mahghuuuls.onlysomanyenchants.core.EnchantmentPolicy;
import com.mahghuuuls.onlysomanyenchants.core.ItemEnchantmentSanitizer;
import com.mahghuuuls.onlysomanyenchants.diag.DiagnosticLogger;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandResultStats;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.NumberInvalidException;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.IntSupplier;

public final class CommandEnchantGuardHandler {

    interface CommandContextResolver {
        List<ItemStack> resolveTargetHeldItems(ICommandSender sender, String targetArg) throws CommandException;

        Enchantment resolveEnchantment(String raw);

        void validateOptionalLevel(Enchantment enchantment, String[] args) throws CommandException;

        boolean canApplyToStack(Enchantment enchantment, ItemStack held);

        boolean hasEnchantmentOnStack(ItemStack held, Enchantment enchantment);

        boolean isCompatibleWithExisting(ItemStack held, Enchantment candidate);
    }

    private static final CommandContextResolver DEFAULT_RESOLVER = new CommandContextResolver() {
        @Override
        public List<ItemStack> resolveTargetHeldItems(ICommandSender sender, String targetArg) throws CommandException {
            List<EntityLivingBase> targets;
            if (EntitySelector.isSelector(targetArg)) {
                targets = EntitySelector.matchEntities(sender, targetArg, EntityLivingBase.class);
            } else {
                MinecraftServer server = sender.getServer();
                if (server == null) {
                    return Collections.emptyList();
                }
                EntityLivingBase single = CommandBase.getEntity(server, sender, targetArg, EntityLivingBase.class);
                targets = single != null
                        ? Collections.singletonList(single)
                        : Collections.<EntityLivingBase>emptyList();
            }
            if (targets.isEmpty()) {
                return Collections.emptyList();
            }
            List<ItemStack> heldItems = new ArrayList<ItemStack>(targets.size());
            for (EntityLivingBase target : targets) {
                if (target != null) {
                    heldItems.add(target.getHeldItemMainhand());
                }
            }
            return heldItems;
        }

        @Override
        public Enchantment resolveEnchantment(String raw) {
            try {
                return Enchantment.getEnchantmentByID(CommandBase.parseInt(raw, 0));
            } catch (NumberInvalidException ignored) {
                return Enchantment.getEnchantmentByLocation(raw);
            }
        }

        @Override
        public void validateOptionalLevel(Enchantment enchantment, String[] args) throws CommandException {
            if (args.length >= 3) {
                CommandBase.parseInt(args[2], enchantment.getMinLevel(), enchantment.getMaxLevel());
            }
        }

        @Override
        public boolean canApplyToStack(Enchantment enchantment, ItemStack held) {
            return enchantment.canApply(held);
        }

        @Override
        public boolean hasEnchantmentOnStack(ItemStack held, Enchantment enchantment) {
            return ItemEnchantmentSanitizer.hasEnchantment(held, enchantment);
        }

        @Override
        public boolean isCompatibleWithExisting(ItemStack held, Enchantment candidate) {
            return isCompatibleWithExistingEnchantments(held, candidate);
        }
    };

    static boolean isCompatibleWithExistingEnchantments(ItemStack held, Enchantment candidate) {
        if (!held.hasTagCompound()) {
            return true;
        }
        NBTTagList tagList = held.getEnchantmentTagList();
        for (int i = 0; i < tagList.tagCount(); i++) {
            int existingId = tagList.getCompoundTagAt(i).getShort("id");
            Enchantment existing = Enchantment.getEnchantmentByID(existingId);
            if (existing != null && !candidate.isCompatibleWith(existing)) {
                return false;
            }
        }
        return true;
    }

    private final CommandContextResolver resolver;
    private final IntSupplier capSupplier;

    public CommandEnchantGuardHandler() {
        this(DEFAULT_RESOLVER, ModConfig::getEffectiveCap);
    }

    CommandEnchantGuardHandler(CommandContextResolver resolver, IntSupplier capSupplier) {
        this.resolver = resolver;
        this.capSupplier = capSupplier;
    }

    public static AnvilCapVerdict evaluateCommandEnchant(int currentDistinct, boolean alreadyHasEnchantment, int maxDistinct) {
        int resultingDistinct = alreadyHasEnchantment ? currentDistinct : (currentDistinct + 1);
        return EnchantmentPolicy.evaluateAnvilOutput(resultingDistinct, maxDistinct);
    }

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        if (event == null || event.isCanceled()) {
            return;
        }
        ICommand command = event.getCommand();
        if (command == null || !"enchant".equalsIgnoreCase(command.getName())) {
            return;
        }
        String[] args = event.getParameters();
        if (args == null || args.length < 2) {
            return;
        }
        ICommandSender sender = event.getSender();
        if (sender == null) {
            return;
        }

        Enchantment enchantment = resolver.resolveEnchantment(args[1]);
        if (enchantment == null) {
            return;
        }

        List<ItemStack> targetHeldItems;
        try {
            resolver.validateOptionalLevel(enchantment, args);
            targetHeldItems = resolver.resolveTargetHeldItems(sender, args[0]);
        } catch (CommandException ex) {
            // Let vanilla or Enchantment Control handle invalid targets or levels normally.
            return;
        }

        if (targetHeldItems == null || targetHeldItems.isEmpty()) {
            return;
        }

        int cap = EnchantmentPolicy.clampCap(capSupplier.getAsInt());
        for (ItemStack held : targetHeldItems) {
            if (held == null || held.isEmpty() || !resolver.canApplyToStack(enchantment, held)) {
                continue;
            }
            boolean alreadyHas = resolver.hasEnchantmentOnStack(held, enchantment);
            if (alreadyHas) {
                continue;
            }
            if (!resolver.isCompatibleWithExisting(held, enchantment)) {
                // Ordinarily illegal combination; let vanilla report cantCombine.
                continue;
            }

            int currentDistinct = ItemEnchantmentSanitizer.countDistinctEnchantments(held);
            AnvilCapVerdict verdict = evaluateCommandEnchant(currentDistinct, false, cap);
            if (verdict.isRejected()) {
                rejectCommand(event, sender, held, enchantment, verdict);
                return;
            }
        }
    }

    private static void rejectCommand(
            CommandEvent event,
            ICommandSender sender,
            ItemStack held,
            Enchantment enchantment,
            AnvilCapVerdict verdict
    ) {
        if (ModConfig.isDiagnosticMode()) {
            List<String> attempted = new ArrayList<String>(ItemEnchantmentSanitizer.formatEnchantmentEntries(held));
            String addedName = enchantment.getRegistryName() != null
                    ? enchantment.getRegistryName().toString()
                    : String.valueOf(Enchantment.getEnchantmentID(enchantment));
            attempted.add("+" + addedName);
            DiagnosticLogger.get().logRejection(
                    "command_enchant",
                    ItemEnchantmentSanitizer.describeItem(held),
                    verdict.getMaxAllowed(),
                    verdict.getAttemptedCount(),
                    attempted,
                    "cap_exceeded"
            );
        }

        String message = EnchantmentPolicy.formatCapRejectionMessage(verdict.getAttemptedCount(), verdict.getMaxAllowed());
        sender.setCommandStat(CommandResultStats.Type.AFFECTED_ITEMS, 0);
        TextComponentString errorMessage = new TextComponentString(message);
        errorMessage.getStyle().setColor(TextFormatting.RED);
        sender.sendMessage(errorMessage);
        event.setCanceled(true);
        event.setException(new CommandException(message));
    }
}
