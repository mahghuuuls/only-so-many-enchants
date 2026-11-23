package com.mahghuuuls.onlysomanyenchants.compat.easymagic;

import com.mahghuuuls.onlysomanyenchants.config.ModConfig;
import com.mahghuuuls.onlysomanyenchants.core.EnchantmentPolicy;
import net.minecraft.client.gui.GuiEnchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.inventory.ContainerEnchantment;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@SideOnly(Side.CLIENT)
@Mixin(value = GuiEnchantment.class, priority = 1100)
public abstract class MixinEasyMagicGuiHints {

    @Shadow
    @Final
    private ContainerEnchantment container;

    @Inject(method = "easymagic$computeEnchantList", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void osme$trimEasyMagicGuiHints(
            int slot,
            CallbackInfoReturnable<List<EnchantmentData>> cir
    ) {
        if (this.container == null) {
            return;
        }
        List<EnchantmentData> original = cir.getReturnValue();
        if (original == null || original.isEmpty()) {
            return;
        }
        // Client-side fallback only runs before server MessageEnchantHints arrives; server hints override with server cap.
        int cap = ModConfig.getEffectiveCap();
        if (original.size() <= cap) {
            return;
        }
        cir.setReturnValue(EnchantmentPolicy.trimEnchantmentList(
                original,
                cap,
                EnchantmentPolicy.createEnchantingTableRandom(this.container.xpSeed, slot)
        ));
    }
}
