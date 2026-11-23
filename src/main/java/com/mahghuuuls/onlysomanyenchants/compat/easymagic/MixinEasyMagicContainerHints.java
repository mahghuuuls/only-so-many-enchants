package com.mahghuuuls.onlysomanyenchants.compat.easymagic;

import com.mahghuuuls.onlysomanyenchants.config.ModConfig;
import com.mahghuuuls.onlysomanyenchants.core.EnchantmentPolicy;
import com.mahghuuuls.onlysomanyenchants.core.ItemEnchantmentSanitizer;
import com.mahghuuuls.onlysomanyenchants.diag.DiagnosticLogger;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.inventory.ContainerEnchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = ContainerEnchantment.class, priority = 1100)
public abstract class MixinEasyMagicContainerHints {

    @Shadow
    @Final
    private World world;

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
        List<EnchantmentData> trimmed = EnchantmentPolicy.trimEnchantmentList(
                original,
                cap,
                EnchantmentPolicy.createEnchantingTableRandom(this.xpSeed, slot)
        );
        if (ModConfig.isDiagnosticMode() && (this.world == null || !this.world.isRemote)) {
            DiagnosticLogger.get().logTrim(
                    "easymagic_slot_hints(slot=" + slot + ")",
                    ItemEnchantmentSanitizer.describeItem(stack),
                    cap,
                    original.size(),
                    ItemEnchantmentSanitizer.formatEnchantmentDataList(original),
                    trimmed.size(),
                    ItemEnchantmentSanitizer.formatEnchantmentDataList(trimmed),
                    ItemEnchantmentSanitizer.formatRemovedEnchantmentDataList(original, trimmed)
            );
        }
        cir.setReturnValue(trimmed);
    }
}
