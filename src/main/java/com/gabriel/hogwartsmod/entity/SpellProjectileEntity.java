package com.gabriel.hogwartsmod.entity;

import com.gabriel.hogwartsmod.registry.ModEntities;
import com.gabriel.hogwartsmod.spell.Spell;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class SpellProjectileEntity extends ThrowableProjectile {
    private static final EntityDataAccessor<Integer> SPELL =
            SynchedEntityData.defineId(SpellProjectileEntity.class, EntityDataSerializers.INT);
    private static final int MAX_LIFETIME = 60;

    public SpellProjectileEntity(EntityType<? extends SpellProjectileEntity> type, Level level) {
        super(type, level);
    }

    public SpellProjectileEntity(Level level, LivingEntity caster, Spell spell) {
        super(ModEntities.SPELL_PROJECTILE.get(), caster, level);
        setSpell(spell);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(SPELL, Spell.EXPELLIARMUS.ordinal());
    }

    public Spell getSpell() {
        return Spell.values()[entityData.get(SPELL)];
    }

    public void setSpell(Spell spell) {
        entityData.set(SPELL, spell.ordinal());
    }

    @Override
    protected float getGravity() {
        return 0.0f;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            DustParticleOptions dust = new DustParticleOptions(getSpell().getColorVector(), 1.2f);
            Vec3 motion = getDeltaMovement();
            for (int i = 0; i < 4; i++) {
                double t = i / 4.0;
                level().addParticle(dust, getX() - motion.x * t, getY() - motion.y * t, getZ() - motion.z * t, 0, 0, 0);
            }
        } else if (tickCount > MAX_LIFETIME) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();
        if (!(target instanceof LivingEntity living) || target == getOwner()) {
            return;
        }
        switch (getSpell()) {
            case EXPELLIARMUS -> disarm(living);
            case STUPEFY -> stun(living);
            default -> {
            }
        }
    }

    private void disarm(LivingEntity target) {
        ItemStack held = target.getItemInHand(InteractionHand.MAIN_HAND);
        if (!held.isEmpty()) {
            ItemEntity drop = new ItemEntity(level(), target.getX(), target.getEyeY(), target.getZ(), held.copy());
            drop.setDeltaMovement(getDeltaMovement().normalize().scale(0.4).add(0, 0.3, 0));
            drop.setPickUpDelay(40);
            level().addFreshEntity(drop);
            target.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        target.knockback(0.8, -getDeltaMovement().x, -getDeltaMovement().z);
    }

    private void stun(LivingEntity target) {
        target.hurt(damageSources().indirectMagic(this, getOwner()), 2.0f);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 10));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 2));
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0));
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level() instanceof ServerLevel server) {
            DustParticleOptions dust = new DustParticleOptions(getSpell().getColorVector(), 1.6f);
            server.sendParticles(dust, getX(), getY(), getZ(), 30, 0.3, 0.3, 0.3, 0.05);
            server.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.0f, 1.2f);
            discard();
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Spell", getSpell().getId());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setSpell(Spell.byId(tag.getString("Spell")));
    }
}
