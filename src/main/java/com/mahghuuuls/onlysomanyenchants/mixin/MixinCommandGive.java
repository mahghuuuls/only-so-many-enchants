package com.mahghuuuls.onlysomanyenchants.mixin;

import com.mahghuuuls.onlysomanyenchants.core.ContainerSanitizer;
import com.mahghuuuls.onlysomanyenchants.core.ItemEnchantmentSanitizer;
import net.minecraft.command.CommandGive;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(CommandGive.class)
public abstract class MixinCommandGive {

    @ModifyVariable(
            method = "execute",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/InventoryPlayer;addItemStackToInventory(Lnet/minecraft/item/ItemStack;)Z"
            )
    )
    private ItemStack osme$sanitizeGivenStack(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            ContainerSanitizer.sanitizeStackAndNested(stack, "command_give", ItemEnchantmentSanitizer.defaultRandom());
        }
        return stack;
    }
}
