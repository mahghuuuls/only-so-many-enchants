package com.mahghuuuls.onlysomanyenchants.compat;

import com.mahghuuuls.onlysomanyenchants.Tags;
import com.mahghuuuls.onlysomanyenchants.core.ContainerSanitizer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.items.IItemHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.Predicate;

public final class PlayerExtraSlotAdapters {

    public static final String MOD_BAUBLES = "baubles";
    public static final String MOD_INTRINSIC_BACKPACK = "intrinsicbackpack";
    public static final String MOD_TOOL_SLOTS = "toolslots";
    public static final String MOD_COSMETIC_ARMOR = "cosmeticarmorreworked";

    private static final Logger LOGGER = LogManager.getLogger(Tags.MOD_ID);
    private static final PlayerExtraSlotAdapters DEFAULT_INSTANCE = new PlayerExtraSlotAdapters(Loader::isModLoaded);

    private final Predicate<String> modLoadedCheck;

    private volatile boolean baublesDisabled;
    private volatile Method baublesGetHandlerMethod;

    private volatile boolean intrinsicBackpackDisabled;
    private volatile Method intrinsicBackpacksOfMethod;
    private volatile Method intrinsicAllCompartmentsMethod;
    private volatile Method intrinsicCompartmentStorageMethod;

    private volatile boolean toolSlotsDisabled;
    private volatile Method toolSlotsOfMethod;
    private volatile Method toolSlotsInventoryMethod;

    private volatile boolean cosmeticArmorDisabled;
    private volatile Method cosmeticArmorGetStacksMethod;

    PlayerExtraSlotAdapters(Predicate<String> modLoadedCheck) {
        this.modLoadedCheck = modLoadedCheck;
    }

    public static int sanitizePlayerExtraSlots(EntityPlayer player) {
        return DEFAULT_INSTANCE.sanitize(player);
    }

    int sanitize(EntityPlayer player) {
        if (player == null || player.world == null || player.world.isRemote) {
            return 0;
        }
        Random rng = player.getRNG();
        int total = 0;
        total += sanitizeBaubles(player, rng);
        total += sanitizeIntrinsicBackpack(player, rng);
        total += sanitizeToolSlots(player, rng);
        total += sanitizeCosmeticArmor(player, rng);
        return total;
    }

    int sanitizeBaubles(EntityPlayer player, Random rng) {
        if (baublesDisabled || !isModLoaded(MOD_BAUBLES)) {
            return 0;
        }
        try {
            Method getHandler = baublesGetHandlerMethod;
            if (getHandler == null) {
                Class<?> apiClass = Class.forName("baubles.api.BaublesApi");
                getHandler = apiClass.getMethod("getBaublesHandler", EntityPlayer.class);
                baublesGetHandlerMethod = getHandler;
            }
            Object result = getHandler.invoke(null, player);
            if (result instanceof IItemHandler) {
                return ContainerSanitizer.sanitizeItemHandler((IItemHandler) result, null, "extra_slot_baubles", rng);
            }
            return 0;
        } catch (Throwable ex) {
            baublesDisabled = true;
            LOGGER.warn("Disabling optional Baubles extra-slot adapter after unexpected error: {}", ex.toString());
            return 0;
        }
    }

