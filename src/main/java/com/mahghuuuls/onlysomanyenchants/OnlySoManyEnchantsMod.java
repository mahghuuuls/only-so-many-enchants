package com.mahghuuuls.onlysomanyenchants;

import com.mahghuuuls.onlysomanyenchants.config.ModConfig;
import com.mahghuuuls.onlysomanyenchants.core.AnvilComputationGuard;
import com.mahghuuuls.onlysomanyenchants.diag.CommandOnlySoManyEnchants;
import com.mahghuuuls.onlysomanyenchants.event.CommandEnchantGuardHandler;
import com.mahghuuuls.onlysomanyenchants.event.WorldAndEntityEnforcementHandler;
import com.mahghuuuls.onlysomanyenchants.network.AnvilFeedbackTracker;
import com.mahghuuuls.onlysomanyenchants.network.NetworkHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
        modid = Tags.MOD_ID,
        name = Tags.MOD_NAME,
        version = Tags.VERSION,
        dependencies = "required-after:mixinbooter@[10.7,)",
        acceptableRemoteVersions = "*"
)
public class OnlySoManyEnchantsMod {

    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_ID);

    @SidedProxy(
            clientSide = "com.mahghuuuls.onlysomanyenchants.ClientProxy",
            serverSide = "com.mahghuuuls.onlysomanyenchants.CommonProxy"
    )
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ModConfig.init(event.getSuggestedConfigurationFile());
        NetworkHandler.register();
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new ModConfig.ReloadHandler());
        MinecraftForge.EVENT_BUS.register(new AnvilFeedbackTracker.LifecycleHandler());
        MinecraftForge.EVENT_BUS.register(new CommandEnchantGuardHandler());
        MinecraftForge.EVENT_BUS.register(new WorldAndEntityEnforcementHandler());
        proxy.preInit(event);
        LOGGER.info(
                "Initialized {} {} (maxDistinctEnchantments={}, diagnosticMode={})",
                Tags.MOD_NAME,
                Tags.VERSION,
                ModConfig.getEffectiveCap(),
                ModConfig.isDiagnosticMode()
        );
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            AnvilComputationGuard.reset();
        }
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandOnlySoManyEnchants());
    }
}
