package com.gabriel.hogwartsmod.client;

import com.gabriel.hogwartsmod.magic.MagicData;
import net.minecraft.client.Minecraft;
import com.gabriel.hogwartsmod.client.fx.ClientFxState;
import com.gabriel.hogwartsmod.network.PlayerFxPacket;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.nbt.CompoundTag;
import com.gabriel.hogwartsmod.client.gui.TraceScreen;
import com.gabriel.hogwartsmod.client.gui.NpcScreen;
import com.gabriel.hogwartsmod.client.gui.PulpitScreen;
import com.gabriel.hogwartsmod.block.PulpitBlockEntity;
import net.minecraft.core.BlockPos;
import com.gabriel.hogwartsmod.client.trace.ClientTeachState;
import com.gabriel.hogwartsmod.network.StudentsPacket;
import com.gabriel.hogwartsmod.spell.Spell;
import com.gabriel.hogwartsmod.spell.Spells;
import com.gabriel.hogwartsmod.teach.TeachRules;

import java.util.List;

public final class ClientPacketHandler {

    private ClientPacketHandler() {
    }

    public static void handleSync(CompoundTag tag) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            MagicData data = MagicData.get(player);
            if (data != null) {
                data.load(tag);
            }
        }
        if (tag.contains("TeachMinLevel")) {
            ClientTeachState.limits = new TeachRules.Limits(tag.getInt("TeachMinLevel"), tag.getInt("TeachMaxSpellLevel"));
            ClientTeachState.range = tag.getInt("TeachRange");
        }
    }

    public static void handleStudents(String spell, List<StudentsPacket.Entry> students) {
        ClientTeachState.spell = spell;
        ClientTeachState.students = students;
    }

    public static void handleOpenPulpit(BlockPos pos, boolean canEdit) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(pos) instanceof PulpitBlockEntity pulpit) {
            mc.setScreen(new PulpitScreen(pos, canEdit, pulpit));
        }
    }

    public static void handleOpenNpc(int entity, String name, List<String> spells) {
        Minecraft.getInstance().setScreen(new NpcScreen(entity, name, spells));
    }

    public static void handleDodgeMove(float dx, float dy, float dz, float distance, int ticks) {
        ClientDodge.start(dx, dy, dz, distance, ticks);
    }

    public static void handleZoneSelection(net.minecraft.core.BlockPos first, net.minecraft.core.BlockPos second, boolean cut) {
        com.gabriel.hogwartsmod.client.zone.ClientZones.cut = cut;
        com.gabriel.hogwartsmod.client.zone.ClientZones.first = first;
        com.gabriel.hogwartsmod.client.zone.ClientZones.second = second;
    }

    public static void handleHouse(java.util.UUID player, String house) {
        com.gabriel.hogwartsmod.house.House value = com.gabriel.hogwartsmod.house.House.byId(house);
        if (value == null) {
            com.gabriel.hogwartsmod.house.HouseManager.CLIENT_HOUSES.remove(player);
        } else {
            com.gabriel.hogwartsmod.house.HouseManager.CLIENT_HOUSES.put(player, value);
        }
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level != null) {
            net.minecraft.world.entity.player.Player p = mc.level.getPlayerByUUID(player);
            if (p != null) {
                p.refreshDisplayName();
            }
        }
    }

    public static void handleSortingHat(int entity, boolean on) {
        com.gabriel.hogwartsmod.client.house.ClientSorting.setWearing(entity, on);
    }

    public static void handleOpenSorting(java.util.UUID target, String name, String house) {
        net.minecraft.client.Minecraft.getInstance().setScreen(new com.gabriel.hogwartsmod.client.gui.SortingScreen(target, name,
                com.gabriel.hogwartsmod.house.House.byId(house)));
    }

    public static void handlePublicLocations(List<com.gabriel.hogwartsmod.apparition.ApparitionPoint> points) {
        com.gabriel.hogwartsmod.client.apparition.ClientApparitionState.publicPoints = List.copyOf(points);
    }

    public static void handleOpenTrace(String spell, String teacher, int attempts) {
        Spell resolved = Spells.get(spell);
        if (resolved != null) {
            Minecraft.getInstance().setScreen(new TraceScreen(resolved, teacher, attempts));
        }
    }

    public static void handleCastAnimation(int entityId, String animation) {
        var level = Minecraft.getInstance().level;
        if (level != null && level.getEntity(entityId) instanceof AbstractClientPlayer player) {
            CastAnimations.play(player, animation);
        }
    }

    public static void handlePlayerFx(int entityId, PlayerFxPacket.Kind kind, boolean value) {
        ClientFxState.apply(entityId, kind, value);
    }
}
