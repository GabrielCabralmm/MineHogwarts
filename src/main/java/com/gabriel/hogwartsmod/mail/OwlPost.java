package com.gabriel.hogwartsmod.mail;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.creature.OwlEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = HogwartsMod.MOD_ID)
public class OwlPost extends SavedData {
    private static final String NAME = HogwartsMod.MOD_ID + "_corujal";
    private final List<Flight> flights = new ArrayList<>();

    private static final class Flight {
        CompoundTag owl;
        UUID target;
        long arriveAt;

        Flight(CompoundTag owl, UUID target, long arriveAt) {
            this.owl = owl;
            this.target = target;
            this.arriveAt = arriveAt;
        }
    }

    public static OwlPost get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(OwlPost::load, OwlPost::new, NAME);
    }

    // -- Partida

    public static void depart(OwlEntity owl) {
        ServerLevel level = (ServerLevel) owl.level();
        MinecraftServer server = level.getServer();
        UUID target = owl.nextTarget();
        poof(level, owl.position());
        if (target == null) {
            owl.discard();
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(target);
        long delay = 300;
        if (player != null && player.level() == level) {
            delay = Math.max(60, Math.min(600, 60 + (long) (player.position().distanceTo(owl.position()) / 6)));
        }
        CompoundTag tag = new CompoundTag();
        owl.save(tag);
        OwlPost post = get(server);
        post.flights.add(new Flight(tag, target, server.overworld().getGameTime() + delay));
        post.setDirty();
        owl.discard();
    }

    private static void poof(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.POOF, pos.x, pos.y + 0.4, pos.z, 14, 0.25, 0.25, 0.25, 0.04);
        level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y + 0.4, pos.z, 6, 0.3, 0.3, 0.3, 0.03);
        level.playSound(null, BlockPos.containing(pos), SoundEvents.PARROT_FLY, SoundSource.NEUTRAL, 1f, 0.7f);
        level.playSound(null, BlockPos.containing(pos), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.NEUTRAL, 0.5f, 1.4f);
    }

    // -- Chegada

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 10 != 0) {
            return;
        }
        MinecraftServer server = event.getServer();
        OwlPost post = get(server);
        if (post.flights.isEmpty()) {
            return;
        }
        long now = server.overworld().getGameTime();
        Iterator<Flight> it = post.flights.iterator();
        while (it.hasNext()) {
            Flight flight = it.next();
            if (flight.arriveAt > now) {
                continue;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(flight.target);
            if (player != null && player.isAlive()) {
                if (spawnNear(player, flight.owl)) {
                    it.remove();
                    post.setDirty();
                }
                continue;
            }
            if (!flight.owl.getBoolean("Voltando") && flight.owl.hasUUID("Owner")) {
                flight.owl.putBoolean("Voltando", true);
                flight.target = flight.owl.getUUID("Owner");
                flight.arriveAt = now + 200;
                post.setDirty();
            }
        }
    }

    private static boolean spawnNear(ServerPlayer player, CompoundTag tag) {
        ServerLevel level = player.serverLevel();
        Entity entity = EntityType.loadEntityRecursive(tag, level, e -> e);
        if (!(entity instanceof OwlEntity owl)) {
            return true;
        }
        double angle = player.getRandom().nextDouble() * Math.PI * 2;
        Vec3 pos = player.position().add(Math.cos(angle) * 7, 4, Math.sin(angle) * 7);
        for (int i = 0; i < 6 && !level.noCollision(owl, owl.getDimensions(owl.getPose()).makeBoundingBox(pos)); i++) {
            pos = pos.add(0, -1, 0).lerp(player.position().add(0, 1.5, 0), 0.3);
        }
        owl.moveTo(pos.x, pos.y, pos.z, owl.getYRot(), 0);
        owl.setDeltaMovement(Vec3.ZERO);
        owl.startApproach();
        if (!level.addFreshEntity(owl)) {
            return false;
        }
        poof(level, pos);
        return true;
    }

    // -- NBT

    public static OwlPost load(CompoundTag tag) {
        OwlPost post = new OwlPost();
        ListTag list = tag.getList("Voos", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag f = list.getCompound(i);
            post.flights.add(new Flight(f.getCompound("Coruja"), f.getUUID("Alvo"), f.getLong("Chegada")));
        }
        return post;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Flight flight : flights) {
            CompoundTag f = new CompoundTag();
            f.put("Coruja", flight.owl);
            f.putUUID("Alvo", flight.target);
            f.putLong("Chegada", flight.arriveAt);
            list.add(f);
        }
        tag.put("Voos", list);
        return tag;
    }
}
