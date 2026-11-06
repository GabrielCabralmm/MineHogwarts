package com.gabriel.hogwartsmod.teach;

import com.gabriel.hogwartsmod.block.PulpitBlockEntity;
import com.gabriel.hogwartsmod.fx.SpellFx;
import com.gabriel.hogwartsmod.magic.MagicConfig;
import com.gabriel.hogwartsmod.magic.MagicData;
import com.gabriel.hogwartsmod.network.ModNetwork;
import com.gabriel.hogwartsmod.network.OpenTracePacket;
import com.gabriel.hogwartsmod.network.StudentsPacket;
import com.gabriel.hogwartsmod.particle.ModParticles;
import com.gabriel.hogwartsmod.spell.Spell;
import com.gabriel.hogwartsmod.spell.Spells;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TeachingManager {
    private record Invite(UUID teacher, String spell, boolean professor, boolean group, long expiresAt) {
    }

    private record Session(UUID teacher, String spell, boolean professor, boolean group) {
    }

    private static final Map<UUID, Invite> INVITES = new HashMap<>();
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private TeachingManager() {
    }

    // -- Situação dos alunos

    public static StudentStatus status(ServerPlayer student, Spell spell, UUID teacher) {
        MagicData data = MagicData.get(student);
        if (data == null || data.knows(spell)) {
            return StudentStatus.KNOWS;
        }
        if (SESSIONS.containsKey(student.getUUID())) {
            return StudentStatus.IN_LESSON;
        }
        Invite invite = activeInvite(student);
        if (invite != null && invite.teacher().equals(teacher)) {
            return StudentStatus.INVITED;
        }
        if (data.getLevel() < spell.getRequiredLevel()) {
            return StudentStatus.LOW_LEVEL;
        }
        return invite != null ? StudentStatus.IN_LESSON : StudentStatus.CAN_LEARN;
    }

    public static void sendNearby(ServerPlayer teacher, Spell spell) {
        double range = MagicConfig.INVITE_RANGE.get();
        List<StudentsPacket.Entry> entries = new ArrayList<>();
        teacher.serverLevel().players().stream()
                .filter(p -> p != teacher && p.distanceTo(teacher) <= range && !p.isSpectator())
                .sorted(Comparator.comparingDouble(p -> p.distanceTo(teacher)))
                .forEach(p -> entries.add(new StudentsPacket.Entry(p.getUUID(), p.getGameProfile().getName(),
                        status(p, spell, teacher.getUUID()))));
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> teacher), new StudentsPacket(spell.getId(), entries));
    }

    // -- Convite

    public static void invite(ServerPlayer teacher, ServerPlayer student, Spell spell) {
        MagicData teacherData = MagicData.get(teacher);
        if (teacherData == null) {
            return;
        }
        long now = teacher.level().getGameTime();
        TeachRules.Check check = TeachRules.check(teacherData, spell, TeachRules.Limits.fromConfig(), now);
        if (check != TeachRules.Check.OK) {
            teacher.displayClientMessage(Component.translatable(check.key(), spell.getDisplayName()), true);
            return;
        }
        if (student == teacher || student.level() != teacher.level() || student.distanceTo(teacher) > MagicConfig.INVITE_RANGE.get()) {
            teacher.displayClientMessage(Component.translatable("teach.hogwartsmod.too_far", student.getDisplayName()), true);
            return;
        }
        StudentStatus status = status(student, spell, teacher.getUUID());
        if (status != StudentStatus.CAN_LEARN) {
            teacher.displayClientMessage(Component.translatable(status.key()).append(" · ").append(student.getDisplayName()), true);
            return;
        }
        sendInvite(teacher, student, spell, teacherData.isProfessor(), false, now);
        teacher.displayClientMessage(Component.translatable("teach.hogwartsmod.invite_sent", student.getDisplayName()), true);
        sendNearby(teacher, spell);
    }

    private static void sendInvite(ServerPlayer teacher, ServerPlayer student, Spell spell, boolean professor, boolean group, long now) {
        long expiresAt = now + MagicConfig.INVITE_SECONDS.get() * 20L;
        INVITES.put(student.getUUID(), new Invite(teacher.getUUID(), spell.getId(), professor, group, expiresAt));

        MutableComponent accept = Component.translatable("teach.hogwartsmod.accept").withStyle(Style.EMPTY
                .withColor(ChatFormatting.GREEN).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hogwarts aula aceitar"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("teach.hogwartsmod.accept_hover"))));
        MutableComponent decline = Component.translatable("teach.hogwartsmod.decline").withStyle(Style.EMPTY
                .withColor(ChatFormatting.RED).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hogwarts aula recusar")));
        student.sendSystemMessage(Component.translatable("teach.hogwartsmod.invite", teacher.getDisplayName(),
                spell.getDisplayName().copy().withStyle(ChatFormatting.GOLD)));
        student.sendSystemMessage(Component.literal("  ").append(accept).append("  ").append(decline));
        student.playNotifySound(SoundEvents.NOTE_BLOCK_CHIME.get(), SoundSource.PLAYERS, 0.8f, 1.4f);
    }

    // -- Aula no Púlpito

    private static List<ServerPlayer> inArea(ServerPlayer teacher, PulpitBlockEntity pulpit) {
        AABB area = pulpit.area();
        return teacher.serverLevel().players().stream()
                .filter(p -> p != teacher && !p.isSpectator() && area.contains(p.position()))
                .sorted(Comparator.comparing(p -> p.getGameProfile().getName()))
                .toList();
    }

    public static void sendInArea(ServerPlayer teacher, PulpitBlockEntity pulpit, Spell spell) {
        List<StudentsPacket.Entry> entries = new ArrayList<>();
        for (ServerPlayer p : inArea(teacher, pulpit)) {
            entries.add(new StudentsPacket.Entry(p.getUUID(), p.getGameProfile().getName(), status(p, spell, teacher.getUUID())));
        }
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> teacher), new StudentsPacket(spell.getId(), entries));
    }

    public static void startClass(ServerPlayer teacher, PulpitBlockEntity pulpit, Spell spell) {
        MagicData data = MagicData.get(teacher);
        if (data == null || !data.isProfessor()) {
            teacher.displayClientMessage(Component.translatable("block.hogwartsmod.pulpito_professor.only_professor_class"), true);
            return;
        }
        long now = teacher.level().getGameTime();
        TeachRules.Check check = TeachRules.check(data, spell, TeachRules.Limits.fromConfig(), now);
        if (check != TeachRules.Check.OK) {
            teacher.displayClientMessage(Component.translatable(check.key(), spell.getDisplayName()), true);
            return;
        }
        int invited = 0;
        for (ServerPlayer student : inArea(teacher, pulpit)) {
            if (status(student, spell, teacher.getUUID()) == StudentStatus.CAN_LEARN) {
                sendInvite(teacher, student, spell, true, true, now);
                invited++;
            }
        }
        if (invited == 0) {
            teacher.displayClientMessage(Component.translatable("teach.hogwartsmod.class_empty"), true);
            return;
        }
        data.setTeachCooldown(spell, now + TeachRules.cooldownTicks(data));
        teacher.displayClientMessage(Component.translatable("teach.hogwartsmod.class_started", spell.getDisplayName(), invited), false);
        sendInArea(teacher, pulpit, spell);
    }

    @Nullable
    private static Invite activeInvite(ServerPlayer student) {
        Invite invite = INVITES.get(student.getUUID());
        if (invite != null && invite.expiresAt() < student.level().getGameTime()) {
            INVITES.remove(student.getUUID());
            return null;
        }
        return invite;
    }

    public static boolean accept(ServerPlayer student) {
        Invite invite = activeInvite(student);
        if (invite == null) {
            student.displayClientMessage(Component.translatable("teach.hogwartsmod.no_invite"), false);
            return false;
        }
        INVITES.remove(student.getUUID());
        Spell spell = Spells.get(invite.spell());
        ServerPlayer teacher = student.server.getPlayerList().getPlayer(invite.teacher());
        MagicData data = MagicData.get(student);
        if (spell == null || teacher == null || data == null) {
            student.displayClientMessage(Component.translatable("teach.hogwartsmod.teacher_gone"), false);
            return false;
        }
        if (data.knows(spell) || data.getLevel() < spell.getRequiredLevel()) {
            student.displayClientMessage(Component.translatable("teach.hogwartsmod.not_eligible", spell.getDisplayName()), false);
            return false;
        }
        SESSIONS.put(student.getUUID(), new Session(invite.teacher(), invite.spell(), invite.professor(), invite.group()));
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> student),
                new OpenTracePacket(spell.getId(), teacher.getGameProfile().getName(), MagicConfig.TRACE_ATTEMPTS.get()));
        teacher.displayClientMessage(Component.translatable("teach.hogwartsmod.accepted", student.getDisplayName(), spell.getDisplayName()), true);
        return true;
    }

    public static boolean decline(ServerPlayer student) {
        Invite invite = activeInvite(student);
        if (invite == null) {
            student.displayClientMessage(Component.translatable("teach.hogwartsmod.no_invite"), false);
            return false;
        }
        INVITES.remove(student.getUUID());
        ServerPlayer teacher = student.server.getPlayerList().getPlayer(invite.teacher());
        if (teacher != null) {
            teacher.displayClientMessage(Component.translatable("teach.hogwartsmod.declined", student.getDisplayName()), true);
        }
        student.displayClientMessage(Component.translatable("teach.hogwartsmod.you_declined"), false);
        return true;
    }

    // -- Resultado do traçado

    public static void finish(ServerPlayer student, String spellId, TraceResult result) {
        Session session = SESSIONS.get(student.getUUID());
        if (session == null || !session.spell().equals(spellId)) {
            return;
        }
        SESSIONS.remove(student.getUUID());
        Spell spell = Spells.get(spellId);
        MagicData data = MagicData.get(student);
        if (spell == null || data == null) {
            return;
        }
        ServerPlayer teacher = student.server.getPlayerList().getPlayer(session.teacher());
        if (result != TraceResult.SUCCESS) {
            String key = result == TraceResult.GAVE_UP ? "teach.hogwartsmod.gave_up" : "teach.hogwartsmod.failed";
            if (teacher != null) {
                teacher.displayClientMessage(Component.translatable(key, student.getDisplayName(), spell.getDisplayName()), false);
            }
            return;
        }
        if (data.knows(spell)) {
            return;
        }
        data.learn(spell);
        data.addXp(MagicConfig.LEARN_XP_BASE.get() + MagicConfig.LEARN_XP_PER_LEVEL.get() * spell.getRequiredLevel());
        playLearned(student, spell);
        student.displayClientMessage(Component.translatable("teach.hogwartsmod.learned", spell.getDisplayName()), true);
        if (teacher != null) {
            MagicData teacherData = MagicData.get(teacher);
            if (teacherData != null) {
                if (!session.group()) {
                    teacherData.setTeachCooldown(spell, teacher.level().getGameTime() + TeachRules.cooldownTicks(teacherData));
                }
                if (session.professor()) {
                    teacherData.addXp(MagicConfig.TEACHER_XP_BASE.get() + MagicConfig.TEACHER_XP_PER_LEVEL.get() * spell.getRequiredLevel());
                }
            }
            teacher.displayClientMessage(Component.translatable("teach.hogwartsmod.student_learned", student.getDisplayName(), spell.getDisplayName()), false);
        }
    }

    public static void playLearned(ServerPlayer player, Spell spell) {
        if (!(player.level() instanceof ServerLevel server)) {
            return;
        }
        Vec3 feet = player.position().add(0, 0.1, 0);
        Vec3 center = player.position().add(0, 1.0, 0);
        SpellFx.particle(server, ModParticles.RING.get(), 0xFFD27A, 2.0f, 22, feet, Vec3.ZERO);
        SpellFx.particle(server, ModParticles.RING.get(), spell.getColor(), 1.4f, 18, feet.add(0, 0.4, 0), Vec3.ZERO);
        SpellFx.burst(server, ModParticles.STAR.get(), spell.getColor(), 0.6f, 14, center, 14, 0.32);
        SpellFx.burst(server, ModParticles.STAR.get(), 0xFFE9A8, 0.45f, 12, center, 10, 0.26);
        SpellFx.burst(server, ModParticles.EMBER.get(), 0xFFD27A, 0.1f, 40, center, 36, 0.1);
        for (int i = 0; i < 24; i++) {
            double angle = i / 24.0 * Math.PI * 2;
            Vec3 pos = feet.add(Math.cos(angle) * 0.9, 0, Math.sin(angle) * 0.9);
            SpellFx.particle(server, ModParticles.SPARK.get(), spell.getColor(), 0.16f, 26, pos, new Vec3(0, 0.12, 0));
        }
        server.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.2f);
        server.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    public static void onLogout(ServerPlayer player) {
        UUID id = player.getUUID();
        INVITES.remove(id);
        Session session = SESSIONS.remove(id);
        if (session != null) {
            ServerPlayer teacher = player.server.getPlayerList().getPlayer(session.teacher());
            Spell spell = Spells.get(session.spell());
            if (teacher != null && spell != null) {
                teacher.displayClientMessage(Component.translatable("teach.hogwartsmod.gave_up", player.getDisplayName(), spell.getDisplayName()), false);
            }
        }
        INVITES.values().removeIf(invite -> invite.teacher().equals(id));
    }
}
