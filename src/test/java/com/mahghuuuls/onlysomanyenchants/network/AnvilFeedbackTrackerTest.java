package com.mahghuuuls.onlysomanyenchants.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayerMP;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnvilFeedbackTrackerTest {

    @Test
    void moddedClientReceivesDeduplicatedPacketsAndImmediateClear() {
        List<String> packets = new ArrayList<String>();
        List<String> actionBars = new ArrayList<String>();
        AtomicBoolean modded = new AtomicBoolean(true);
        AtomicLong clock = new AtomicLong(1000L);

        AnvilFeedbackTracker tracker = new AnvilFeedbackTracker(
                new AnvilFeedbackTracker.FeedbackDispatcher() {
                    @Override
                    public boolean hasClientMod(UUID playerId, EntityPlayerMP player) {
                        return modded.get();
                    }

                    @Override
                    public void sendModdedPacket(UUID playerId, EntityPlayerMP player, int windowId, boolean active, int attemptedCount, int maxAllowed) {
                        packets.add(windowId + ":" + active + ":" + attemptedCount + ":" + maxAllowed);
                    }

                    @Override
                    public void sendUnmoddedActionBar(UUID playerId, EntityPlayerMP player, String message) {
                        actionBars.add(message);
                    }
                },
                clock::get,
                4000L
        );

        UUID player = UUID.randomUUID();
        tracker.reportRejection(player, null, 7, 2, 1);
        tracker.reportRejection(player, null, 7, 2, 1);
        assertEquals(1, packets.size(), "Identical active rejection state must be deduplicated for modded clients");
        assertEquals("7:true:2:1", packets.get(0));
        assertTrue(tracker.hasActiveRejection(player));
        assertTrue(actionBars.isEmpty());

        // Changing attempted count sends an updated packet.
        tracker.reportRejection(player, null, 7, 3, 1);
        assertEquals(2, packets.size());
        assertEquals("7:true:3:1", packets.get(1));

        // Mismatched windowId clear must not clear active state or send a packet.
        tracker.clearRejection(player, null, 99);
        assertEquals(2, packets.size());
        assertTrue(tracker.hasActiveRejection(player));

        // Matching windowId clear sends one inactive packet; repeated clears do nothing.
        tracker.clearRejection(player, null, 7);
        tracker.clearRejection(player, null, 7);
        assertEquals(3, packets.size());
        assertEquals("7:false:0:0", packets.get(2));
        assertFalse(tracker.hasActiveRejection(player));
    }

    @Test
    void unmoddedClientReceivesRateLimitedActionBarMessagesAndImmediateStateChanges() {
        List<String> packets = new ArrayList<String>();
        List<String> actionBars = new ArrayList<String>();
        AtomicLong clock = new AtomicLong(5000L);

        AnvilFeedbackTracker tracker = new AnvilFeedbackTracker(
                new AnvilFeedbackTracker.FeedbackDispatcher() {
                    @Override
                    public boolean hasClientMod(UUID playerId, EntityPlayerMP player) {
                        return false;
                    }

                    @Override
                    public void sendModdedPacket(UUID playerId, EntityPlayerMP player, int windowId, boolean active, int attemptedCount, int maxAllowed) {
                        packets.add("packet");
                    }

                    @Override
                    public void sendUnmoddedActionBar(UUID playerId, EntityPlayerMP player, String message) {
                        actionBars.add(message);
                    }
                },
                clock::get,
                4000L
        );

        UUID player = UUID.randomUUID();
        tracker.reportRejection(player, null, 3, 2, 1);
        clock.addAndGet(1000L);
        tracker.reportRejection(player, null, 3, 2, 1);
        assertEquals(1, actionBars.size());
        assertEquals("This would give the item 2 enchantments. The limit is 1.", actionBars.get(0));
        assertTrue(packets.isEmpty());

        // State change within cooldown sends immediately.
        clock.addAndGet(500L);
        tracker.reportRejection(player, null, 3, 3, 1);
        assertEquals(2, actionBars.size());
        assertEquals("This would give the item 3 enchantments. The limit is 1.", actionBars.get(1));

        // After cooldown elapses, a reminder may be sent for the unchanged state.
        clock.addAndGet(4500L);
        tracker.reportRejection(player, null, 3, 3, 1);
        assertEquals(3, actionBars.size());

        // Logout clears tracked player state cleanly.
        tracker.clearPlayer(player);
        assertFalse(tracker.hasActiveRejection(player));
    }

    @Test
    void packetRoundTripsThroughByteBuf() {
        PacketAnvilCapRejection outbound = new PacketAnvilCapRejection(12, true, 3, 2);
        ByteBuf buf = Unpooled.buffer();
        outbound.toBytes(buf);

        PacketAnvilCapRejection inbound = new PacketAnvilCapRejection();
        inbound.fromBytes(buf);

        assertEquals(12, inbound.getWindowId());
        assertTrue(inbound.isActive());
        assertEquals(3, inbound.getAttemptedCount());
        assertEquals(2, inbound.getMaxAllowed());
    }
}
