package com.gabriel.hogwartsmod.creature;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.magic.MagicData;
import com.gabriel.hogwartsmod.zone.ZoneRule;
import com.gabriel.hogwartsmod.zone.Zones;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = HogwartsMod.MOD_ID)
public final class CreatureManager {
    private static final double SUMMON_RANGE = 16.0;

    private CreatureManager() {
    }

    private static void say(ServerPlayer player, Component text, ChatFormatting color) {
        player.displayClientMessage(text.copy().withStyle(color), true);
    }

    // -- Guardar

    public static boolean store(ServerPlayer player, TamableAnimal creature) {
        Species species = Species.of(creature);
        MagicData data = MagicData.get(player);
        if (species == null || data == null || !creature.isOwnedBy(player)) {
            return false;
        }
        if (creature instanceof OwlEntity owl && owl.isBusy()) {
            say(player, Component.translatable("message.hogwartsmod.owl_busy"), ChatFormatting.GRAY);
            return false;
        }
        CreatureStore store = data.getCreatures();
        CreatureStore.Entry current = store.get(species);
        if (current != null && !creature.getUUID().equals(current.summoned())) {
            say(player, Component.translatable("message.hogwartsmod.creature_slot_taken", species.getDisplayName()), ChatFormatting.GRAY);
            return false;
        }
        creature.ejectPassengers();
        CompoundTag tag = new CompoundTag();
        creature.saveWithoutId(tag);
        tag.remove("UUID");
        tag.remove("Pos");
        tag.remove("Motion");
        tag.remove("Passengers");
        tag.remove("Leash");
        store.put(new CreatureStore.Entry(species, tag, creature.getMaxHealth(), null));
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.PORTAL, creature.getX(), creature.getY() + creature.getBbHeight() / 2, creature.getZ(), 40, 0.5, 0.6, 0.5, 0.4);
        level.sendParticles(ParticleTypes.END_ROD, creature.getX(), creature.getY() + creature.getBbHeight() / 2, creature.getZ(), 12, 0.4, 0.4, 0.4, 0.05);
        level.playSound(null, creature.blockPosition(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 1f, 0.7f);
        level.playSound(null, creature.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5f, 1.6f);
        creature.discard();
        data.markForSync();
        say(player, Component.translatable("message.hogwartsmod.creature_stored", store.get(species).name()), ChatFormatting.GOLD);
        return true;
    }

    // -- Compra

    public static boolean giveTamed(ServerPlayer player, Species species) {
        MagicData data = MagicData.get(player);
        if (data == null || data.getCreatures().get(species) != null) {
            return false;
        }
        if (!(species.getType().create(player.level()) instanceof TamableAnimal creature)) {
            return false;
        }
        creature.tame(player);
        creature.setHealth(creature.getMaxHealth());
        CompoundTag tag = new CompoundTag();
        creature.saveWithoutId(tag);
        tag.remove("UUID");
        tag.remove("Pos");
        tag.remove("Motion");
        data.getCreatures().put(new CreatureStore.Entry(species, tag, creature.getMaxHealth(), null));
        creature.discard();
        data.markForSync();
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 12, 0.4, 0.5, 0.4, 0.05);
        level.playSound(null, player.blockPosition(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 1f, 0.7f);
        say(player, Component.translatable("message.hogwartsmod.creature_bought", species.getDisplayName()), ChatFormatting.GOLD);
        return true;
    }

    // -- Invocar

    public static void summon(ServerPlayer player, String speciesId) {
        Species species = Species.byId(speciesId);
        MagicData data = MagicData.get(player);
        if (species == null || data == null) {
            return;
        }
        CreatureStore.Entry entry = data.getCreatures().get(species);
        if (entry == null || entry.summoned() != null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(SUMMON_RANGE));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 pos;
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos base = hit.getDirection() == Direction.UP ? hit.getBlockPos().above() : hit.getBlockPos().relative(hit.getDirection());
            pos = Vec3.atBottomCenterOf(base);
        } else {
            Vec3 front = player.position().add(player.getLookAngle().multiply(1, 0, 1).normalize().scale(3));
            pos = new Vec3(front.x, player.getY(), front.z);
        }
        if (Zones.blocks(level, pos, ZoneRule.CRIATURAS) || Zones.blocks(player, ZoneRule.CRIATURAS)) {
            Zones.warn(player, ZoneRule.CRIATURAS);
            return;
        }
        Entity created = species.getType().create(level);
        if (!(created instanceof LivingEntity living)) {
            return;
        }
        living.load(entry.data());
        living.setUUID(java.util.UUID.randomUUID());
        float yaw = player.getYRot() + 180f;
        living.moveTo(pos.x, pos.y, pos.z, yaw, 0f);
        living.setYHeadRot(yaw);
        living.setYBodyRot(yaw);
        living.setDeltaMovement(Vec3.ZERO);
        living.fallDistance = 0;
        if (living instanceof TamableAnimal tame) {
            tame.setOwnerUUID(player.getUUID());
            tame.setTame(true);
            tame.setOrderedToSit(false);
        }
        if (!level.noCollision(living)) {
            living.moveTo(player.getX(), player.getY(), player.getZ(), yaw, 0f);
        }
        level.addFreshEntity(living);
        data.getCreatures().put(entry.withSummoned(living.getUUID()));
        data.markForSync();
        level.sendParticles(ParticleTypes.PORTAL, pos.x, pos.y + 0.8, pos.z, 50, 0.6, 0.8, 0.6, 0.6);
        level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y + 0.8, pos.z, 16, 0.5, 0.6, 0.5, 0.06);
        level.playSound(null, BlockPos.containing(pos), SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.PLAYERS, 1f, 0.7f);
        level.playSound(null, BlockPos.containing(pos), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5f, 1.3f);
    }

    // -- Morte e saída

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof TamableAnimal creature) || creature.level().isClientSide || Species.of(creature) == null) {
            return;
        }
        if (creature.getOwner() instanceof ServerPlayer owner) {
            MagicData data = MagicData.get(owner);
            Species species = Species.of(creature);
            if (data != null) {
                CreatureStore.Entry entry = data.getCreatures().get(species);
                if (entry != null && creature.getUUID().equals(entry.summoned())) {
                    data.getCreatures().remove(species);
                    data.markForSync();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            recallAll(player);
        }
    }

    public static void recallAll(ServerPlayer player) {
        MagicData data = MagicData.get(player);
        if (data == null) {
            return;
        }
        for (CreatureStore.Entry entry : data.getCreatures().all()) {
            if (entry.summoned() == null) {
                continue;
            }
            Entity entity = player.serverLevel().getEntity(entry.summoned());
            if (entity instanceof TamableAnimal creature && creature.isAlive()) {
                store(player, creature);
            }
        }
    }
}
