package com.mahghuuuls.onlysomanyenchants.mixin;

import com.mahghuuuls.onlysomanyenchants.core.ContainerSanitizer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Container.class)
public abstract class MixinContainer {

    @Inject(method = "addListener", at = @At("HEAD"))
    private void osme$beforeAddListener(IContainerListener listener, CallbackInfo ci) {
        if (!(listener instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) listener;
        if (player.world == null || player.world.isRemote) {
            return;
        }
        ContainerSanitizer.sanitizeContainer((Container) (Object) this, "container_open", player.getRNG());
    }
}
