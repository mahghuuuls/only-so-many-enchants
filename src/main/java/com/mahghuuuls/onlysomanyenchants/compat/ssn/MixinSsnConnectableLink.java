package com.mahghuuuls.onlysomanyenchants.compat.ssn;

import com.mahghuuuls.onlysomanyenchants.Tags;
import com.mahghuuuls.onlysomanyenchants.core.ContainerSanitizer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityLockableLoot;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

@Pseudo
@Mixin(targets = "mrriegel.storagenetwork.capabilities.CapabilityConnectableLink", remap = false)
public abstract class MixinSsnConnectableLink {

    @Unique
    private static final Logger OSME_SSN_LOGGER = LogManager.getLogger(Tags.MOD_ID);

    @Unique
    private static volatile boolean osme$ssnDisabled;

    @Unique
    private static volatile boolean osme$reflectionReady;

    @Unique
    private static Field osme$inventoryFaceField;

    @Unique
    private static Method osme$getTargetPosMethod;

    @Unique
    private static Method osme$dimPosIsLoadedMethod;

    @Unique
    private static Method osme$dimPosGetWorldMethod;

    @Unique
    private static Method osme$dimPosGetTileEntityMethod;

    @Unique
    private static Method osme$dimPosGetCapabilityMethod;

    @Inject(method = "getStoredStacks", at = @At("HEAD"), require = 0)
    private void osme$beforeGetStoredStacks(CallbackInfoReturnable<List<?>> cir) {
        if (osme$ssnDisabled) {
            return;
        }
        try {
            if (!osme$ensureReflection(this.getClass())) {
                return;
            }
            Object faceObj = osme$inventoryFaceField.get(this);
            if (!(faceObj instanceof EnumFacing)) {
                return;
            }
            EnumFacing inventoryFace = (EnumFacing) faceObj;
            Object targetPos = osme$getTargetPosMethod.invoke(this);
            if (targetPos == null) {
                return;
            }
            if (!osme$ensureDimPosReflection(targetPos.getClass())) {
                return;
            }
            Object loadedObj = osme$dimPosIsLoadedMethod.invoke(targetPos);
            if (!(loadedObj instanceof Boolean) || !((Boolean) loadedObj).booleanValue()) {
                return;
            }
            Object worldObj = osme$dimPosGetWorldMethod.invoke(targetPos);
            if (!(worldObj instanceof World)) {
                return;
            }
            World world = (World) worldObj;
            if (world.isRemote) {
                return;
            }
            Object tileObj = osme$dimPosGetTileEntityMethod.invoke(targetPos, TileEntity.class);
            TileEntity tileEntity = tileObj instanceof TileEntity ? (TileEntity) tileObj : null;
            if (tileEntity instanceof TileEntityLockableLoot
                    && ((TileEntityLockableLoot) tileEntity).getLootTable() != null) {
                return;
            }
            Object handlerObj = osme$dimPosGetCapabilityMethod.invoke(
                    targetPos,
                    CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,
                    inventoryFace.getOpposite()
            );
            if (handlerObj instanceof IItemHandler) {
                ContainerSanitizer.sanitizeItemHandler(
                        (IItemHandler) handlerObj,
                        tileEntity,
                        "ssn_backing_stock",
                        world.rand
                );
            }
        } catch (Throwable ex) {
            osme$ssnDisabled = true;
            OSME_SSN_LOGGER.warn("Disabling optional Simple Storage Network backing-stock sync after unexpected error: {}", ex.toString());
        }
    }

    @Unique
    private static boolean osme$ensureReflection(Class<?> linkClass) throws ReflectiveOperationException {
        if (osme$reflectionReady && osme$inventoryFaceField != null && osme$getTargetPosMethod != null) {
            return true;
        }
        Field faceField = linkClass.getDeclaredField("inventoryFace");
        faceField.setAccessible(true);
        Method targetPosMethod = linkClass.getMethod("getTargetPos");
        osme$inventoryFaceField = faceField;
        osme$getTargetPosMethod = targetPosMethod;
        osme$reflectionReady = true;
        return true;
    }

    @Unique
    private static boolean osme$ensureDimPosReflection(Class<?> dimPosClass) throws ReflectiveOperationException {
        if (osme$dimPosIsLoadedMethod != null
                && osme$dimPosGetWorldMethod != null
                && osme$dimPosGetTileEntityMethod != null
                && osme$dimPosGetCapabilityMethod != null) {
            return true;
        }
        osme$dimPosIsLoadedMethod = dimPosClass.getMethod("isLoaded");
        osme$dimPosGetWorldMethod = dimPosClass.getMethod("getWorld");
        osme$dimPosGetTileEntityMethod = dimPosClass.getMethod("getTileEntity", Class.class);
        osme$dimPosGetCapabilityMethod = dimPosClass.getMethod("getCapability", Capability.class, EnumFacing.class);
        return true;
    }
}
