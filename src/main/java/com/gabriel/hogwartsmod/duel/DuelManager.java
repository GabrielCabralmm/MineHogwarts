package com.gabriel.hogwartsmod.duel;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.magic.MagicConfig;
import com.gabriel.hogwartsmod.magic.MagicData;
import com.gabriel.hogwartsmod.money.Coin;
import com.gabriel.hogwartsmod.money.MoneyHelper;
import com.gabriel.hogwartsmod.money.Wallet;
import com.gabriel.hogwartsmod.network.ModNetwork;
import com.gabriel.hogwartsmod.spell.Spell;
import com.gabriel.hogwartsmod.spell.Spells;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = HogwartsMod.MOD_ID)
public final class DuelManager {
    private static final int INVITE_TICKS = 1200;
    private static final int COUNTDOWN_TICKS = 60;
    private static final int DURATION_TICKS = 3 * 60 * 20;
    private static final double MAX_DISTANCE = 20;
    private static final long COOLDOWN_MILLIS = 4L * 60 * 60 * 1000;
    private static final DustParticleOptions RING = new DustParticleOptions(new Vector3f(1.0f, 0.8f, 0.2f), 2.2f);

    private record Invite(UUID from, String fromName, UUID to, long bet, long expires) {
    }

    private static final Map<UUID, Invite> INVITES = new HashMap<>();
    private static final Map<UUID, Duel> DUELS = new HashMap<>();

    private DuelManager() {
    }

    @Nullable
    public static Duel of(@Nullable Entity entity) {
        return entity == null ? null : DUELS.get(entity.getUUID());
    }

    private static MutableComponent msg(String key, Object... args) {
        return Component.translatable("message.hogwartsmod.duel." + key, args);
    }

    // -- Convite

