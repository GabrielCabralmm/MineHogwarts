package com.gabriel.hogwartsmod.registry;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.potion.AddItemLootModifier;
import com.mojang.serialization.Codec;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModLootModifiers {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, HogwartsMod.MOD_ID);

    public static final RegistryObject<Codec<AddItemLootModifier>> ADD_ITEM = SERIALIZERS.register("adicionar_item", AddItemLootModifier.CODEC);

    public static final RegistryObject<Codec<com.gabriel.hogwartsmod.wand.AddDropLootModifier>> ADD_DROP = SERIALIZERS.register("adicionar_drop", com.gabriel.hogwartsmod.wand.AddDropLootModifier.CODEC);

    private ModLootModifiers() {
    }
}
