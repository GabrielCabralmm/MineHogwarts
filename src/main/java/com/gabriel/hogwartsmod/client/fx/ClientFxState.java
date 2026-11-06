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
        boolean dodge;
        long dodgeStart;
        boolean apparate;
        long apparateStart;
        long arriveStart = -1000;
    }

    public static float apparateProgress(int entityId, float partialTick) {
        State state = STATES.get(entityId);
        ClientLevel level = Minecraft.getInstance().level;
        if (state == null || !state.apparate || level == null) {
            return -1f;
        }
        float t = (level.getGameTime() - state.apparateStart + partialTick) / 40f;
        return t > 1.5f ? -1f : Math.min(1f, t);
    }

    public static float arrivalProgress(int entityId, float partialTick) {
        State state = STATES.get(entityId);
        ClientLevel level = Minecraft.getInstance().level;
        if (state == null || level == null) {
            return -1f;
        }
        float t = (level.getGameTime() - state.arriveStart + partialTick) / 10f;
        return t < 0 || t >= 1 ? -1f : t;
    }

    public static boolean isApparating(int entityId) {
        State state = STATES.get(entityId);
        ClientLevel level = Minecraft.getInstance().level;
        return state != null && state.apparate && level != null && level.getGameTime() - state.apparateStart < 60;
    }

    public static boolean isDodging(int entityId) {
        State state = STATES.get(entityId);
        ClientLevel level = Minecraft.getInstance().level;
        return state != null && state.dodge && level != null && level.getGameTime() - state.dodgeStart < 20;
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
            case ARRIVE -> {
                state.apparate = false;
                state.arriveStart = now;
            }
            case APPARATE -> {
                state.apparate = value;
                if (value) {
                    state.apparateStart = now;
                }
            }
            case DODGE -> {
                state.dodge = value;
                if (value) {
                    state.dodgeStart = now;
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
                if (entity == null && !state.lumos && !state.shield && !state.dodge && !state.apparate) {
                    it.remove();
                }
                continue;
            }
            if (state.shield && level.random.nextInt(2) == 0) {
                Vec3 look = player.getViewVector(1f);
                Vec3 right = look.cross(new Vec3(0, 1, 0));
                if (right.lengthSqr() > 1.0E-4) {
                    right = right.normalize();
                    Vec3 up = right.cross(look).normalize();
                    double a = level.random.nextDouble() * Math.PI * 2;
                    Vec3 p = player.getEyePosition(1f).add(look.scale(1.05)).add(0, -0.15, 0)
                            .add(right.scale(Math.cos(a) * 0.95)).add(up.scale(Math.sin(a) * 0.95));
                    level.addParticle(ModParticles.SPARK.get().with(0xBFDFFF, 0.08f, 10), p.x, p.y, p.z, 0, 0, 0);
                }
            }
            if (isApparating(entry.getKey())) {
                float t = Math.min(1f, (level.getGameTime() - state.apparateStart) / 40f);
                Vec3 core = player.position().add(0, 1.0, 0);
                int count = 1 + Math.round(t * t * 4);
                for (int i = 0; i < count; i++) {
                    double a = level.random.nextDouble() * Math.PI * 2;
                    double radius = 0.9 + level.random.nextDouble() * 0.9;
                    double h = (level.random.nextDouble() - 0.5) * 1.8 * (1 - 0.6 * t);
                    double px = core.x + Math.cos(a) * radius;
                    double py = core.y + h;
                    double pz = core.z + Math.sin(a) * radius;
                    double pull = 0.06 + 0.16 * t;
                    double swirl = 0.08 + 0.1 * t;
                    double vx = (core.x - px) * pull - Math.sin(a) * swirl;
                    double vy = (core.y - py) * pull;
                    double vz = (core.z - pz) * pull + Math.cos(a) * swirl;
                    int color = level.random.nextInt(3) == 0 ? 0x4A4458 : 0x241F2C;
                    level.addParticle(ModParticles.SMOKE.get().with(color, 0.1f + 0.1f * t, 7 + level.random.nextInt(3)),
                            px, py, pz, vx, vy, vz);
                }
                if (t > 0.4f && level.random.nextFloat() < t) {
                    double a = level.random.nextDouble() * Math.PI * 2;
                    double r = 0.2 + level.random.nextDouble() * 0.5;
                    int color = level.random.nextInt(3) == 0 ? 0xA8DCFF : 0x3C9BFF;
                    level.addParticle(ModParticles.BOLT.get().with(color, 0.1f + 0.06f * t, 2 + level.random.nextInt(2)),
                            core.x + Math.cos(a) * r, core.y + (level.random.nextDouble() - 0.5) * 1.2 * (1 - t), core.z + Math.sin(a) * r, 0, 0, 0);
                }
            }
            if (isDodging(entry.getKey())) {
                Vec3 base = player.position();
                Vec3 motion = player.getDeltaMovement();
                for (int i = 0; i < 14; i++) {
                    double ox = (level.random.nextDouble() - 0.5) * 0.8;
                    double oy = 0.1 + level.random.nextDouble() * 1.7;
                    double oz = (level.random.nextDouble() - 0.5) * 0.8;
                    int color = level.random.nextInt(3) == 0 ? 0x4A4458 : 0x2E2A36;
                    level.addParticle(ModParticles.SMOKE.get().with(color, 0.45f + level.random.nextFloat() * 0.45f, 16 + level.random.nextInt(10)),
                            base.x + ox, base.y + oy, base.z + oz, -motion.x * 0.15, 0.01, -motion.z * 0.15);
                }
                for (int i = 0; i < 5; i++) {
                    double bx = base.x + (level.random.nextDouble() - 0.5) * 0.7;
                    double by = base.y + 0.2 + level.random.nextDouble() * 1.5;
                    double bz = base.z + (level.random.nextDouble() - 0.5) * 0.7;
                    int color = level.random.nextInt(3) == 0 ? 0xA8DCFF : 0x3C9BFF;
                    level.addParticle(ModParticles.BOLT.get().with(color, 0.09f + level.random.nextFloat() * 0.07f, 2 + level.random.nextInt(3)),
                            bx, by, bz, 0, 0, 0);
                }
                if (level.random.nextInt(2) == 0) {
                    level.addParticle(ModParticles.SPARK.get().with(0x8A7FA8, 0.08f, 12), base.x, base.y + 1.0, base.z,
                            (level.random.nextFloat() - 0.5f) * 0.1, 0.03, (level.random.nextFloat() - 0.5f) * 0.1);
                }
            }
            if (state.lumos) {
                Vec3 tip = wandTip(player, 1f);
                float flicker = 0.9f + level.random.nextFloat() * 0.2f;
                level.addParticle(ModParticles.ORB.get().with(0xF4F8FF, 0.28f * flicker, 2), tip.x, tip.y, tip.z, 0, 0, 0);
                level.addParticle(ModParticles.GLOW.get().with(0x6E86B4, 0.6f * flicker, 2), tip.x, tip.y, tip.z, 0, 0, 0);
                if (level.random.nextInt(2) == 0) {
                    level.addParticle(ModParticles.SPARK.get().with(0xCFE4FF, 0.08f, 14), tip.x, tip.y, tip.z,
                            (level.random.nextFloat() - 0.5f) * 0.04, 0.02, (level.random.nextFloat() - 0.5f) * 0.04);
                }
            }
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        STATES.clear();
    }
}
