package com.mahghuuuls.onlysomanyenchants.mixin;

import com.mahghuuuls.onlysomanyenchants.config.ModConfig;
import com.mahghuuuls.onlysomanyenchants.core.EnchantmentPolicy;
import com.mahghuuuls.onlysomanyenchants.core.ItemEnchantmentSanitizer;
import com.mahghuuuls.onlysomanyenchants.diag.DiagnosticLogger;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ContainerEnchantment;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ContainerEnchantment.class)
public abstract class MixinContainerEnchantment {

    @Shadow
    public int xpSeed;

    @Shadow
    public int[] enchantClue;

    @Shadow
    public int[] worldClue;

    @Shadow
    @Final
    private World world;

    @Unique
    private boolean osme$enchantingInProgress;

    @Unique
    private int osme$promisedClueId = -1;

    @Unique
    private int osme$promisedClueLevel = -1;

    @Inject(method = "onCraftMatrixChanged", at = @At("HEAD"))
    private void osme$beforeCraftMatrixChanged(IInventory inventoryIn, CallbackInfo ci) {
        this.osme$enchantingInProgress = false;
        this.osme$promisedClueId = -1;
        this.osme$promisedClueLevel = -1;
    }

    @Inject(method = "enchantItem", at = @At("HEAD"))
    private void osme$beforeEnchantItem(EntityPlayer playerIn, int id, CallbackInfoReturnable<Boolean> cir) {
        this.osme$enchantingInProgress = true;
        if (id >= 0 && this.enchantClue != null && this.worldClue != null
                && id < this.enchantClue.length && id < this.worldClue.length) {
            this.osme$promisedClueId = this.enchantClue[id];
            this.osme$promisedClueLevel = this.worldClue[id];
        } else {
            this.osme$promisedClueId = -1;
            this.osme$promisedClueLevel = -1;
        }
    }

    @Inject(method = "enchantItem", at = @At("RETURN"))
    private void osme$afterEnchantItem(EntityPlayer playerIn, int id, CallbackInfoReturnable<Boolean> cir) {
        this.osme$enchantingInProgress = false;
        this.osme$promisedClueId = -1;
        this.osme$promisedClueLevel = -1;
    }

    @Inject(method = "getEnchantmentList", at = @At("RETURN"), cancellable = true)
    private void osme$trimEnchantmentList(
            ItemStack stack,
            int enchantSlot,
            int level,
            CallbackInfoReturnable<List<EnchantmentData>> cir
    ) {
        List<EnchantmentData> original = cir.getReturnValue();
        if (original == null || original.isEmpty()) {
            return;
        }
        int cap = ModConfig.getEffectiveCap();
        List<EnchantmentData> trimmed = original.size() > cap
                ? EnchantmentPolicy.trimEnchantmentList(
                        original,
                        cap,
                        EnchantmentPolicy.createEnchantingTableRandom(this.xpSeed, enchantSlot)
                )
                : original;
        if (this.osme$enchantingInProgress
                && ModConfig.isDiagnosticMode()
                && (this.world == null || !this.world.isRemote)) {
            String sourcePath = "enchanting_table(clue=" + osme$formatClue(this.osme$promisedClueId, this.osme$promisedClueLevel) + ")";
            if (trimmed.size() < original.size()) {
                List<String> beforeEntries = ItemEnchantmentSanitizer.formatEnchantmentDataList(original);
                List<String> afterEntries = ItemEnchantmentSanitizer.formatEnchantmentDataList(trimmed);
                List<String> removedEntries = ItemEnchantmentSanitizer.formatRemovedEnchantmentDataList(original, trimmed);
                DiagnosticLogger.get().logTrim(
                        sourcePath,
                        ItemEnchantmentSanitizer.describeItem(stack),
                        cap,
                        original.size(),
                        beforeEntries,
                        trimmed.size(),
                        afterEntries,
                        removedEntries
                );
            } else {
                DiagnosticLogger.get().logEnchantmentGrant(
                        sourcePath,
                        ItemEnchantmentSanitizer.describeItem(stack),
                        cap,
                        ItemEnchantmentSanitizer.formatEnchantmentDataList(trimmed)
                );
            }
        }
        if (trimmed != original) {
            cir.setReturnValue(trimmed);
        }
    }

    @Unique
    private static String osme$formatClue(int clueId, int clueLevel) {
        if (clueId < 0) {
            return "none";
        }
        Enchantment enchantment = Enchantment.getEnchantmentByID(clueId);
        String name = (enchantment != null && enchantment.getRegistryName() != null)
                ? enchantment.getRegistryName().toString()
                : "id:" + clueId;
        return name + "@" + clueLevel;
    }
}
