package com.mahghuuuls.onlysomanyenchants.network;

import com.mahghuuuls.onlysomanyenchants.Tags;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public final class NetworkHandler {

    private static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(Tags.MOD_ID);
    private static boolean registered = false;

    private NetworkHandler() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        CHANNEL.registerMessage(PacketAnvilCapRejection.Handler.class, PacketAnvilCapRejection.class, 0, Side.CLIENT);
        registered = true;
    }

    public static void sendAnvilCapRejection(EntityPlayerMP player, int windowId, boolean active, int attemptedCount, int maxAllowed) {
        if (player == null) {
            return;
        }
        CHANNEL.sendTo(new PacketAnvilCapRejection(windowId, active, attemptedCount, maxAllowed), player);
    }
}
