package com.mahghuuuls.onlysomanyenchants.diag;

import net.minecraft.command.WrongUsageException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandOnlySoManyEnchantsTest {

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
    void rejectsMissingOrUnknownSubcommandWithWrongUsageException() {
        CommandOnlySoManyEnchants command = new CommandOnlySoManyEnchants();

        assertThrows(WrongUsageException.class, () -> command.execute(null, null, new String[0]));
        assertThrows(WrongUsageException.class, () -> command.execute(null, null, new String[]{"unknown"}));
        assertThrows(WrongUsageException.class, () -> command.execute(null, null, new String[]{"inspect", "extra"}));
    }
}
