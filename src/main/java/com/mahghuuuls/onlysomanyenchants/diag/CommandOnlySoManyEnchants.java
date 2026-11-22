package com.mahghuuuls.onlysomanyenchants.diag;

import com.mahghuuuls.onlysomanyenchants.core.InspectionReport;
import com.mahghuuuls.onlysomanyenchants.core.ItemEnchantmentSanitizer;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public final class CommandOnlySoManyEnchants extends CommandBase {

    @Override
    public String getName() {
        return "onlysomanyenchants";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/onlysomanyenchants inspect";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 1 || !"inspect".equalsIgnoreCase(args[0])) {
            throw new WrongUsageException(getUsage(sender));
        }

        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        ItemStack held = player.getHeldItemMainhand();
        if (held.isEmpty()) {
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Only So Many Enchants: No item held in main hand."));
            return;
        }

        InspectionReport report = ItemEnchantmentSanitizer.inspectStack(held);
        String statusText = report.isOverCap() ? (TextFormatting.RED + "OVER CAP") : (TextFormatting.GREEN + "within cap");
        sender.sendMessage(new TextComponentString(
                "Item: " + report.getItemId()
                        + " | Distinct Enchantments: " + report.getDistinctCount()
                        + " / " + report.getActiveCap()
                        + " (" + statusText + TextFormatting.RESET + ")"
        ));

        if (report.getEntries().isEmpty()) {
            sender.sendMessage(new TextComponentString("  Enchantments: none"));
            return;
        }

        for (InspectionReport.Entry entry : report.getEntries()) {
            sender.sendMessage(new TextComponentString(
                    "  - " + entry.getRegistryName()
                            + " lvl " + entry.getLevel()
                            + " [" + entry.getSourceTag() + "]"
            ));
        }
    }

    @Override
    public List<String> getTabCompletions(
            MinecraftServer server,
            ICommandSender sender,
            String[] args,
            @Nullable BlockPos targetPos
    ) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "inspect");
        }
        return Collections.emptyList();
    }
}
