package com.gabriel.hogwartsmod.command;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.magic.MagicData;
import com.gabriel.hogwartsmod.spell.Spell;
import com.gabriel.hogwartsmod.spell.Spells;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Mod.EventBusSubscriber(modid = HogwartsMod.MOD_ID)
public final class HogwartsCommand {
    private static final String ALL = "todos";
    private static final SimpleCommandExceptionType UNKNOWN_SPELL =
            new SimpleCommandExceptionType(Component.translatable("command.hogwartsmod.unknown_spell"));
    private static final SuggestionProvider<CommandSourceStack> SPELLS_OR_ALL = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Stream.concat(Stream.of(ALL), Spells.all().stream().map(Spell::getId)), builder);
    private static final SuggestionProvider<CommandSourceStack> SPELLS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Spells.all().stream().map(Spell::getId), builder);

    private HogwartsCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("hogwarts")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("feitico")
                        .then(Commands.literal("aprender")
                                .then(Commands.argument("alvo", EntityArgument.player())
                                        .then(Commands.argument("feitico", StringArgumentType.word()).suggests(SPELLS_OR_ALL)
                                                .executes(ctx -> learn(ctx, true)))))
                        .then(Commands.literal("esquecer")
                                .then(Commands.argument("alvo", EntityArgument.player())
                                        .then(Commands.argument("feitico", StringArgumentType.word()).suggests(SPELLS_OR_ALL)
                                                .executes(ctx -> learn(ctx, false))))))
                .then(Commands.literal("atalho")
                        .then(Commands.argument("slot", IntegerArgumentType.integer(1, MagicData.SLOT_COUNT))
                                .then(Commands.argument("feitico", StringArgumentType.word()).suggests(SPELLS)
                                        .executes(HogwartsCommand::setSlot))))
                .then(Commands.literal("nivel")
                        .then(Commands.argument("alvo", EntityArgument.player())
                                .then(Commands.argument("nivel", IntegerArgumentType.integer(1))
                                        .executes(HogwartsCommand::setLevel))))
                .then(Commands.literal("xp")
                        .then(Commands.argument("alvo", EntityArgument.player())
                                .then(Commands.argument("quantidade", IntegerArgumentType.integer(1))
                                        .executes(HogwartsCommand::addXp))))
                .then(Commands.literal("reserva")
                        .then(Commands.argument("alvo", EntityArgument.player())
                                .executes(HogwartsCommand::fillReserve)))
                .then(Commands.literal("info")
                        .then(Commands.argument("alvo", EntityArgument.player())
                                .executes(HogwartsCommand::info))));
    }

    private static MagicData data(ServerPlayer player) {
        MagicData data = MagicData.get(player);
        if (data == null) {
            throw new IllegalStateException("Dados mágicos ausentes");
        }
        return data;
    }

    private static List<Spell> resolve(String id) throws CommandSyntaxException {
        if (ALL.equals(id)) {
            return new ArrayList<>(Spells.all());
        }
        Spell spell = Spells.get(id);
        if (spell == null) {
            throw UNKNOWN_SPELL.create();
        }
        return List.of(spell);
    }

    private static int learn(CommandContext<CommandSourceStack> ctx, boolean learn) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "alvo");
        List<Spell> spells = resolve(StringArgumentType.getString(ctx, "feitico"));
        MagicData data = data(target);
        for (Spell spell : spells) {
            if (learn) {
                data.learn(spell);
            } else {
                data.forget(spell);
            }
        }
        String key = learn ? "command.hogwartsmod.learned" : "command.hogwartsmod.forgot";
        ctx.getSource().sendSuccess(() -> Component.translatable(key, spells.size(), target.getDisplayName()), true);
        return spells.size();
    }

    private static int setSlot(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int slot = IntegerArgumentType.getInteger(ctx, "slot");
        Spell spell = resolve(StringArgumentType.getString(ctx, "feitico")).get(0);
        MagicData data = data(player);
        if (!data.knows(spell)) {
            ctx.getSource().sendFailure(Component.translatable("message.hogwartsmod.not_learned", spell.getDisplayName()));
            return 0;
        }
        data.setSlot(slot - 1, spell);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.hogwartsmod.slot_set", slot, spell.getDisplayName()), false);
        return 1;
    }

    private static int setLevel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "alvo");
        int level = IntegerArgumentType.getInteger(ctx, "nivel");
        MagicData data = data(target);
        data.setLevel(level);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.hogwartsmod.level_set", target.getDisplayName(), data.getLevel()), true);
        return 1;
    }

    private static int addXp(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "alvo");
        int amount = IntegerArgumentType.getInteger(ctx, "quantidade");
        MagicData data = data(target);
        data.addXp(amount);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.hogwartsmod.xp_added", amount, target.getDisplayName(), data.getLevel()), true);
        return 1;
    }

    private static int fillReserve(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "alvo");
        MagicData data = data(target);
        data.setReserve(data.getMaxReserve());
        data.markForSync();
        ctx.getSource().sendSuccess(() -> Component.translatable("command.hogwartsmod.reserve_filled", target.getDisplayName()), true);
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "alvo");
        MagicData data = data(target);
        CommandSourceStack source = ctx.getSource();
        source.sendSuccess(() -> Component.translatable("command.hogwartsmod.info_header", target.getDisplayName(),
                data.getLevel(), data.getXp(), MagicData.xpForNextLevel(data.getLevel()),
                Math.round(data.getReserve()), Math.round(data.getMaxReserve())), false);
        for (String id : data.getLearned()) {
            Spell spell = Spells.get(id);
            if (spell != null) {
                source.sendSuccess(() -> Component.translatable("command.hogwartsmod.info_spell", spell.getDisplayName(),
                        data.getMastery(spell), spell.getMaxMastery(), data.getMasteryUses(spell)), false);
            }
        }
        return 1;
    }
}
