package com.gabriel.hogwartsmod.npc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public abstract class HogwartsNpc extends PathfinderMob implements GeoEntity {
    public static final int MAX_MODEL = 48;
    private static final EntityDataAccessor<String> MODEL = SynchedEntityData.defineId(HogwartsNpc.class, EntityDataSerializers.STRING);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.npc.parado");
    private static final RawAnimation TALK = RawAnimation.begin().thenPlay("animation.npc.falar");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    protected HogwartsNpc(EntityType<? extends HogwartsNpc> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0);
    }

    protected abstract String defaultModel();

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(MODEL, "");
    }

    public String getModel() {
        String model = entityData.get(MODEL);
        return model.isEmpty() ? defaultModel() : model;
    }

    public void setModel(String model) {
        String clean = model == null ? "" : model.strip().toLowerCase().replaceAll("[^a-z0-9_]", "_");
        entityData.set(MODEL, clean.length() > MAX_MODEL ? clean.substring(0, MAX_MODEL) : clean);
    }

    public String getNpcName() {
        return getCustomName() != null ? getCustomName().getString() : getName().getString();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8f, 1f));
        goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    public void talk() {
        triggerAnim("main", "falar");
    }

    // -- Animações

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4, state -> state.setAndContinue(IDLE))
                .triggerableAnim("falar", TALK));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // -- Comportamento

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player && player.isCreative() && player.isShiftKeyDown()) {
            discard();
            return true;
        }
        return source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurt(source, amount);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Modelo", entityData.get(MODEL));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setModel(tag.getString("Modelo"));
    }
}
