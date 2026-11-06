package com.gabriel.hogwartsmod.entity;

import com.gabriel.hogwartsmod.magic.MagicConfig;
import com.gabriel.hogwartsmod.network.ModNetwork;
import com.gabriel.hogwartsmod.network.OpenNpcPacket;
import com.gabriel.hogwartsmod.registry.ModItems;
import com.gabriel.hogwartsmod.spell.Spell;
import com.gabriel.hogwartsmod.spell.Spells;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class ProfessorNpcEntity extends com.gabriel.hogwartsmod.npc.HogwartsNpc {
    private final List<String> spells = new ArrayList<>();

    public ProfessorNpcEntity(EntityType<? extends ProfessorNpcEntity> type, Level level) {
        super(type, level);
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.WAND.get()));
        setDropChance(EquipmentSlot.MAINHAND, 0f);
    }

    @Override
    protected String defaultModel() {
        return "professor";
    }

    // -- Feitiços ensinados

    public List<String> getSpells() {
        return List.copyOf(spells);
    }

    public boolean teaches(Spell spell) {
        return spells.contains(spell.getId());
    }

    public void addSpell(Spell spell) {
        if (!spells.contains(spell.getId())) {
            spells.add(spell.getId());
            spells.sort((a, b) -> {
                Spell sa = Spells.get(a);
                Spell sb = Spells.get(b);
                return Integer.compare(sa == null ? 0 : sa.getRequiredLevel(), sb == null ? 0 : sb.getRequiredLevel());
            });
        }
    }

    public void removeSpell(Spell spell) {
        spells.remove(spell.getId());
    }

    // -- Interação

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            if (!MagicConfig.NPC_PROFESSOR_ENABLED.get()) {
                player.displayClientMessage(Component.translatable("entity.hogwartsmod.professor_npc.disabled"), true);
            } else {
                getLookControl().setLookAt(player);
                talk();
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new OpenNpcPacket(getId(), getNpcName(), getSpells()));
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    // -- Salvamento

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ListTag list = new ListTag();
        spells.forEach(id -> list.add(StringTag.valueOf(id)));
        tag.put("Spells", list);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        spells.clear();
        ListTag list = tag.getList("Spells", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            spells.add(list.getString(i));
        }
    }
}
