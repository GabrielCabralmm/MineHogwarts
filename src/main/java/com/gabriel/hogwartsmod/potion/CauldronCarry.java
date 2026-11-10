package com.gabriel.hogwartsmod.potion;

import com.gabriel.hogwartsmod.HogwartsMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = HogwartsMod.MOD_ID)
public final class CauldronCarry {
    public static final String CARRIED = "NoColo";
    private static final UUID SLOW_ID = UUID.fromString("4f6b2c1e-8d0a-4c55-9a51-2f3c7d9e0b17");

    private CauldronCarry() {
    }

    public static boolean isCarried(ItemStack stack) {
        return stack.getItem() instanceof CauldronItem && stack.hasTag() && stack.getTag().getBoolean(CARRIED);
    }

    public static boolean isCarrying(Player player) {
        return carriedSlot(player) >= 0;
    }

    public static int carriedSlot(Player player) {
        var items = player.getInventory().items;
        for (int i = 0; i < items.size(); i++) {
            if (isCarried(items.get(i))) {
                return i;
            }
        }
        return isCarried(player.getOffhandItem()) ? 40 : -1;
    }

    private static void keepInHand(ServerPlayer player) {
        int slot = carriedSlot(player);
        var inv = player.getInventory();
        if (slot < 0 || slot == inv.selected) {
            return;
        }
        if (slot >= 9 || slot == 40) {
            ItemStack carried = slot == 40 ? player.getOffhandItem() : inv.items.get(slot);
            ItemStack current = inv.getSelected();
            if (slot == 40) {
                player.setItemInHand(InteractionHand.OFF_HAND, current);
            } else {
                inv.items.set(slot, current);
            }
            inv.items.set(inv.selected, carried);
            player.inventoryMenu.broadcastChanges();
            return;
        }
        inv.selected = slot;
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(slot));
    }

    // -- Lentidão

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        Player player = event.player;
        if (player instanceof ServerPlayer serverPlayer && player.containerMenu == player.inventoryMenu) {
            keepInHand(serverPlayer);
        }
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        boolean carrying = isCarrying(player);
        boolean has = speed.getModifier(SLOW_ID) != null;
        if (carrying && !has) {
            speed.addTransientModifier(new AttributeModifier(SLOW_ID, "hogwartsmod_caldeirao", -0.35, AttributeModifier.Operation.MULTIPLY_TOTAL));
        } else if (!carrying && has) {
            speed.removeModifier(SLOW_ID);
        }
        if (carrying && player.isSprinting()) {
            player.setSprinting(false);
        }
    }

    // -- Mãos ocupadas

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (isCarrying(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (isCarrying(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (isCarrying(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseEntity(PlayerInteractEvent.EntityInteract event) {
        if (isCarrying(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isCarrying(event.getEntity()) && event.getHand() == InteractionHand.OFF_HAND) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        ItemStack stack = event.getEntity().getItem();
        if (isCarried(stack) && event.getPlayer() instanceof ServerPlayer player) {
            event.setCanceled(true);
            if (player.getMainHandItem().isEmpty()) {
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            } else if (!player.getInventory().add(stack)) {
                player.drop(stack, false, false);
            }
            player.displayClientMessage(Component.translatable("message.hogwartsmod.cauldron_carry_drop").withStyle(ChatFormatting.GRAY), true);
        }
    }
}
