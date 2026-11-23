package com.mahghuuuls.onlysomanyenchants.mixin;

import com.mahghuuuls.onlysomanyenchants.core.ContainerSanitizer;
import com.mahghuuuls.onlysomanyenchants.core.ItemEnchantmentSanitizer;
import net.minecraft.inventory.IInventory;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(LootTable.class)
public abstract class MixinLootTable {

    @Inject(method = "fillInventory", at = @At("RETURN"))
    private void osme$afterFillInventory(IInventory inventory, Random rand, LootContext context, CallbackInfo ci) {
        if (inventory == null) {
            return;
        }
        if (context != null && context.getWorld() != null && context.getWorld().isRemote) {
            return;
        }
        ContainerSanitizer.sanitizeInventory(
                inventory,
                "loot_table",
                rand != null ? rand : ItemEnchantmentSanitizer.defaultRandom()
        );
    }
}
