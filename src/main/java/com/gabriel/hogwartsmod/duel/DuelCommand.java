package com.gabriel.hogwartsmod.duel;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.money.Coin;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = HogwartsMod.MOD_ID)
public final class DuelCommand {

    private DuelCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("duelo")
                .then(Commands.literal("aceitar").then(Commands.argument("jogador", EntityArgument.player())
                        .executes(ctx -> DuelManager.answer(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "jogador"), true))))
                .then(Commands.literal("recusar").then(Commands.argument("jogador", EntityArgument.player())
                        .executes(ctx -> DuelManager.answer(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "jogador"), false))))
                .then(Commands.argument("jogador", EntityArgument.player())
                        .executes(ctx -> DuelManager.invite(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "jogador"), 0))
                        .then(Commands.argument("valor", IntegerArgumentType.integer(1, 10000))
                                .then(Commands.argument("moeda", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("galeoes", "sicles", "nuques"), b))
                                        .executes(DuelCommand::inviteWithBet)))));
    }

    private static int inviteWithBet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        int amount = IntegerArgumentType.getInteger(ctx, "valor");
        String coin = StringArgumentType.getString(ctx, "moeda").toLowerCase();
        long unit;
        if (coin.startsWith("gale")) {
            unit = Coin.GALEAO.getValue();
        } else if (coin.startsWith("sic")) {
            unit = Coin.SICLE.getValue();
        } else if (coin.startsWith("nuq")) {
            unit = Coin.NUQUE.getValue();
        } else {
            ctx.getSource().sendFailure(Component.translatable("message.hogwartsmod.duel.bad_coin"));
            return 0;
        }
        return DuelManager.invite(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "jogador"), amount * unit);
    }
}