    public static int invite(ServerPlayer from, ServerPlayer to, long bet) {
        MinecraftServer server = from.server;
        long now = server.overworld().getGameTime();
        String problem = checkPair(from, to, bet);
        if (problem != null) {
            from.sendSystemMessage(msg(problem, to.getDisplayName()).withStyle(ChatFormatting.RED));
            return 0;
        }
        INVITES.put(to.getUUID(), new Invite(from.getUUID(), from.getGameProfile().getName(), to.getUUID(), bet, now + INVITE_TICKS));
        String name = from.getGameProfile().getName();
        MutableComponent accept = Component.translatable("message.hogwartsmod.duel.accept").withStyle(s -> s.withColor(ChatFormatting.GREEN).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duelo aceitar " + name))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("message.hogwartsmod.duel.accept_hover"))));
        MutableComponent refuse = Component.translatable("message.hogwartsmod.duel.refuse").withStyle(s -> s.withColor(ChatFormatting.RED).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duelo recusar " + name))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("message.hogwartsmod.duel.refuse_hover"))));
        to.sendSystemMessage(msg("invited", from.getDisplayName()).withStyle(ChatFormatting.GOLD));
        if (bet > 0) {
            to.sendSystemMessage(msg("bet", money(bet)).withStyle(ChatFormatting.YELLOW));
        }
        to.sendSystemMessage(Component.literal("  ").append(accept).append(Component.literal("   ")).append(refuse));
        to.playNotifySound(SoundEvents.NOTE_BLOCK_BELL.get(), SoundSource.PLAYERS, 0.8f, 1.2f);
        from.sendSystemMessage(msg("sent", to.getDisplayName()).withStyle(ChatFormatting.GRAY));
        return 1;
    }

    @Nullable
    private static String checkPair(ServerPlayer a, ServerPlayer b, long bet) {
        if (a == b) {
            return "self";
        }
        if (DUELS.containsKey(a.getUUID()) || DUELS.containsKey(b.getUUID())) {
            return "busy";
        }
        if (a.level() != b.level() || a.distanceTo(b) > MAX_DISTANCE) {
            return "far";
        }
        if (DuelRecords.get(a.server).remainingMillis(a.getUUID(), b.getUUID(), COOLDOWN_MILLIS) > 0) {
            return "cooldown";
        }
        if (bet > 0 && (Wallet.value(a) < bet || Wallet.value(b) < bet)) {
            return "no_money";
        }
        return null;
    }

    public static int answer(ServerPlayer to, ServerPlayer from, boolean accept) {
        Invite invite = INVITES.get(to.getUUID());
        long now = to.server.overworld().getGameTime();
        if (invite == null || !invite.from().equals(from.getUUID()) || invite.expires() < now) {
            to.sendSystemMessage(msg("no_invite").withStyle(ChatFormatting.GRAY));
            return 0;
        }
        INVITES.remove(to.getUUID());
        if (!accept) {
            to.sendSystemMessage(msg("refused_you").withStyle(ChatFormatting.GRAY));
            from.sendSystemMessage(msg("refused", to.getDisplayName()).withStyle(ChatFormatting.GRAY));
            return 1;
        }
        String problem = checkPair(from, to, invite.bet());
        if (problem != null) {
            to.sendSystemMessage(msg(problem, from.getDisplayName()).withStyle(ChatFormatting.RED));
            from.sendSystemMessage(msg(problem, to.getDisplayName()).withStyle(ChatFormatting.RED));
            return 0;
        }
        start(from, to, invite.bet());
        return 1;
    }

    // -- Início

    public static Duel start(ServerPlayer a, ServerPlayer b, long bet) {
        if (bet > 0) {
            payCoins(a, bet);
            payCoins(b, bet);
        }
        long now = a.server.overworld().getGameTime();
        Vec3 center = a.position().add(b.position()).scale(0.5);
        double radius = Math.max(8, a.distanceTo(b) / 2 + 4);
        ServerBossEvent bar = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
        bar.addPlayer(a);
        bar.addPlayer(b);
        Duel duel = new Duel(a.getUUID(), b.getUUID(), a.getGameProfile().getName(), b.getGameProfile().getName(), a.level().dimension(), center, radius,
                now + COUNTDOWN_TICKS, now + COUNTDOWN_TICKS + DURATION_TICKS, bet, bar);
        DUELS.put(a.getUUID(), duel);
        DUELS.put(b.getUUID(), duel);
        for (ServerPlayer p : List.of(a, b)) {
            p.setHealth(p.getMaxHealth());
            ServerPlayer other = p == a ? b : a;
            p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, other.getEyePosition());
            ModNetwork.playAnimation(p, "saudacao_duelo");
        }
        updateBar(duel, now);
        return duel;
    }

    private static void payCoins(ServerPlayer player, long value) {
        int g = (int) (value / Coin.GALEAO.getValue());
        long rest = value - (long) g * Coin.GALEAO.getValue();
        int s = (int) (rest / Coin.SICLE.getValue());
        int n = (int) (rest - (long) s * Coin.SICLE.getValue());
        Wallet.pay(player, g, s, n);
    }

    private static void giveCoins(ServerPlayer player, long value) {
        int g = (int) (value / Coin.GALEAO.getValue());
        long rest = value - (long) g * Coin.GALEAO.getValue();
        int s = (int) (rest / Coin.SICLE.getValue());
        MoneyHelper.give(player, g, s, (int) (rest - (long) s * Coin.SICLE.getValue()));
    }

    public static Component money(long value) {
        int g = (int) (value / Coin.GALEAO.getValue());
        long rest = value - (long) g * Coin.GALEAO.getValue();
        int s = (int) (rest / Coin.SICLE.getValue());
        int n = (int) (rest - (long) s * Coin.SICLE.getValue());
        List<String> parts = new ArrayList<>();
        if (g > 0) parts.add(Component.translatable("message.hogwartsmod.duel.coin_g", g).getString());
        if (s > 0) parts.add(Component.translatable("message.hogwartsmod.duel.coin_s", s).getString());
        if (n > 0) parts.add(Component.translatable("message.hogwartsmod.duel.coin_n", n).getString());
        return Component.literal(String.join(", ", parts));
    }

    // -- Durante

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || DUELS.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        long now = server.overworld().getGameTime();
        for (Duel duel : new ArrayList<>(new java.util.LinkedHashSet<>(DUELS.values()))) {
            ServerPlayer a = server.getPlayerList().getPlayer(duel.a);
            ServerPlayer b = server.getPlayerList().getPlayer(duel.b);
            if (a == null || b == null) {
                finish(server, duel, a != null ? duel.a : b != null ? duel.b : null, "disconnect");
                continue;
            }
            tickDuel(server, duel, a, b, now);
        }
    }

