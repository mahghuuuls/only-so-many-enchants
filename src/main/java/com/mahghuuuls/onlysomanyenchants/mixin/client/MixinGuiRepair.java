package com.mahghuuuls.onlysomanyenchants.mixin.client;

import com.mahghuuuls.onlysomanyenchants.client.AnvilRejectionOverlayRenderer;
import com.mahghuuuls.onlysomanyenchants.client.ClientAnvilFeedbackState;
import net.minecraft.client.gui.GuiRepair;
import net.minecraft.inventory.ContainerRepair;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRepair.class)
public abstract class MixinGuiRepair {

    @Shadow
    @Final
    private ContainerRepair anvil;

    @Inject(method = "drawGuiContainerForegroundLayer", at = @At("TAIL"))
    private void osme$renderCapRejectionOverlay(int mouseX, int mouseY, CallbackInfo ci) {
        AnvilRejectionOverlayRenderer.renderIfActive((GuiRepair) (Object) this, this.anvil);
    }

    @Inject(method = "onGuiClosed", at = @At("TAIL"))
    private void osme$clearCapRejectionOnClose(CallbackInfo ci) {
        ClientAnvilFeedbackState.clear();
    }
}
