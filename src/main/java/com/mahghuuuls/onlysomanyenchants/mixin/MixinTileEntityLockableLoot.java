package com.mahghuuuls.onlysomanyenchants.mixin;

import com.mahghuuuls.onlysomanyenchants.core.ContainerSanitizer;
import com.mahghuuuls.onlysomanyenchants.core.ItemEnchantmentSanitizer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityLockableLoot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(TileEntityLockableLoot.class)
public abstract class MixinTileEntityLockableLoot {

    @Inject(
            method = "fillWithLoot",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/storage/loot/LootTable;fillInventory(Lnet/minecraft/inventory/IInventory;Ljava/util/Random;Lnet/minecraft/world/storage/loot/LootContext;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void osme$afterFillWithLoot(@Nullable EntityPlayer player, CallbackInfo ci) {
        TileEntity self = (TileEntity) (Object) this;
        if (self.getWorld() == null || self.getWorld().isRemote) {
            return;
        }
        ContainerSanitizer.sanitizeInventory(
                (IInventory) (Object) this,
                "loot_chest",
                ItemEnchantmentSanitizer.defaultRandom()
        );
    }
}