    private static void tickDuel(MinecraftServer server, Duel duel, ServerPlayer a, ServerPlayer b, long now) {
        ServerLevel level = server.getLevel(duel.dimension);
        if (level == null) {
            return;
        }
        if (now % 5 == 0) {
            int points = (int) (duel.radius * 9);
            for (int i = 0; i < points; i++) {
                double ang = Math.PI * 2 * i / points + (now / 5 % 2) * Math.PI / points;
                double x = duel.center.x + Math.cos(ang) * duel.radius;
                double z = duel.center.z + Math.sin(ang) * duel.radius;
                level.sendParticles(RING, x, duel.center.y + 0.15, z, 1, 0, 0, 0, 0);
                if (i % 6 == (now / 5) % 6) {
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, x, duel.center.y + 0.3, z, 1, 0, 0.05, 0, 0.01);
                }
            }
        }
        if (now < duel.startTick) {
            int count = (int) Math.ceil((duel.startTick - now) / 20.0);
            if (count != duel.lastCount) {
                duel.lastCount = count;
                for (ServerPlayer p : List.of(a, b)) {
                    title(p, Component.literal(String.valueOf(count)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), msg("salute"));
                    p.playNotifySound(SoundEvents.NOTE_BLOCK_HAT.get(), SoundSource.PLAYERS, 1f, 1.2f);
                }
            }
            return;
        }
        if (duel.lastCount != 0) {
            duel.lastCount = 0;
            for (ServerPlayer p : List.of(a, b)) {
                title(p, msg("go").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), Component.empty());
                p.playNotifySound(SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1f, 1f);
            }
        }
        if (now % 10 == 0) {
            updateBar(duel, now);
        }
        for (ServerPlayer p : List.of(a, b)) {
            if (p.level() != level || horizontal(p.position(), duel.center) > duel.radius + 0.3) {
                finish(server, duel, duel.other(p.getUUID()), "left");
                return;
            }
        }
        for (ServerPlayer other : level.players()) {
            if (duel.has(other.getUUID()) || other.isSpectator()) {
                continue;
            }
            double d = horizontal(other.position(), duel.center);
            if (d < duel.radius) {
                Vec3 dir = new Vec3(other.getX() - duel.center.x, 0, other.getZ() - duel.center.z);
                dir = dir.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : dir.normalize();
                Vec3 out = duel.center.add(dir.scale(duel.radius + 1.2));
                other.teleportTo(out.x, other.getY(), out.z);
                other.displayClientMessage(msg("keep_out").withStyle(ChatFormatting.GRAY), true);
            }
        }
        if (now >= duel.endTick) {
            float ha = a.getHealth() / a.getMaxHealth();
            float hb = b.getHealth() / b.getMaxHealth();
            finish(server, duel, Math.abs(ha - hb) < 0.05f ? null : ha > hb ? duel.a : duel.b, "time");
        }
    }

    private static double horizontal(Vec3 p, Vec3 c) {
        double dx = p.x - c.x;
        double dz = p.z - c.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static void title(ServerPlayer p, Component title, Component subtitle) {
        p.connection.send(new ClientboundSetTitlesAnimationPacket(0, 22, 4));
        p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        p.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    private static void updateBar(Duel duel, long now) {
        long left = Math.max(0, duel.endTick - Math.max(now, duel.startTick));
        int secs = (int) (left / 20);
        duel.bar.setName(msg("bar", duel.nameA, duel.nameB, String.format("%d:%02d", secs / 60, secs % 60)));
        duel.bar.setProgress(Math.max(0f, Math.min(1f, left / (float) DURATION_TICKS)));
    }

    // -- Feitiços

    public static boolean blocksSpell(ServerPlayer player, Spell spell) {
        Duel duel = DUELS.get(player.getUUID());
        if (duel == null) {
            return false;
        }
        long now = player.server.overworld().getGameTime();
        if (!duel.fighting(now)) {
            player.displayClientMessage(msg("wait").withStyle(ChatFormatting.GRAY), true);
            return true;
        }
        if (MagicConfig.DUEL_BLOCKED_SPELLS.get().contains(spell.getId())) {
            player.displayClientMessage(msg("blocked_spell", spell.getDisplayName()).withStyle(ChatFormatting.RED), true);
            return true;
        }
        duel.spells.get(player.getUUID()).add(spell.getId());
        return false;
    }

    // -- Dano

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttack(LivingAttackEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) {
            return;
        }
        Entity attacker = event.getSource().getEntity();
        Duel targetDuel = of(target);
        Duel attackerDuel = of(attacker);
        long now = target.getServer() == null ? 0 : target.getServer().overworld().getGameTime();
        if (targetDuel != null && attacker != null && attacker != target
                && (!targetDuel.has(attacker.getUUID()) || !targetDuel.fighting(now))) {
            event.setCanceled(true);
            return;
        }
        if (attackerDuel != null && attacker != target && !attackerDuel.has(target.getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Duel duel = DUELS.get(player.getUUID());
        if (duel == null || duel.ended || event.getAmount() < player.getHealth()) {
            return;
        }
        event.setCanceled(true);
        player.setHealth(1f);
        finish(player.server, duel, duel.other(player.getUUID()), "defeated");
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Duel duel = DUELS.get(player.getUUID());
        if (duel == null) {
            return;
        }
        event.setCanceled(true);
        player.setHealth(1f);
        finish(player.server, duel, duel.other(player.getUUID()), "defeated");
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Duel duel = DUELS.get(player.getUUID());
            if (duel != null) {
                finish(player.server, duel, duel.other(player.getUUID()), "disconnect");
            }
            INVITES.remove(player.getUUID());
        }
    }

    // -- Final

    public static void finish(MinecraftServer server, Duel duel, @Nullable UUID winner, String reason) {
        if (duel.ended) {
            return;
        }
        duel.ended = true;
        DUELS.remove(duel.a);
        DUELS.remove(duel.b);
        duel.bar.removeAllPlayers();
        DuelRecords.get(server).record(duel.a, duel.b);
        ServerPlayer pa = server.getPlayerList().getPlayer(duel.a);
        ServerPlayer pb = server.getPlayerList().getPlayer(duel.b);
        for (ServerPlayer p : new ServerPlayer[]{pa, pb}) {
            if (p == null) {
                continue;
            }
            p.setHealth(p.getMaxHealth());
            p.clearFire();
            p.removeAllEffects();
            ModNetwork.playAnimation(p, "");
            MagicData data = MagicData.get(p);
            if (data != null) {
                boolean won = p.getUUID().equals(winner);
                data.addXp(won ? MagicConfig.DUEL_XP_WIN.get() : MagicConfig.DUEL_XP_LOSE.get());
                for (String id : duel.spells.get(p.getUUID())) {
                    Spell spell = Spells.get(id);
                    if (spell != null) {
                        data.addMastery(spell, 2, 0);
                    }
                }
                data.markForSync();
            }
        }
        if (duel.betEach > 0) {
            if (winner == null) {
                if (pa != null) giveCoins(pa, duel.betEach);
                if (pb != null) giveCoins(pb, duel.betEach);
            } else {
                ServerPlayer w = server.getPlayerList().getPlayer(winner);
                if (w != null) {
                    giveCoins(w, duel.betEach * 2);
                }
            }
        }
        Component result = winner == null
                ? msg("draw", duel.nameA, duel.nameB).withStyle(ChatFormatting.YELLOW)
                : msg("won_" + reason, duel.name(winner), duel.name(duel.other(winner))).withStyle(ChatFormatting.GOLD);
        ServerLevel level = server.getLevel(duel.dimension);
        if (level != null) {
            for (ServerPlayer p : level.players()) {
                if (duel.has(p.getUUID()) || horizontal(p.position(), duel.center) < 48) {
                    p.sendSystemMessage(result);
                }
            }
            level.playSound(null, duel.center.x, duel.center.y, duel.center.z, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 0.8f);
        }
        if (duel.betEach > 0 && winner != null) {
            ServerPlayer w = server.getPlayerList().getPlayer(winner);
            if (w != null) {
                w.sendSystemMessage(msg("bet_won", money(duel.betEach * 2)).withStyle(ChatFormatting.GOLD));
            }
        }
        for (ServerPlayer p : new ServerPlayer[]{pa, pb}) {
            if (p != null) {
                title(p, winner == null ? msg("title_draw").withStyle(ChatFormatting.YELLOW)
                        : p.getUUID().equals(winner) ? msg("title_win").withStyle(ChatFormatting.GOLD) : msg("title_lose").withStyle(ChatFormatting.GRAY),
                        Component.empty());
            }
        }
    }
}
