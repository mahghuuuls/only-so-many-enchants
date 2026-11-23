package com.mahghuuuls.onlysomanyenchants.mixin;

import com.mahghuuuls.onlysomanyenchants.config.ModConfig;
import com.mahghuuuls.onlysomanyenchants.core.AnvilCapVerdict;
import com.mahghuuuls.onlysomanyenchants.core.AnvilComputationGuard;
import com.mahghuuuls.onlysomanyenchants.core.EnchantmentPolicy;
import com.mahghuuuls.onlysomanyenchants.core.ItemEnchantmentSanitizer;
import com.mahghuuuls.onlysomanyenchants.diag.DiagnosticLogger;
import com.mahghuuuls.onlysomanyenchants.network.AnvilFeedbackTracker;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerRepair;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketSetSlot;
import net.minecraft.network.play.server.SPacketWindowProperty;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ContainerRepair.class)
public abstract class MixinContainerRepair {

    @Shadow
    @Final
    private IInventory outputSlot;

    @Shadow
    public int maximumCost;

    @Shadow
    public int materialCost;

    @Shadow
    @Final
    private EntityPlayer player;

    @Inject(method = "updateRepairOutput", at = @At("HEAD"))
    private void osme$beforeUpdateRepairOutput(CallbackInfo ci) {
        AnvilComputationGuard.enter();
    }

    @Inject(method = "updateRepairOutput", at = @At("RETURN"))
    private void osme$afterUpdateRepairOutput(CallbackInfo ci) {
        try {
            if (this.player == null || this.player.world == null || this.player.world.isRemote) {
                return;
            }
            if (!(this.player instanceof EntityPlayerMP)) {
                return;
            }
            EntityPlayerMP serverPlayer = (EntityPlayerMP) this.player;
            int windowId = ((Container) (Object) this).windowId;

            ItemStack candidateOutput = this.outputSlot.getStackInSlot(0);
            if (candidateOutput == null || candidateOutput.isEmpty()) {
                AnvilFeedbackTracker.get().clearRejection(serverPlayer, windowId);
                return;
            }

            int distinctCount = ItemEnchantmentSanitizer.countDistinctEnchantments(candidateOutput);
            AnvilCapVerdict verdict = EnchantmentPolicy.evaluateAnvilOutput(distinctCount, ModConfig.getEffectiveCap());

            if (verdict.isRejected()) {
                if (ModConfig.isDiagnosticMode()) {
                    String itemId = ItemEnchantmentSanitizer.describeItem(candidateOutput);
                    DiagnosticLogger.get().logRejection(
                            "anvil",
                            itemId,
                            verdict.getMaxAllowed(),
                            verdict.getAttemptedCount(),
                            ItemEnchantmentSanitizer.formatEnchantmentEntries(candidateOutput),
                            "cap_exceeded"
                    );
                }
                this.outputSlot.setInventorySlotContents(0, ItemStack.EMPTY);
                this.maximumCost = 0;
                this.materialCost = 0;
                ((Container) (Object) this).detectAndSendChanges();
                if (serverPlayer.connection != null) {
                    // Explicitly clear client-predicted output slot and cost on both modded and unmodded clients.
                    serverPlayer.connection.sendPacket(new SPacketSetSlot(windowId, 2, ItemStack.EMPTY));
                    serverPlayer.connection.sendPacket(new SPacketWindowProperty(windowId, 0, 0));
                }
                AnvilFeedbackTracker.get().reportRejection(
                        serverPlayer,
                        windowId,
                        verdict.getAttemptedCount(),
                        verdict.getMaxAllowed()
                );
            } else {
                AnvilFeedbackTracker.get().clearRejection(serverPlayer, windowId);
            }
        } finally {
            AnvilComputationGuard.exit();
        }
    }
}
