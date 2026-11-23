package com.mahghuuuls.onlysomanyenchants.mixin;

import com.mahghuuuls.onlysomanyenchants.core.ContainerSanitizer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.client.CPacketCreativeInventoryAction;
import net.minecraft.network.play.server.SPacketSetSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayServer.class)
public abstract class MixinNetHandlerPlayServer {

    @Shadow
    public EntityPlayerMP player;

    @Inject(method = "processCreativeInventoryAction", at = @At("TAIL"))
    private void osme$afterCreativeInventoryAction(CPacketCreativeInventoryAction packetIn, CallbackInfo ci) {
        if (this.player == null || this.player.world == null || this.player.world.isRemote) {
            return;
        }
        if (!this.player.interactionManager.isCreative()) {
            return;
        }
        int slotId = packetIn.getSlotId();
        if (slotId < 1 || slotId > 45
                || this.player.inventoryContainer == null
                || slotId >= this.player.inventoryContainer.inventorySlots.size()) {
            return;
        }
        ItemStack placed = this.player.inventoryContainer.getSlot(slotId).getStack();
        if (placed == null || placed.isEmpty()) {
            return;
        }
        if (ContainerSanitizer.sanitizeStackAndNested(placed, "creative_inventory", this.player.getRNG())) {
            this.player.inventoryContainer.detectAndSendChanges();
            if (this.player.connection != null) {
                this.player.connection.sendPacket(
                        new SPacketSetSlot(this.player.inventoryContainer.windowId, slotId, placed)
                );
            }
        }
    }
}
