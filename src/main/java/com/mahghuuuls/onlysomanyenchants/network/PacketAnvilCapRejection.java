package com.mahghuuuls.onlysomanyenchants.network;

import com.mahghuuuls.onlysomanyenchants.OnlySoManyEnchantsMod;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class PacketAnvilCapRejection implements IMessage {

    private int windowId;
    private boolean active;
    private int attemptedCount;
    private int maxAllowed;

    public PacketAnvilCapRejection() {
    }

    public PacketAnvilCapRejection(int windowId, boolean active, int attemptedCount, int maxAllowed) {
        this.windowId = windowId;
        this.active = active;
        this.attemptedCount = attemptedCount;
        this.maxAllowed = maxAllowed;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.windowId = buf.readInt();
        this.active = buf.readBoolean();
        this.attemptedCount = buf.readInt();
        this.maxAllowed = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.windowId);
        buf.writeBoolean(this.active);
        buf.writeInt(this.attemptedCount);
        buf.writeInt(this.maxAllowed);
    }

    public int getWindowId() {
        return windowId;
    }

    public boolean isActive() {
        return active;
    }

    public int getAttemptedCount() {
        return attemptedCount;
    }

    public int getMaxAllowed() {
        return maxAllowed;
    }

    public static final class Handler implements IMessageHandler<PacketAnvilCapRejection, IMessage> {
        @Override
        public IMessage onMessage(PacketAnvilCapRejection message, MessageContext ctx) {
            if (OnlySoManyEnchantsMod.proxy != null) {
                OnlySoManyEnchantsMod.proxy.handleAnvilCapRejection(
                        message.windowId,
                        message.active,
                        message.attemptedCount,
                        message.maxAllowed
                );
            }
            return null;
        }
    }
}
