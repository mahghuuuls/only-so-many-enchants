package com.mahghuuuls.onlysomanyenchants.mixin;

import com.mahghuuuls.onlysomanyenchants.core.ContainerSanitizer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.village.MerchantRecipeList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityVillager.class)
public abstract class MixinEntityVillager {

    @Shadow
    private MerchantRecipeList buyingList;

    @Inject(method = "populateBuyingList", at = @At("TAIL"))
    private void osme$afterPopulateBuyingList(CallbackInfo ci) {
        EntityLivingBase self = (EntityLivingBase) (Object) this;
        if (self.world == null || self.world.isRemote || this.buyingList == null) {
            return;
        }
        ContainerSanitizer.sanitizeMerchantRecipes(this.buyingList, "villager_populate", self.getRNG());
    }

    @Inject(method = "getRecipes", at = @At("RETURN"))
    private void osme$afterGetRecipes(EntityPlayer player, CallbackInfoReturnable<MerchantRecipeList> cir) {
        EntityLivingBase self = (EntityLivingBase) (Object) this;
        if (self.world == null || self.world.isRemote) {
            return;
        }
        MerchantRecipeList recipes = cir.getReturnValue();
        if (recipes != null && !recipes.isEmpty()) {
            ContainerSanitizer.sanitizeMerchantRecipes(recipes, "villager_recipes", self.getRNG());
        }
    }
}
