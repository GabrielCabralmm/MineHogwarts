package com.gabriel.hogwartsmod.client;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.magic.MagicData;
import com.gabriel.hogwartsmod.network.ModNetwork;
import com.gabriel.hogwartsmod.network.SelectSlotPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = HogwartsMod.MOD_ID, value = Dist.CLIENT)
public final class ClientInputEvents {

    private ClientInputEvents() {
    }

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
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
}
