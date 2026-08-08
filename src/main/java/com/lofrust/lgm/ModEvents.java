package com.lofrust.lgm;

import com.lofrust.lgm.item.MaximumEffort;
import com.lofrust.lgm.item.ModItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = LGM.MODID)
public class ModEvents {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);

        if (heldItem.getItem() instanceof MaximumEffort gunItem) {
            // ===== КУЛДАУН ВЫСТРЕЛОВ =====
            String cooldownKey = "gun_cooldown_" + gunItem.getDescriptionId();
            int currentCooldown = player.getPersistentData().getInt(cooldownKey);
            if (currentCooldown > 0) {
                player.getPersistentData().putInt(cooldownKey, currentCooldown - 1);
            }

            // ===== ПЕРЕЗАРЯДКА =====
            String reloadKey = "gun_reloading_" + gunItem.getDescriptionId();
            int reloadTicks = player.getPersistentData().getInt(reloadKey);

            if (reloadTicks > 0) {
                int newTicks = reloadTicks - 1;
                player.getPersistentData().putInt(reloadKey, newTicks);

                if (newTicks == 0) {
                    int maxAmmo = gunItem.getMaxAmmo(heldItem);
                    int currentAmmo = gunItem.getAmmo(heldItem);
                    int ammoToAdd = maxAmmo - currentAmmo;

                    if (ammoToAdd > 0) {
                        int ammoFound = 0;
                        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                            ItemStack stack = player.getInventory().getItem(i);
                            if (stack.is(ModItems.HEAVY_AMMO.get())) {
                                int count = stack.getCount();
                                int take = Math.min(ammoToAdd - ammoFound, count);
                                stack.shrink(take);
                                ammoFound += take;
                                if (ammoFound >= ammoToAdd) break;
                            }
                        }

                        if (ammoFound > 0) {
                            gunItem.setAmmo(heldItem, currentAmmo + ammoFound);
                        }
                    }
                }
            }
        }
    }
}
