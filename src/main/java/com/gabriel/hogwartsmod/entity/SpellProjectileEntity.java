package com.gabriel.hogwartsmod.entity;

import com.gabriel.hogwartsmod.registry.ModEntities;
import com.gabriel.hogwartsmod.spell.Spell;
import com.gabriel.hogwartsmod.spell.SpellCategory;
import com.gabriel.hogwartsmod.spell.Spells;
import com.gabriel.hogwartsmod.spell.impl.ProtegoSpell;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class SpellProjectileEntity extends ThrowableProjectile {
    private static final EntityDataAccessor<String> SPELL =
            SynchedEntityData.defineId(SpellProjectileEntity.class, EntityDataSerializers.STRING);
    private static final int MAX_LIFETIME = 60;
    private boolean reflected;

    public SpellProjectileEntity(EntityType<? extends SpellProjectileEntity> type, Level level) {
        super(type, level);
    }

    public SpellProjectileEntity(Level level, LivingEntity caster, Spell spell) {
        super(ModEntities.SPELL_PROJECTILE.get(), caster, level);
        entityData.set(SPELL, spell.getId());
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(SPELL, Spells.STUPEFY.getId());
    }

    @Nullable
    public Spell getSpell() {
        return Spells.get(entityData.get(SPELL));
    }

    @Override
    protected float getGravity() {
        return 0.0f;
    }

    @Override
    public void tick() {
        super.tick();
        Spell spell = getSpell();
        if (level().isClientSide && spell != null) {
            DustParticleOptions dust = new DustParticleOptions(spell.getColorVector(), 1.2f);
            Vec3 motion = getDeltaMovement();
            for (int i = 0; i < 4; i++) {
                double t = i / 4.0;
                level().addParticle(dust, getX() - motion.x * t, getY() - motion.y * t, getZ() - motion.z * t, 0, 0, 0);
            }
        } else if (!level().isClientSide && tickCount > MAX_LIFETIME) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        Spell spell = getSpell();
        if (spell == null || !(target instanceof LivingEntity living) || target == getOwner() || level().isClientSide) {
            return;
        }
        ServerPlayer caster = getOwner() instanceof ServerPlayer sp ? sp : null;
        if (living instanceof Player defender) {
            ProtegoSpell.BlockResult block = ProtegoSpell.tryBlock(defender, position(), spell.getCategory(), spell.getShieldDamage());
            if (block == ProtegoSpell.BlockResult.REFLECTED) {
                setOwner(defender);
                setDeltaMovement(getDeltaMovement().scale(-1));
                tickCount = 0;
                reflected = true;
                return;
            }
            if (block != ProtegoSpell.BlockResult.NONE && spell.getCategory() != SpellCategory.FORCA) {
                return;
            }
        }
        spell.onProjectileHit(this, living, caster);
    }

    @Override
    protected void onHit(HitResult result) {
        reflected = false;
        super.onHit(result);
        if (level() instanceof ServerLevel server && isAlive()) {
            if (reflected || (result instanceof EntityHitResult ehr && ehr.getEntity() == getOwner())) {
                return;
            }
            Spell spell = getSpell();
            if (spell != null) {
                DustParticleOptions dust = new DustParticleOptions(spell.getColorVector(), 1.6f);
                server.sendParticles(dust, getX(), getY(), getZ(), 30, 0.3, 0.3, 0.3, 0.05);
            }
            server.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.0f, 1.2f);
            discard();
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Spell", entityData.get(SPELL));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Spell")) {
            entityData.set(SPELL, tag.getString("Spell"));
        }
    }
}
