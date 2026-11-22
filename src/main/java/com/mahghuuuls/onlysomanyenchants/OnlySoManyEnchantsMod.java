package com.mahghuuuls.onlysomanyenchants;

import com.mahghuuuls.onlysomanyenchants.config.ModConfig;
import com.mahghuuuls.onlysomanyenchants.diag.CommandOnlySoManyEnchants;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
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
        MinecraftForge.EVENT_BUS.register(new ModConfig.ReloadHandler());
        proxy.preInit(event);
        LOGGER.info(
                "Initialized {} {} (maxDistinctEnchantments={}, diagnosticMode={})",
                Tags.MOD_NAME,
                Tags.VERSION,
                ModConfig.getEffectiveCap(),
                ModConfig.isDiagnosticMode()
        );
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandOnlySoManyEnchants());
    }
}
