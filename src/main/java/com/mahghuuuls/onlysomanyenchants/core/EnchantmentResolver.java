package com.mahghuuuls.onlysomanyenchants.core;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.util.ResourceLocation;

@FunctionalInterface
public interface EnchantmentResolver {

    EnchantmentResolver FORGE = new EnchantmentResolver() {
        @Override
        public String resolveRegistryName(int enchantmentId) {
            Enchantment enchantment = Enchantment.getEnchantmentByID(enchantmentId);
            if (enchantment == null) {
                return null;
            }
            ResourceLocation name = enchantment.getRegistryName();
            return name != null ? name.toString() : "id:" + enchantmentId;
        }
    };

    String resolveRegistryName(int enchantmentId);
}
