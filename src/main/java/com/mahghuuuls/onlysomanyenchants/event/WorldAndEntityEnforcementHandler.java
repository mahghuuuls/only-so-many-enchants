package com.mahghuuuls.onlysomanyenchants.event;

import com.mahghuuuls.onlysomanyenchants.core.ContainerSanitizer;
import com.mahghuuuls.onlysomanyenchants.core.ItemEnchantmentSanitizer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketEntityEquipment;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

import java.util.List;
import java.util.Random;

public final class WorldAndEntityEnforcementHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event == null || event.getWorld() == null || event.getWorld().isRemote) {
            return;
        }
        Entity entity = event.getEntity();
        if (entity instanceof EntityItem) {
            sanitizeEntityItem((EntityItem) entity, "entity_item_join", event.getWorld().rand);
        } else if (entity instanceof EntityLivingBase && !(entity instanceof EntityPlayer)) {
            EntityLivingBase living = (EntityLivingBase) entity;
            ContainerSanitizer.sanitizeLivingEquipment(living, "mob_equip_join", living.getRNG());
        }
    }

    @SubscribeEvent
    public void onLivingEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event == null || event.getEntityLiving() == null) {
            return;
        }
        EntityLivingBase entity = event.getEntityLiving();
        if (entity.world == null || entity.world.isRemote) {
            return;
        }
        ItemStack equipped = event.getTo();
        if (equipped == null || equipped.isEmpty()) {
            return;
        }
        if (ContainerSanitizer.sanitizeStackAndNested(equipped, "living_equipment_change", entity.getRNG())) {
            if (entity instanceof EntityPlayerMP) {
                EntityPlayerMP player = (EntityPlayerMP) entity;
                if (player.inventoryContainer != null) {
                    player.inventoryContainer.detectAndSendChanges();
                }
            } else if (entity.world instanceof WorldServer && event.getSlot() != null) {
                ((WorldServer) entity.world).getEntityTracker().sendToTracking(
                        entity,
                        new SPacketEntityEquipment(entity.getEntityId(), event.getSlot(), equipped)
                );
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingDrops(LivingDropsEvent event) {
        if (event == null || event.getEntityLiving() == null) {
            return;
        }
        EntityLivingBase entity = event.getEntityLiving();
        if (entity.world == null || entity.world.isRemote || event.getDrops() == null) {
            return;
        }
        Random rng = entity.getRNG();
        for (EntityItem drop : event.getDrops()) {
            sanitizeEntityItem(drop, "living_drops", rng);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onItemFished(ItemFishedEvent event) {
        if (event == null || event.getEntityPlayer() == null) {
            return;
        }
        EntityPlayer player = event.getEntityPlayer();
        if (player.world == null || player.world.isRemote) {
            return;
        }
        List<ItemStack> drops = event.getDrops();
        if (drops == null || drops.isEmpty()) {
            return;
        }
        Random rng = player.getRNG();
        for (ItemStack stack : drops) {
            if (stack != null && !stack.isEmpty()) {
                ContainerSanitizer.sanitizeStackAndNested(stack, "fishing", rng);
            }
        }
    }

    @SubscribeEvent
    public void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event == null || event.player == null || event.player.world == null || event.player.world.isRemote) {
            return;
        }
        ItemStack crafted = event.crafting;
        if (crafted == null || crafted.isEmpty()) {
            return;
        }
        if (ContainerSanitizer.sanitizeStackAndNested(crafted, "crafting", event.player.getRNG())) {
            if (event.player instanceof EntityPlayerMP) {
                EntityPlayerMP serverPlayer = (EntityPlayerMP) event.player;
                if (serverPlayer.inventoryContainer != null) {
                    serverPlayer.inventoryContainer.detectAndSendChanges();
                }
                serverPlayer.updateHeldItem();
            }
        }
    }

    private static void sanitizeEntityItem(EntityItem entityItem, String sourcePath, Random rng) {
        if (entityItem == null) {
            return;
        }
        ItemStack stack = entityItem.getItem();
        if (stack == null || stack.isEmpty()) {
            return;
        }
        Random effectiveRng = rng != null ? rng : ItemEnchantmentSanitizer.defaultRandom();
        if (ContainerSanitizer.sanitizeStackAndNested(stack, sourcePath, effectiveRng)) {
            entityItem.setItem(stack);
        }
    }
}
