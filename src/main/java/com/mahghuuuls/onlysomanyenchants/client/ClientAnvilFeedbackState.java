package com.mahghuuuls.onlysomanyenchants.client;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class ClientAnvilFeedbackState {

    private static int activeWindowId = -1;
    private static boolean active = false;
    private static int attemptedCount = 0;
    private static int maxAllowed = 1;

    private ClientAnvilFeedbackState() {
    }

    public static void update(int windowId, boolean isActive, int attempted, int cap) {
        if (!isActive) {
            if (activeWindowId == windowId || activeWindowId == -1) {
                clear();
            }
            return;
        }
        activeWindowId = windowId;
        active = true;
        attemptedCount = attempted;
        maxAllowed = Math.max(1, cap);
    }

    public static void clear() {
        activeWindowId = -1;
        active = false;
        attemptedCount = 0;
        maxAllowed = 1;
    }

    public static boolean isActiveFor(int windowId) {
        return active && activeWindowId == windowId;
    }

    public static int getAttemptedCount() {
        return attemptedCount;
    }

    public static int getMaxAllowed() {
        return maxAllowed;
    }
}
