package com.gabriel.hogwartsmod.mission;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.UUID;

public record Mission(String id, String title, String description, ObjectiveType type, String target, int amount,
                      int galeoes, int sicles, int nuques, boolean repeatable, int cooldownHours) {
    public static final int MAX_TITLE = 32;
    public static final int MAX_DESCRIPTION = 120;
    public static final int MAX_TARGET = 64;

    public static Mission blank() {
        return new Mission(UUID.randomUUID().toString().substring(0, 8), "", "", ObjectiveType.ENTREGAR, "minecraft:feather", 5,
                0, 5, 0, false, 24);
    }

    public Mission sanitized() {
        return new Mission(id, cut(title, MAX_TITLE), cut(description, MAX_DESCRIPTION), type, cut(target.strip(), MAX_TARGET),
                type == ObjectiveType.IR ? 1 : Math.max(1, Math.min(999, amount)),
                clamp(galeoes), clamp(sicles), clamp(nuques), repeatable, Math.max(0, Math.min(24 * 30, cooldownHours)));
    }

    private static String cut(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(9999, v));
    }

    public Item item() {
        ResourceLocation id = ResourceLocation.tryParse(target);
        Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
        return item == null ? Items.AIR : item;
    }

    public EntityType<?> entity() {
        ResourceLocation id = ResourceLocation.tryParse(target);
        return id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id) ? null : BuiltInRegistries.ENTITY_TYPE.get(id);
    }

    public Component targetName() {
        return switch (type) {
            case ENTREGAR -> item() == Items.AIR ? Component.literal(target) : item().getDescription();
            case DERROTAR -> entity() == null ? Component.literal(target) : entity().getDescription();
            case IR -> Component.literal(target);
        };
    }

    public Component objectiveText() {
        return Component.translatable("mission.hogwartsmod.objective." + type.getId(), amount, targetName());
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Id", id);
        tag.putString("Titulo", title);
        tag.putString("Descricao", description);
        tag.putString("Tipo", type.getId());
        tag.putString("Alvo", target);
        tag.putInt("Quantidade", amount);
        tag.putInt("Galeoes", galeoes);
        tag.putInt("Sicles", sicles);
        tag.putInt("Nuques", nuques);
        tag.putBoolean("Repetivel", repeatable);
        tag.putInt("EsperaHoras", cooldownHours);
        return tag;
    }

    public static Mission load(CompoundTag tag) {
        return new Mission(tag.getString("Id"), tag.getString("Titulo"), tag.getString("Descricao"), ObjectiveType.byId(tag.getString("Tipo")),
                tag.getString("Alvo"), tag.getInt("Quantidade"), tag.getInt("Galeoes"), tag.getInt("Sicles"), tag.getInt("Nuques"),
                tag.getBoolean("Repetivel"), tag.getInt("EsperaHoras"));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeNbt(save());
    }

    public static Mission read(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return load(tag == null ? new CompoundTag() : tag);
    }

    public Mission withTitle(String v) { return new Mission(id, v, description, type, target, amount, galeoes, sicles, nuques, repeatable, cooldownHours); }
    public Mission withDescription(String v) { return new Mission(id, title, v, type, target, amount, galeoes, sicles, nuques, repeatable, cooldownHours); }
    public Mission withType(ObjectiveType v) { return new Mission(id, title, description, v, target, amount, galeoes, sicles, nuques, repeatable, cooldownHours); }
    public Mission withTarget(String v) { return new Mission(id, title, description, type, v, amount, galeoes, sicles, nuques, repeatable, cooldownHours); }
    public Mission withAmount(int v) { return new Mission(id, title, description, type, target, v, galeoes, sicles, nuques, repeatable, cooldownHours); }
    public Mission withReward(int g, int s, int n) { return new Mission(id, title, description, type, target, amount, g, s, n, repeatable, cooldownHours); }
    public Mission withRepeatable(boolean v, int hours) { return new Mission(id, title, description, type, target, amount, galeoes, sicles, nuques, v, hours); }
}
