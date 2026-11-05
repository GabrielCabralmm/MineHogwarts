package com.gabriel.hogwartsmod.client.fx;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.network.PlayerFxPacket;
import com.gabriel.hogwartsmod.particle.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Mod.EventBusSubscriber(modid = HogwartsMod.MOD_ID, value = Dist.CLIENT)
public final class ClientFxState {
    private static final Map<Integer, State> STATES = new HashMap<>();

    public static final class State {
        boolean lumos;
        boolean shield;
        long shieldStart;
        long lastHit = -100;
        boolean lastHitPerfect;
    }

    private ClientFxState() {
    }

    static Map<Integer, State> states() {
        return STATES;
    }

    public static void apply(int entityId, PlayerFxPacket.Kind kind, boolean value) {
        ClientLevel level = Minecraft.getInstance().level;
        long now = level == null ? 0 : level.getGameTime();
        State state = STATES.computeIfAbsent(entityId, id -> new State());
        switch (kind) {
            case LUMOS -> state.lumos = value;
            case SHIELD -> {
                state.shield = value;
                if (value) {
                    state.shieldStart = now;
                }
            }
            case SHIELD_HIT -> {
                state.lastHit = now;
                state.lastHitPerfect = value;
            }
        }
    }

    public static Vec3 wandTip(Player player, float partialTick) {
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 look = player.getViewVector(partialTick);
        float yaw = (float) Math.toRadians(player.getViewYRot(partialTick));
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        return eye.add(look.scale(0.75)).add(right.scale(0.32)).add(0, -0.28, 0);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || mc.level == null || mc.isPaused()) {
            return;
        }
        ClientLevel level = mc.level;
        Iterator<Map.Entry<Integer, State>> it = STATES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, State> entry = it.next();
            Entity entity = level.getEntity(entry.getKey());
            State state = entry.getValue();
            if (!(entity instanceof Player player) || !player.isAlive()) {
                if (entity == null && !state.lumos && !state.shield) {
                    it.remove();
                }
                continue;
            }
            if (state.lumos) {
                Vec3 tip = wandTip(player, 1f);
                float flicker = 0.9f + level.random.nextFloat() * 0.2f;
                level.addParticle(ModParticles.GLOW.get().with(0xEAF3FF, 0.22f * flicker, 3), tip.x, tip.y, tip.z, 0, 0, 0);
                level.addParticle(ModParticles.GLOW.get().with(0x2C3D5C, 1.1f * flicker, 3), tip.x, tip.y, tip.z, 0, 0, 0);
                if (level.random.nextInt(4) == 0) {
                    level.addParticle(ModParticles.SPARK.get().with(0xCFE4FF, 0.06f, 14), tip.x, tip.y, tip.z,
                            (level.random.nextFloat() - 0.5f) * 0.02, 0.01, (level.random.nextFloat() - 0.5f) * 0.02);
                }
            }
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        STATES.clear();
    }
}
