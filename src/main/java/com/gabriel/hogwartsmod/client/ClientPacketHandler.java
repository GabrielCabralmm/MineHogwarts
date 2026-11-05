package com.gabriel.hogwartsmod.client;

import com.gabriel.hogwartsmod.magic.MagicData;
import net.minecraft.client.Minecraft;
import com.gabriel.hogwartsmod.client.fx.ClientFxState;
import com.gabriel.hogwartsmod.network.PlayerFxPacket;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.nbt.CompoundTag;
import com.gabriel.hogwartsmod.client.gui.TraceScreen;
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
