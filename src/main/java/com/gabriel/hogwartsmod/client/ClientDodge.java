package com.gabriel.hogwartsmod.client;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.client.fx.ClientFxState;
import com.gabriel.hogwartsmod.magic.MagicData;
import com.gabriel.hogwartsmod.network.DodgePacket;
import com.gabriel.hogwartsmod.network.ModNetwork;
import com.gabriel.hogwartsmod.spell.Spells;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = HogwartsMod.MOD_ID, value = Dist.CLIENT)
public final class ClientDodge {
    private static Vec3 velocity = Vec3.ZERO;
    private static int remaining;

    private ClientDodge() {
    }

    public static void request() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || remaining > 0) {
            return;
        }
        MagicData data = MagicData.get(player);
        if (data == null || !data.knows(Spells.ESQUIVA) || data.getCooldownRemaining(Spells.ESQUIVA, player.level().getGameTime()) > 0) {
            return;
        }
        float left = player.input.leftImpulse;
        float forward = player.input.forwardImpulse;
        float sin = Mth.sin(player.getYRot() * Mth.DEG_TO_RAD);
        float cos = Mth.cos(player.getYRot() * Mth.DEG_TO_RAD);
        ModNetwork.CHANNEL.sendToServer(new DodgePacket(left * cos - forward * sin, forward * cos + left * sin));
    }

    public static void start(float dx, float dz, float distance, int ticks) {
        velocity = new Vec3(dx, 0, dz).normalize().scale(distance / Math.max(1, ticks));
        remaining = ticks;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (event.phase != TickEvent.Phase.START || player == null || remaining <= 0) {
            return;
        }
        remaining--;
        double y = Math.max(player.getDeltaMovement().y, -0.08);
        player.setDeltaMovement(velocity.x, y, velocity.z);
        if (remaining == 0) {
            player.setDeltaMovement(velocity.x * 0.25, player.getDeltaMovement().y, velocity.z * 0.25);
        }
    }

    @SubscribeEvent
    public static void hidePlayer(RenderPlayerEvent.Pre event) {
        if (ClientFxState.isDodging(event.getEntity().getId())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void hideHand(RenderHandEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && (remaining > 0 || ClientFxState.isDodging(player.getId()))) {
            event.setCanceled(true);
        }
    }
}
