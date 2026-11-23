package com.mahghuuuls.onlysomanyenchants;

import com.mahghuuuls.onlysomanyenchants.client.ClientAnvilFeedbackState;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
    }

    @Override
    public void handleAnvilCapRejection(final int windowId, final boolean active, final int attemptedCount, final int maxAllowed) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null) {
            mc.addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    ClientAnvilFeedbackState.update(windowId, active, attemptedCount, maxAllowed);
                }
            });
        }
    }
}
