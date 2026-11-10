package com.gabriel.hogwartsmod.creature;

import com.gabriel.hogwartsmod.magic.MagicData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class ThestralEntity extends MountCreature {

    public ThestralEntity(EntityType<? extends ThestralEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 44.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected boolean isTamingFood(ItemStack stack) {
        return stack.is(Items.ROTTEN_FLESH);
    }

    @Override
    protected String animationPrefix() {
        return "testralio";
    }

    @Override
    protected String messagePrefix() {
        return "thestral";
    }

    @Override
    protected float flightSpeed() {
        return 0.95f;
    }

    @Override
    protected boolean needsSight() {
        return true;
    }

    @Override
    public boolean canInteractWith(Player player) {
        MagicData data = MagicData.get(player);
        return data != null && data.hasSeenDeath();
    }

    // -- Sons

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return isFlyingVisual() ? SoundEvents.PHANTOM_FLAP : SoundEvents.HORSE_BREATHE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SKELETON_HORSE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SKELETON_HORSE_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.7f;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide && isFlyingVisual() && tickCount % 14 == 0) {
            level().playLocalSound(getX(), getY(), getZ(), SoundEvents.PHANTOM_FLAP, getSoundSource(), 0.6f, 0.8f, false);
        }
    }
}
