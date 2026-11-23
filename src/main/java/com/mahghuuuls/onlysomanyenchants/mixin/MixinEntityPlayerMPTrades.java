package com.mahghuuuls.onlysomanyenchants.mixin;

import com.mahghuuuls.onlysomanyenchants.core.ContainerSanitizer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.village.MerchantRecipeList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EntityPlayerMP.class)
public abstract class MixinEntityPlayerMPTrades {

    @ModifyVariable(
            method = "displayVillagerTradeGui",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/village/MerchantRecipeList;writeToBuf(Lnet/minecraft/network/PacketBuffer;)V"
            )
    )
    private MerchantRecipeList osme$beforeWriteTradeList(MerchantRecipeList recipes) {
        EntityPlayerMP self = (EntityPlayerMP) (Object) this;
        if (self.world != null && !self.world.isRemote && recipes != null && !recipes.isEmpty()) {
            ContainerSanitizer.sanitizeMerchantRecipes(recipes, "merchant_trade_gui", self.getRNG());
        }
        return recipes;
    }
}
