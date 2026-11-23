package com.mahghuuuls.onlysomanyenchants.compat.easymagic;

import com.mahghuuuls.onlysomanyenchants.config.ModConfig;
import com.mahghuuuls.onlysomanyenchants.core.EnchantmentPolicy;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.inventory.ContainerEnchantment;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = ContainerEnchantment.class, priority = 1100)
public abstract class MixinEasyMagicContainerHints {

    @Shadow
    public int xpSeed;

    @Inject(method = "easymagic$buildSlotHints", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void osme$trimEasyMagicSlotHints(
            ItemStack stack,
            int slot,
            CallbackInfoReturnable<List<EnchantmentData>> cir
    ) {
        List<EnchantmentData> original = cir.getReturnValue();
        if (original == null || original.isEmpty()) {
            return;
        }
        int cap = ModConfig.getEffectiveCap();
        if (original.size() <= cap) {
            return;
        }
        cir.setReturnValue(EnchantmentPolicy.trimEnchantmentList(
                original,
                cap,
                EnchantmentPolicy.createEnchantingTableRandom(this.xpSeed, slot)
        ));
    }
}
