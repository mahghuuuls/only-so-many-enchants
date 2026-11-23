package com.mahghuuuls.onlysomanyenchants.network;

import com.mahghuuuls.onlysomanyenchants.Tags;
import com.mahghuuuls.onlysomanyenchants.core.EnchantmentPolicy;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.ContainerRepair;
import net.minecraft.network.play.server.SPacketChat;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

public final class AnvilFeedbackTracker {

    public static final long UNMODDED_ACTION_BAR_COOLDOWN_MS = 4000L;

    public interface FeedbackDispatcher {
        boolean hasClientMod(UUID playerId, EntityPlayerMP player);

        void sendModdedPacket(UUID playerId, EntityPlayerMP player, int windowId, boolean active, int attemptedCount, int maxAllowed);

        void sendUnmoddedActionBar(UUID playerId, EntityPlayerMP player, String message);
    }

    private static final class PlayerState {
        final int windowId;
        final boolean active;
        final int attemptedCount;
        final int maxAllowed;
        final long lastUnmoddedSentMillis;

        PlayerState(int windowId, boolean active, int attemptedCount, int maxAllowed, long lastUnmoddedSentMillis) {
            this.windowId = windowId;
            this.active = active;
            this.attemptedCount = attemptedCount;
            this.maxAllowed = maxAllowed;
            this.lastUnmoddedSentMillis = lastUnmoddedSentMillis;
        }
    }

    private static final FeedbackDispatcher DEFAULT_DISPATCHER = new FeedbackDispatcher() {
        @Override
        public boolean hasClientMod(UUID playerId, EntityPlayerMP player) {
            if (player == null || player.connection == null || player.connection.netManager == null) {
                return false;
            }
            if (player.connection.netManager.isLocalChannel()) {
                return true;
            }
            NetworkDispatcher dispatcher = NetworkDispatcher.get(player.connection.netManager);
            if (dispatcher == null) {
                return false;
            }
            Map<String, String> modList = dispatcher.getModList();
            return modList != null && modList.containsKey(Tags.MOD_ID);
        }

        @Override
        public void sendModdedPacket(UUID playerId, EntityPlayerMP player, int windowId, boolean active, int attemptedCount, int maxAllowed) {
            NetworkHandler.sendAnvilCapRejection(player, windowId, active, attemptedCount, maxAllowed);
        }

        @Override
        public void sendUnmoddedActionBar(UUID playerId, EntityPlayerMP player, String message) {
            if (player != null && player.connection != null) {
                player.connection.sendPacket(new SPacketChat(new TextComponentString(message), ChatType.GAME_INFO));
            }
        }
    };

    private static final AnvilFeedbackTracker INSTANCE = new AnvilFeedbackTracker(
            DEFAULT_DISPATCHER,
            System::currentTimeMillis,
            UNMODDED_ACTION_BAR_COOLDOWN_MS
    );

    private final FeedbackDispatcher dispatcher;
    private final LongSupplier clockMillis;
    private final long unmoddedCooldownMs;
    private final Map<UUID, PlayerState> states = new ConcurrentHashMap<UUID, PlayerState>();

    public AnvilFeedbackTracker(FeedbackDispatcher dispatcher, LongSupplier clockMillis, long unmoddedCooldownMs) {
        this.dispatcher = dispatcher;
        this.clockMillis = clockMillis;
        this.unmoddedCooldownMs = Math.max(0L, unmoddedCooldownMs);
    }

    public static AnvilFeedbackTracker get() {
        return INSTANCE;
    }

    public void reportRejection(EntityPlayerMP player, int windowId, int attemptedCount, int maxAllowed) {
        if (player == null) {
            return;
        }
        reportRejection(player.getUniqueID(), player, windowId, attemptedCount, maxAllowed);
    }

    public void reportRejection(UUID playerId, EntityPlayerMP player, int windowId, int attemptedCount, int maxAllowed) {
        if (playerId == null) {
            return;
        }
        PlayerState previous = states.get(playerId);
        boolean sameActiveState = previous != null
                && previous.active
                && previous.windowId == windowId
                && previous.attemptedCount == attemptedCount
                && previous.maxAllowed == maxAllowed;

        if (dispatcher.hasClientMod(playerId, player)) {
            if (!sameActiveState) {
                states.put(playerId, new PlayerState(windowId, true, attemptedCount, maxAllowed, 0L));
                dispatcher.sendModdedPacket(playerId, player, windowId, true, attemptedCount, maxAllowed);
            }
            return;
        }

        long now = clockMillis.getAsLong();
        boolean cooldownElapsed = previous == null || (now - previous.lastUnmoddedSentMillis) >= unmoddedCooldownMs;
        if (!sameActiveState || cooldownElapsed) {
            states.put(playerId, new PlayerState(windowId, true, attemptedCount, maxAllowed, now));
            dispatcher.sendUnmoddedActionBar(playerId, player, EnchantmentPolicy.formatCapRejectionMessage(attemptedCount, maxAllowed));
        }
    }

    public void clearRejection(EntityPlayerMP player, int windowId) {
        if (player == null) {
            return;
        }
        clearRejection(player.getUniqueID(), player, windowId);
    }

    public void clearRejection(UUID playerId, EntityPlayerMP player, int windowId) {
        if (playerId == null) {
            return;
        }
        PlayerState previous = states.get(playerId);
        if (previous == null || !previous.active || previous.windowId != windowId) {
            return;
        }
        states.remove(playerId);
        if (dispatcher.hasClientMod(playerId, player)) {
            dispatcher.sendModdedPacket(playerId, player, windowId, false, 0, 0);
        }
    }

    public void clearPlayer(UUID playerId) {
        if (playerId != null) {
            states.remove(playerId);
        }
    }

    public boolean hasActiveRejection(UUID playerId) {
        PlayerState state = playerId != null ? states.get(playerId) : null;
        return state != null && state.active;
    }

    public static final class LifecycleHandler {
        @SubscribeEvent
        public void onContainerClose(PlayerContainerEvent.Close event) {
            if (event.getEntityPlayer() instanceof EntityPlayerMP && event.getContainer() instanceof ContainerRepair) {
                INSTANCE.clearRejection((EntityPlayerMP) event.getEntityPlayer(), event.getContainer().windowId);
            }
        }

        @SubscribeEvent
        public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
            if (event.player != null) {
                INSTANCE.clearPlayer(event.player.getUniqueID());
            }
        }
    }
}
