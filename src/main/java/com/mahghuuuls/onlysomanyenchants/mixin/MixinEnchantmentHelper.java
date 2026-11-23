package com.mahghuuuls.onlysomanyenchants.mixin;

import com.mahghuuuls.onlysomanyenchants.core.ItemEnchantmentSanitizer;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Random;

@Mixin(EnchantmentHelper.class)
public abstract class MixinEnchantmentHelper {

    @Inject(method = "addRandomEnchantment", at = @At("RETURN"))
    private static void osme$afterAddRandomEnchantment(
            Random random,
            ItemStack stack,
            int level,
            boolean allowTreasure,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        if (!osme$isLogicalServer()) {
            return;
        }
        ItemStack result = cir.getReturnValue();
        if (result == null || result.isEmpty()) {
            return;
        }
        ItemEnchantmentSanitizer.sanitizeStack(
                result,
                "add_random_enchantment",
                random != null ? random : ItemEnchantmentSanitizer.defaultRandom()
        );
    }

    @Inject(method = "setEnchantments", at = @At("TAIL"))
    private static void osme$afterSetEnchantments(Map<Enchantment, Integer> enchMap, ItemStack stack, CallbackInfo ci) {
        if (!osme$isLogicalServer()) {
            return;
        }
        // ItemEnchantmentSanitizer writes NBT lists directly without calling setEnchantments, preventing recursion.
        ItemEnchantmentSanitizer.sanitizePassiveSetEnchantments(stack, ItemEnchantmentSanitizer.defaultRandom());
    }

    @Unique
    private static boolean osme$isLogicalServer() {
        FMLCommonHandler fml = FMLCommonHandler.instance();
        if (fml == null) {
            return false;
        }
        return fml.getEffectiveSide().isServer()
                || (fml.getSidedDelegate() != null && fml.getSide().isServer());
    }
}