    int sanitizeIntrinsicBackpack(EntityPlayer player, Random rng) {
        if (intrinsicBackpackDisabled || !isModLoaded(MOD_INTRINSIC_BACKPACK)) {
            return 0;
        }
        try {
            Method ofMethod = intrinsicBackpacksOfMethod;
            if (ofMethod == null) {
                Class<?> backpacksClass = Class.forName("com.mahghuuuls.intrinsicbackpack.capability.Backpacks");
                ofMethod = backpacksClass.getMethod("of", EntityPlayer.class);
                intrinsicBackpacksOfMethod = ofMethod;
            }
            Object backpackData = ofMethod.invoke(null, player);
            if (backpackData == null) {
                return 0;
            }
            Method allCompartmentsMethod = intrinsicAllCompartmentsMethod;
            if (allCompartmentsMethod == null) {
                allCompartmentsMethod = backpackData.getClass().getMethod("allCompartmentsForPersistence");
                intrinsicAllCompartmentsMethod = allCompartmentsMethod;
            }
            Object compartmentsObj = allCompartmentsMethod.invoke(backpackData);
            if (!(compartmentsObj instanceof List<?>)) {
                return 0;
            }
            List<?> compartments = (List<?>) compartmentsObj;
            int total = 0;
            for (int i = 0; i < compartments.size(); i++) {
                Object compartment = compartments.get(i);
                if (compartment == null) {
                    continue;
                }
                Method storageMethod = intrinsicCompartmentStorageMethod;
                if (storageMethod == null) {
                    storageMethod = compartment.getClass().getMethod("storage");
                    intrinsicCompartmentStorageMethod = storageMethod;
                }
                Object storage = storageMethod.invoke(compartment);
                if (storage instanceof IItemHandler) {
                    total += ContainerSanitizer.sanitizeItemHandler(
                            (IItemHandler) storage,
                            null,
                            "extra_slot_intrinsicbackpack",
                            rng
                    );
                }
            }
            return total;
        } catch (Throwable ex) {
            intrinsicBackpackDisabled = true;
            LOGGER.warn("Disabling optional Intrinsic Backpack extra-slot adapter after unexpected error: {}", ex.toString());
            return 0;
        }
    }

    int sanitizeToolSlots(EntityPlayer player, Random rng) {
        if (toolSlotsDisabled || !isModLoaded(MOD_TOOL_SLOTS)) {
            return 0;
        }
        try {
            Method ofMethod = toolSlotsOfMethod;
            if (ofMethod == null) {
                Class<?> capClass = Class.forName("com.mahghuuuls.toolslots.common.slots.ToolSlotsCapability");
                ofMethod = capClass.getMethod("of", EntityPlayer.class);
                toolSlotsOfMethod = ofMethod;
            }
            Object playerToolSlots = ofMethod.invoke(null, player);
            if (playerToolSlots == null) {
                return 0;
            }
            Method invMethod = toolSlotsInventoryMethod;
            if (invMethod == null) {
                invMethod = playerToolSlots.getClass().getMethod("inventory");
                toolSlotsInventoryMethod = invMethod;
            }
            Object handler = invMethod.invoke(playerToolSlots);
            if (handler instanceof IItemHandler) {
                return ContainerSanitizer.sanitizeItemHandler((IItemHandler) handler, null, "extra_slot_toolslots", rng);
            }
            return 0;
        } catch (Throwable ex) {
            toolSlotsDisabled = true;
            LOGGER.warn("Disabling optional Tool Slots extra-slot adapter after unexpected error: {}", ex.toString());
            return 0;
        }
    }

    int sanitizeCosmeticArmor(EntityPlayer player, Random rng) {
        if (cosmeticArmorDisabled || !isModLoaded(MOD_COSMETIC_ARMOR)) {
            return 0;
        }
        try {
            Method getStacksMethod = cosmeticArmorGetStacksMethod;
            if (getStacksMethod == null) {
                Class<?> apiClass = Class.forName("lain.mods.cos.api.CosArmorAPI");
                getStacksMethod = apiClass.getMethod("getCAStacks", UUID.class);
                cosmeticArmorGetStacksMethod = getStacksMethod;
            }
            Object stacks = getStacksMethod.invoke(null, player.getUniqueID());
            if (stacks instanceof IItemHandler) {
                return ContainerSanitizer.sanitizeItemHandler((IItemHandler) stacks, null, "extra_slot_cosmeticarmor", rng);
            }
            return 0;
        } catch (Throwable ex) {
            cosmeticArmorDisabled = true;
            LOGGER.warn("Disabling optional Cosmetic Armor Reworked extra-slot adapter after unexpected error: {}", ex.toString());
            return 0;
        }
    }

    private boolean isModLoaded(String modId) {
        return modLoadedCheck != null && modLoadedCheck.test(modId);
    }

    boolean isBaublesDisabled() {
        return baublesDisabled;
    }

    boolean isIntrinsicBackpackDisabled() {
        return intrinsicBackpackDisabled;
    }

    boolean isToolSlotsDisabled() {
        return toolSlotsDisabled;
    }

    boolean isCosmeticArmorDisabled() {
        return cosmeticArmorDisabled;
    }
}
