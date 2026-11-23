package com.mahghuuuls.onlysomanyenchants.diag;

import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandOnlySoManyEnchantsTest {

    private static ICommandSender senderWithPermissionLevel(final int permissionLevel) {
        return new ICommandSender() {
            @Override
            public String getName() {
                return "TestSender";
            }

            @Override
            public boolean canUseCommand(int permLevel, String commandName) {
                return permissionLevel >= permLevel;
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
            }
        };
    }

    @Test
    void exposesExpectedNamePermissionUsageAndTabCompletions() {
        CommandOnlySoManyEnchants command = new CommandOnlySoManyEnchants();

        assertEquals("onlysomanyenchants", command.getName());
        assertEquals(2, command.getRequiredPermissionLevel());
        assertEquals("/onlysomanyenchants inspect", command.getUsage(null));

        List<String> firstArgCompletions = command.getTabCompletions(null, null, new String[]{"in"}, null);
        assertEquals(1, firstArgCompletions.size());
        assertEquals("inspect", firstArgCompletions.get(0));

        List<String> secondArgCompletions = command.getTabCompletions(null, null, new String[]{"inspect", ""}, null);
        assertTrue(secondArgCompletions.isEmpty());
    }

    @Test
    void checkPermissionDeniesNonOperatorsAndAllowsLevelTwoOperators() {
        CommandOnlySoManyEnchants command = new CommandOnlySoManyEnchants();

        assertFalse(command.checkPermission(null, senderWithPermissionLevel(0)));
        assertFalse(command.checkPermission(null, senderWithPermissionLevel(1)));
        assertTrue(command.checkPermission(null, senderWithPermissionLevel(2)));
        assertTrue(command.checkPermission(null, senderWithPermissionLevel(4)));
    }

    @Test
    void rejectsMissingOrUnknownSubcommandWithWrongUsageException() {
        CommandOnlySoManyEnchants command = new CommandOnlySoManyEnchants();

        assertThrows(WrongUsageException.class, () -> command.execute(null, null, new String[0]));
        assertThrows(WrongUsageException.class, () -> command.execute(null, null, new String[]{"unknown"}));
        assertThrows(WrongUsageException.class, () -> command.execute(null, null, new String[]{"inspect", "extra"}));
    }
}
