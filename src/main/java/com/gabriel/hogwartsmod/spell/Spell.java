package com.gabriel.hogwartsmod.spell;

import com.gabriel.hogwartsmod.entity.SpellProjectileEntity;
import com.gabriel.hogwartsmod.magic.MagicData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public abstract class Spell {
    private final String id;
    private final SpellCategory category;
    private final SpellDelivery delivery;
    private final int requiredLevel;
    private final float cost;
    private final int cooldownTicks;
    private final int maxMastery;
    private final int masteryBase;
    private final int color;

    protected Spell(String id, SpellCategory category, SpellDelivery delivery, int requiredLevel, float cost,
                    int cooldownTicks, int maxMastery, int masteryBase, int color) {
        this.id = id;
        this.category = category;
        this.delivery = delivery;
        this.requiredLevel = requiredLevel;
        this.cost = cost;
        this.cooldownTicks = cooldownTicks;
        this.maxMastery = maxMastery;
        this.masteryBase = masteryBase;
        this.color = color;
    }

    public String getId() {
        return id;
    }

    public SpellCategory getCategory() {
        return category;
    }

    public SpellDelivery getDelivery() {
        return delivery;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public float getCost() {
        return cost;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    public int getMaxMastery() {
        return maxMastery;
    }

    public int getMasteryBase() {
        return masteryBase;
    }

    public int getColor() {
        return color;
    }

    public boolean isAbility() {
        return false;
    }

    public boolean isSustained() {
        return delivery.isSustained();
    }

    public Vector3f getColorVector() {
        return new Vector3f(((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f);
    }

    public Component getDisplayName() {
        return Component.translatable("spell.hogwartsmod." + id);
    }

    public int getXpPerUse() {
        return 2 + requiredLevel;
    }

    public boolean isFreeCast(MagicData data, long gameTime) {
        return false;
    }

    public void cast(ServerPlayer caster, MagicData data) {
    }

    public boolean startHold(ServerPlayer caster, MagicData data) {
        return true;
    }

    public boolean tickHold(ServerPlayer caster, MagicData data, int ticksHeld) {
        return true;
    }

    public void stopHold(ServerPlayer caster, MagicData data) {
    }

    public void onProjectileHit(SpellProjectileEntity projectile, LivingEntity target, @Nullable ServerPlayer caster) {
    }

    public float getShieldDamage() {
        return 0f;
    }

    protected static Vec3 wandTip(ServerPlayer player) {
        return player.getEyePosition().add(player.getLookAngle().scale(0.8)).add(0, -0.2, 0);
    }
}
