package com.gabriel.hogwartsmod.client;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.magic.MagicData;
import com.gabriel.hogwartsmod.network.ModNetwork;
import com.gabriel.hogwartsmod.network.SelectSlotPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import com.gabriel.hogwartsmod.client.gui.GrimoireScreen;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = HogwartsMod.MOD_ID, value = Dist.CLIENT)
public final class ClientInputEvents {

    private ClientInputEvents() {
    }

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.screen == null && com.gabriel.hogwartsmod.potion.CauldronCarry.isCarrying(mc.player)) {
            event.setCanceled(true);
            return;
        }
        if (mc.player == null || mc.screen != null || !ModKeys.isModifierDown()) {
            return;
        }
        MagicData data = MagicData.get(mc.player);
        if (data == null || mc.player.isUsingItem()) {
            return;
        }
        double delta = event.getScrollDelta();
        if (delta == 0) {
            return;
        }
        int next = data.getActiveSlot() + (delta > 0 ? -1 : 1);
        data.setActiveSlot(next);
        ModNetwork.CHANNEL.sendToServer(new SelectSlotPacket(data.getActiveSlot()));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        while (ModKeys.DODGE.consumeClick()) {
            if (mc.player != null && mc.screen == null) {
                ClientDodge.request();
            }
        }
        while (ModKeys.OPEN_GRIMOIRE.consumeClick()) {
            if (mc.player != null && mc.screen == null) {
                mc.setScreen(new GrimoireScreen());
            }
        }
    }
}
