package com.mahghuuuls.onlysomanyenchants;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
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

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("Initializing {} {}", Tags.MOD_NAME, Tags.VERSION);
    }
}
