package com.gabriel.hogwartsmod.house.door;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = HogwartsMod.MOD_ID)
public final class HouseDoorCommand {
    private static final SimpleCommandExceptionType BAD_NUMBERS =
            new SimpleCommandExceptionType(Component.translatable("command.hogwartsmod.door.bad_numbers"));
    private static final SimpleCommandExceptionType BAD_RIDDLE =
            new SimpleCommandExceptionType(Component.translatable("command.hogwartsmod.door.bad_riddle"));

    private HouseDoorCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("hogwarts")
                .then(Commands.literal("porta").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("ver").executes(HouseDoorCommand::show))
                        .then(Commands.literal("senha")
                                .then(Commands.argument("senha", StringArgumentType.greedyString()).executes(HouseDoorCommand::setPassword)))
                        .then(Commands.literal("combinacao")
                                .then(Commands.argument("simbolos", StringArgumentType.greedyString()).executes(HouseDoorCommand::setCombination)))
                        .then(Commands.literal("sequencia")
                                .then(Commands.argument("barris", StringArgumentType.greedyString()).executes(HouseDoorCommand::setSequence)))
                        .then(Commands.literal("charada")
                                .then(Commands.literal("adicionar")
                                        .then(Commands.argument("charada", StringArgumentType.greedyString()).executes(HouseDoorCommand::addRiddle)))
                                .then(Commands.literal("remover")
                                        .then(Commands.argument("numero", IntegerArgumentType.integer(1)).executes(HouseDoorCommand::removeRiddle))))));
    }

    private static HouseDoors doors(CommandContext<CommandSourceStack> ctx) {
        return HouseDoors.get(ctx.getSource().getServer());
    }

    private static int[] numbers(String text, int count, int max) throws CommandSyntaxException {
        String[] parts = text.trim().split("[\\s,\\-]+");
        if (count > 0 && parts.length != count || parts.length == 0 || parts.length > 12) {
            throw BAD_NUMBERS.create();
        }
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                out[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                throw BAD_NUMBERS.create();
            }
            if (out[i] < 1 || out[i] > max) {
                throw BAD_NUMBERS.create();
            }
        }
        return out;
    }

    private static String join(int[] values) {
        return Arrays.stream(values).mapToObj(String::valueOf).collect(Collectors.joining(" "));
    }

    private static int setPassword(CommandContext<CommandSourceStack> ctx) {
        String value = StringArgumentType.getString(ctx, "senha");
        doors(ctx).setPassword(value);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.hogwartsmod.door.password", value), true);
        return 1;
    }

    private static int setCombination(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        int[] value = numbers(StringArgumentType.getString(ctx, "simbolos"), HouseDoors.RINGS, HouseDoors.SYMBOLS);
        doors(ctx).setCombination(value);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.hogwartsmod.door.combination", join(value)), true);
        return 1;
    }

    private static int setSequence(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        int[] value = numbers(StringArgumentType.getString(ctx, "barris"), 0, HouseDoors.BARRELS);
        doors(ctx).setSequence(value);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.hogwartsmod.door.sequence", join(value)), true);
        return 1;
    }

    private static int addRiddle(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String[] parts = StringArgumentType.getString(ctx, "charada").split("\\|", 2);
        if (parts.length < 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw BAD_RIDDLE.create();
        }
        doors(ctx).addRiddle(parts[0], parts[1]);
        int n = doors(ctx).riddles().size();
        ctx.getSource().sendSuccess(() -> Component.translatable("command.hogwartsmod.door.riddle_added", n), true);
        return 1;
    }

    private static int removeRiddle(CommandContext<CommandSourceStack> ctx) {
        int n = IntegerArgumentType.getInteger(ctx, "numero");
        boolean ok = doors(ctx).removeRiddle(n - 1);
        ctx.getSource().sendSuccess(() -> Component.translatable(ok ? "command.hogwartsmod.door.riddle_removed" : "command.hogwartsmod.door.riddle_missing", n), true);
        return ok ? 1 : 0;
    }

    private static int show(CommandContext<CommandSourceStack> ctx) {
        HouseDoors doors = doors(ctx);
        CommandSourceStack source = ctx.getSource();
        source.sendSuccess(() -> Component.translatable("command.hogwartsmod.door.show_password", doors.password()).withStyle(ChatFormatting.RED), false);
        source.sendSuccess(() -> Component.translatable("command.hogwartsmod.door.show_combination", join(doors.combination())).withStyle(ChatFormatting.GREEN), false);
        source.sendSuccess(() -> Component.translatable("command.hogwartsmod.door.show_sequence", join(doors.sequence())).withStyle(ChatFormatting.GOLD), false);
        List<HouseDoors.Riddle> riddles = doors.riddles();
        source.sendSuccess(() -> Component.translatable("command.hogwartsmod.door.show_riddles", riddles.size()).withStyle(ChatFormatting.BLUE), false);
        for (int i = 0; i < riddles.size(); i++) {
            HouseDoors.Riddle riddle = riddles.get(i);
            int n = i + 1;
            source.sendSuccess(() -> Component.literal(n + ". " + riddle.question() + " → " + riddle.answer()).withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }
}
