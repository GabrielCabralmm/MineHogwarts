package com.gabriel.hogwartsmod.zone;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.item.ZoneWandItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = HogwartsMod.MOD_ID)
public final class ZoneEvents {

    private ZoneEvents() {
    }

    @SubscribeEvent
    public static void leftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getItemStack().getItem() instanceof ZoneWandItem)) {
            return;
        }
        event.setCanceled(true);
        if (event.getLevel().isClientSide && event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START) {
            if (ZoneWandItem.canUse(event.getEntity())) {
                com.gabriel.hogwartsmod.network.ModNetwork.CHANNEL.sendToServer(new com.gabriel.hogwartsmod.network.ZoneMarkPacket(event.getPos(), event.getEntity().isShiftKeyDown()));
            } else {
                event.getEntity().displayClientMessage(net.minecraft.network.chat.Component.translatable("message.hogwartsmod.zone_staff_only")
                        .withStyle(net.minecraft.ChatFormatting.GRAY), true);
            }
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ZoneData.get(player.server).sendTo(player);
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ZoneSelections.clear(player);
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ZoneSelections.clear(player);
        }
    }
}
