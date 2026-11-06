package com.gabriel.hogwartsmod.client.apparition;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.client.ModKeys;
import com.gabriel.hogwartsmod.client.fx.ClientFxState;
import com.gabriel.hogwartsmod.client.gui.ApparitionWheelScreen;
import com.gabriel.hogwartsmod.magic.MagicData;
import com.gabriel.hogwartsmod.network.ApparatePacket;
import com.gabriel.hogwartsmod.network.ModNetwork;
import com.gabriel.hogwartsmod.spell.Spells;
import com.gabriel.hogwartsmod.spell.impl.AparatacaoSpell;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = HogwartsMod.MOD_ID, value = Dist.CLIENT)
public final class ClientApparition {
    private static final int HOLD_TICKS = 6;
    private static int held = -1;

    private ClientApparition() {
    }

    private static boolean canUse(LocalPlayer player) {
        MagicData data = MagicData.get(player);
        return data != null && data.knows(Spells.APARATACAO);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || mc.player == null) {
            return;
        }
        while (ModKeys.APPARATE.consumeClick()) {
            if (mc.screen == null && held < 0 && canUse(mc.player)) {
                held = 0;
            }
        }
        if (held < 0) {
            return;
        }
        if (mc.screen != null) {
            held = -1;
            return;
        }
        if (ModKeys.isPhysicallyDown(ModKeys.APPARATE)) {
            held++;
            if (held >= HOLD_TICKS) {
                held = -1;
                mc.setScreen(new ApparitionWheelScreen());
            }
        } else {
            held = -1;
            ModNetwork.CHANNEL.sendToServer(new ApparatePacket(AparatacaoSpell.Mode.VISUAL, -1, ""));
        }
    }

    @SubscribeEvent
    public static void lockMovement(MovementInputUpdateEvent event) {
        if (ClientFxState.isApparating(event.getEntity().getId())) {
            event.getInput().forwardImpulse = 0;
            event.getInput().leftImpulse = 0;
            event.getInput().up = false;
            event.getInput().down = false;
            event.getInput().left = false;
            event.getInput().right = false;
            event.getInput().jumping = false;
            event.getInput().shiftKeyDown = false;
        }
    }
}
