package com.gabriel.hogwartsmod.command;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.apparition.ApparitionPoint;
import com.gabriel.hogwartsmod.apparition.PublicLocations;
import com.gabriel.hogwartsmod.spell.impl.AparatacaoSpell;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.gabriel.hogwartsmod.entity.ProfessorNpcEntity;
import com.gabriel.hogwartsmod.magic.MagicData;
import com.gabriel.hogwartsmod.registry.ModEntities;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import java.util.Comparator;
import java.util.stream.Collectors;
import com.gabriel.hogwartsmod.magic.Profession;
import com.gabriel.hogwartsmod.teach.TeachingManager;
import com.gabriel.hogwartsmod.spell.Spell;
import com.gabriel.hogwartsmod.spell.Spells;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
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
import java.util.function.Predicate;
import java.util.stream.Stream;

@Mod.EventBusSubscriber(modid = HogwartsMod.MOD_ID)
public final class HogwartsCommand {
    private static final String ALL = "todos";
    private static final Predicate<CommandSourceStack> STAFF = source -> source.hasPermission(2);
    private static final SuggestionProvider<CommandSourceStack> PROFESSIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Stream.of(Profession.values()).map(Profession::getId), builder);
    private static final SuggestionProvider<CommandSourceStack> WOODS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Stream.of(com.gabriel.hogwartsmod.wand.WandWood.values()).map(com.gabriel.hogwartsmod.wand.WandWood::getId), builder);
    private static final SuggestionProvider<CommandSourceStack> CORES = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Stream.of(com.gabriel.hogwartsmod.wand.WandCore.values()).map(com.gabriel.hogwartsmod.wand.WandCore::getId), builder);
    private static final SuggestionProvider<CommandSourceStack> HANDLES = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Stream.of(com.gabriel.hogwartsmod.wand.WandHandle.values()).map(com.gabriel.hogwartsmod.wand.WandHandle::getId), builder);
    private static final SimpleCommandExceptionType BAD_WAND =
            new SimpleCommandExceptionType(Component.translatable("command.hogwartsmod.wand_invalid"));
    private static final DynamicCommandExceptionType NO_DATA =
            new DynamicCommandExceptionType(name -> Component.translatable("command.hogwartsmod.no_data", name));
    private static final SuggestionProvider<CommandSourceStack> PUBLIC_PLACES = (ctx, builder) ->
            SharedSuggestionProvider.suggest(PublicLocations.get(ctx.getSource().getServer()).all().stream().map(ApparitionPoint::name), builder);
    private static final SuggestionProvider<CommandSourceStack> ZONES = (ctx, builder) ->
            SharedSuggestionProvider.suggest(com.gabriel.hogwartsmod.zone.ZoneData.get(ctx.getSource().getServer()).all().stream()
                    .map(com.gabriel.hogwartsmod.zone.Zone::name), builder);
    private static final SuggestionProvider<CommandSourceStack> ZONE_RULES = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Stream.of(com.gabriel.hogwartsmod.zone.ZoneRule.values()).map(com.gabriel.hogwartsmod.zone.ZoneRule::getId), builder);
    private static final SuggestionProvider<CommandSourceStack> HOUSES = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Stream.concat(Stream.of("nenhuma"),
                    Stream.of(com.gabriel.hogwartsmod.house.House.values()).map(com.gabriel.hogwartsmod.house.House::getId)), builder);
    private static final SuggestionProvider<CommandSourceStack> ONLY_HOUSES = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Stream.of(com.gabriel.hogwartsmod.house.House.values()).map(com.gabriel.hogwartsmod.house.House::getId), builder);
    private static final SuggestionProvider<CommandSourceStack> HOUSES_OR_PLAYERS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Stream.concat(Stream.of(com.gabriel.hogwartsmod.house.House.values()).map(com.gabriel.hogwartsmod.house.House::getId),
                    ctx.getSource().getOnlinePlayerNames().stream()), builder);
    private static final DynamicCommandExceptionType NO_HOUSE =
            new DynamicCommandExceptionType(name -> Component.translatable("command.hogwartsmod.points_no_house", name));
    private static final SimpleCommandExceptionType UNKNOWN_HOUSE =
            new SimpleCommandExceptionType(Component.translatable("command.hogwartsmod.unknown_house"));
    private static final SimpleCommandExceptionType NO_NPC =
            new SimpleCommandExceptionType(Component.translatable("command.hogwartsmod.no_npc"));
    private static final SimpleCommandExceptionType UNKNOWN_PROFESSION =
            new SimpleCommandExceptionType(Component.translatable("command.hogwartsmod.unknown_profession"));
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
                .then(Commands.literal("aula")
                        .then(Commands.literal("aceitar")
                                .executes(ctx -> TeachingManager.accept(ctx.getSource().getPlayerOrException()) ? 1 : 0))
                        .then(Commands.literal("recusar")
                                .executes(ctx -> TeachingManager.decline(ctx.getSource().getPlayerOrException()) ? 1 : 0)))
                .then(Commands.literal("aparatacao")
                        .then(Commands.literal("licenca").requires(STAFF)
                                .then(Commands.argument("alvo", EntityArgument.player())
                                        .then(Commands.argument("ativa", BoolArgumentType.bool())
                                                .executes(HogwartsCommand::setLicense))))
                        .then(Commands.literal("publico").requires(HogwartsCommand::canManagePublic)
                                .then(Commands.literal("criar")
                                        .then(Commands.argument("nome", StringArgumentType.greedyString())
                                                .executes(HogwartsCommand::createPublic)))
                                .then(Commands.literal("remover")
                                        .then(Commands.argument("nome", StringArgumentType.greedyString()).suggests(PUBLIC_PLACES)
                                                .executes(HogwartsCommand::removePublic)))
                                .then(Commands.literal("lista")
                                        .executes(HogwartsCommand::listPublic))))
                .then(Commands.literal("zona").requires(STAFF)
                        .then(Commands.literal("criar")
                                .then(Commands.argument("nome", StringArgumentType.greedyString())
                                        .executes(ctx -> com.gabriel.hogwartsmod.zone.ZoneEditing.create(ctx.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(ctx, "nome").strip()) ? 1 : 0)))
                        .then(Commands.literal("remover")
                                .then(Commands.argument("nome", StringArgumentType.greedyString()).suggests(ZONES)
                                        .executes(HogwartsCommand::removeZone)))
                        .then(Commands.literal("recortar")
                                .then(Commands.argument("nome", StringArgumentType.greedyString()).suggests(ZONES)
                                        .executes(ctx -> com.gabriel.hogwartsmod.zone.ZoneEditing.cut(ctx.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(ctx, "nome").strip()) ? 1 : 0)))
                        .then(Commands.literal("expandir")
                                .then(Commands.argument("nome", StringArgumentType.greedyString()).suggests(ZONES)
                                        .executes(ctx -> com.gabriel.hogwartsmod.zone.ZoneEditing.expand(ctx.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(ctx, "nome").strip()) ? 1 : 0)))
                        .then(Commands.literal("regra")
                                .then(Commands.argument("regra", StringArgumentType.word()).suggests(ZONE_RULES)
                                        .then(Commands.argument("ativa", BoolArgumentType.bool())
                                                .then(Commands.argument("nome", StringArgumentType.greedyString()).suggests(ZONES)
                                                        .executes(HogwartsCommand::zoneRule)))))
                        .then(Commands.literal("lista")
                                .executes(HogwartsCommand::listZones)))
                .then(Commands.literal("npc").requires(STAFF)
                        .then(Commands.literal("missoes")
                                .executes(ctx -> createMissionNpc(ctx, null))
                                .then(Commands.argument("nome", StringArgumentType.greedyString())
                                        .executes(ctx -> createMissionNpc(ctx, StringArgumentType.getString(ctx, "nome")))))
                        .then(Commands.literal("loja")
                                .executes(ctx -> createShopNpc(ctx, null))
                                .then(Commands.argument("nome", StringArgumentType.greedyString())
                                        .executes(ctx -> createShopNpc(ctx, StringArgumentType.getString(ctx, "nome")))))
                        .then(Commands.literal("auxiliar_varinhas")
                                .executes(ctx -> createCreatureShop(ctx, com.gabriel.hogwartsmod.shop.ShopKind.AUXILIAR_VARINHAS, null))
                                .then(Commands.argument("nome", StringArgumentType.greedyString())
                                        .executes(ctx -> createCreatureShop(ctx, com.gabriel.hogwartsmod.shop.ShopKind.AUXILIAR_VARINHAS, StringArgumentType.getString(ctx, "nome")))))
                        .then(Commands.literal("comerciante")
                                .executes(ctx -> createCreatureShop(ctx, com.gabriel.hogwartsmod.shop.ShopKind.COMERCIANTE, null))
                                .then(Commands.argument("nome", StringArgumentType.greedyString())
                                        .executes(ctx -> createCreatureShop(ctx, com.gabriel.hogwartsmod.shop.ShopKind.COMERCIANTE, StringArgumentType.getString(ctx, "nome")))))
                        .then(Commands.literal("contrabandista")
                                .executes(ctx -> createCreatureShop(ctx, com.gabriel.hogwartsmod.shop.ShopKind.CONTRABANDISTA, null))
                                .then(Commands.argument("nome", StringArgumentType.greedyString())
                                        .executes(ctx -> createCreatureShop(ctx, com.gabriel.hogwartsmod.shop.ShopKind.CONTRABANDISTA, StringArgumentType.getString(ctx, "nome")))))
                        .then(Commands.literal("modelo")
                                .then(Commands.argument("modelo", StringArgumentType.word())
                                        .executes(HogwartsCommand::setNpcModel)))
                        .then(Commands.literal("criar")
                                .executes(ctx -> createNpc(ctx, null))
                                .then(Commands.argument("nome", StringArgumentType.greedyString())
                                        .executes(ctx -> createNpc(ctx, StringArgumentType.getString(ctx, "nome")))))
                        .then(Commands.literal("nome")
                                .then(Commands.argument("nome", StringArgumentType.greedyString())
                                        .executes(HogwartsCommand::renameNpc)))
                        .then(Commands.literal("feitico")
                                .then(Commands.literal("adicionar")
                                        .then(Commands.argument("feitico", StringArgumentType.word()).suggests(SPELLS_OR_ALL)
                                                .executes(ctx -> npcSpell(ctx, true))))
                                .then(Commands.literal("remover")
                                        .then(Commands.argument("feitico", StringArgumentType.word()).suggests(SPELLS_OR_ALL)
                                                .executes(ctx -> npcSpell(ctx, false)))))
                        .then(Commands.literal("remover")
                                .executes(HogwartsCommand::removeNpc)))
                .then(Commands.literal("pontos")
                        .then(Commands.literal("ver")
                                .executes(HogwartsCommand::showPoints))
                        .then(Commands.literal("dar").requires(HogwartsCommand::canGivePoints)
                                .then(Commands.argument("alvo", StringArgumentType.word()).suggests(HOUSES_OR_PLAYERS)
                                        .then(Commands.argument("quantidade", IntegerArgumentType.integer(1, 1000))
                                                .executes(ctx -> points(ctx, 1, null))
                                                .then(Commands.argument("motivo", StringArgumentType.greedyString())
                                                        .executes(ctx -> points(ctx, 1, StringArgumentType.getString(ctx, "motivo")))))))
                        .then(Commands.literal("tirar").requires(HogwartsCommand::canGivePoints)
                                .then(Commands.argument("alvo", StringArgumentType.word()).suggests(HOUSES_OR_PLAYERS)
                                        .then(Commands.argument("quantidade", IntegerArgumentType.integer(1, 1000))
                                                .executes(ctx -> points(ctx, -1, null))
                                                .then(Commands.argument("motivo", StringArgumentType.greedyString())
                                                        .executes(ctx -> points(ctx, -1, StringArgumentType.getString(ctx, "motivo")))))))
                        .then(Commands.literal("definir").requires(STAFF)
                                .then(Commands.argument("casa", StringArgumentType.word()).suggests(ONLY_HOUSES)
                                        .then(Commands.argument("quantidade", IntegerArgumentType.integer(0, 1000000))
                                                .executes(HogwartsCommand::setPoints))))
                        .then(Commands.literal("zerar").requires(STAFF)
                                .executes(HogwartsCommand::resetPoints))
                        .then(Commands.literal("encerrar").requires(STAFF)
                                .executes(HogwartsCommand::finishCup)))
                .then(Commands.literal("casa").requires(STAFF)
                        .then(Commands.argument("alvo", EntityArgument.player())
                                .then(Commands.argument("casa", StringArgumentType.word()).suggests(HOUSES)
                                        .executes(ctx -> setHouse(ctx, false))
                                        .then(Commands.literal("anunciar")
                                                .executes(ctx -> setHouse(ctx, true))))))
                .then(Commands.literal("varinha").requires(STAFF)
                        .then(Commands.argument("alvo", EntityArgument.player())
                                .then(Commands.argument("madeira", StringArgumentType.word()).suggests(WOODS)
                                        .then(Commands.argument("nucleo", StringArgumentType.word()).suggests(CORES)
                                                .then(Commands.argument("comprimento", com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg(9, 14))
                                                        .executes(HogwartsCommand::giveWand)
                                                        .then(Commands.argument("cabo", StringArgumentType.word()).suggests(HANDLES)
                                                                .executes(HogwartsCommand::giveWand)))))))
                .then(Commands.literal("profissao").requires(STAFF)
                        .then(Commands.argument("alvo", EntityArgument.player())
                                .then(Commands.argument("profissao", StringArgumentType.word()).suggests(PROFESSIONS)
                                        .executes(HogwartsCommand::setProfession))))
                .then(Commands.literal("feitico").requires(STAFF)
                        .then(Commands.literal("maestria")
                                .then(Commands.argument("alvo", EntityArgument.player())
                                        .then(Commands.argument("feitico", StringArgumentType.word()).suggests(SPELLS_OR_ALL)
                                                .then(Commands.argument("nivel", IntegerArgumentType.integer(0, 5))
                                                        .executes(HogwartsCommand::setMastery)))))
                        .then(Commands.literal("aprender")
                                .then(Commands.argument("alvo", EntityArgument.player())
                                        .then(Commands.argument("feitico", StringArgumentType.word()).suggests(SPELLS_OR_ALL)
                                                .executes(ctx -> learn(ctx, true)))))
                        .then(Commands.literal("esquecer")
                                .then(Commands.argument("alvo", EntityArgument.player())
                                        .then(Commands.argument("feitico", StringArgumentType.word()).suggests(SPELLS_OR_ALL)
                                                .executes(ctx -> learn(ctx, false))))))
                .then(Commands.literal("atalho").requires(STAFF)
                        .then(Commands.argument("slot", IntegerArgumentType.integer(1, MagicData.SLOT_COUNT))
                                .then(Commands.argument("feitico", StringArgumentType.word()).suggests(SPELLS)
                                        .executes(HogwartsCommand::setSlot))))
                .then(Commands.literal("nivel").requires(STAFF)
                        .then(Commands.argument("alvo", EntityArgument.player())
                                .then(Commands.argument("nivel", IntegerArgumentType.integer(1))
                                        .executes(HogwartsCommand::setLevel))))
                .then(Commands.literal("xp").requires(STAFF)
                        .then(Commands.argument("alvo", EntityArgument.player())
                                .then(Commands.argument("quantidade", IntegerArgumentType.integer(1))
                                        .executes(HogwartsCommand::addXp))))
                .then(Commands.literal("reserva").requires(STAFF)
                        .then(Commands.argument("alvo", EntityArgument.player())
                                .executes(HogwartsCommand::fillReserve)))
                .then(Commands.literal("vidas").requires(STAFF)
                        .then(Commands.argument("alvo", EntityArgument.player())
                                .executes(ctx -> lives(ctx, 0, 0))
                                .then(Commands.literal("adicionar").then(Commands.argument("quantidade", IntegerArgumentType.integer(1))
                                        .executes(ctx -> lives(ctx, 1, IntegerArgumentType.getInteger(ctx, "quantidade")))))
                                .then(Commands.literal("remover").then(Commands.argument("quantidade", IntegerArgumentType.integer(1))
                                        .executes(ctx -> lives(ctx, -1, IntegerArgumentType.getInteger(ctx, "quantidade")))))
                                .then(Commands.literal("definir").then(Commands.argument("quantidade", IntegerArgumentType.integer(0))
                                        .executes(ctx -> lives(ctx, 2, IntegerArgumentType.getInteger(ctx, "quantidade")))))))
                .then(Commands.literal("testralio").requires(STAFF)
                        .then(Commands.argument("alvo", EntityArgument.player())
                                .then(Commands.literal("ver").executes(ctx -> thestralSight(ctx, true)))
                                .then(Commands.literal("esconder").executes(ctx -> thestralSight(ctx, false)))))
                .then(Commands.literal("info").requires(STAFF)
                        .then(Commands.argument("alvo", EntityArgument.player())
                                .executes(HogwartsCommand::info))));
    }

    private static MagicData data(ServerPlayer player) throws CommandSyntaxException {
        MagicData data = MagicData.get(player);
        if (data == null) {
            throw NO_DATA.create(player.getDisplayName());
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

    private static boolean canManagePublic(CommandSourceStack source) {
        if (source.hasPermission(2)) {
            return true;
        }
        MagicData data = source.getPlayer() == null ? null : MagicData.get(source.getPlayer());
        return data != null && data.hasApparitionLicense();
    }

    private static int setLicense(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "alvo");
        boolean value = BoolArgumentType.getBool(ctx, "ativa");
        data(target).setApparitionLicense(value);
        ctx.getSource().sendSuccess(() -> Component.translatable(value ? "command.hogwartsmod.license_on" : "command.hogwartsmod.license_off",
                target.getDisplayName()), true);
        return 1;
    }

    private static int createPublic(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(ctx, "nome").strip();
        if (name.length() > 24) {
            name = name.substring(0, 24);
        }
        PublicLocations places = PublicLocations.get(player.server);
        places.put(new ApparitionPoint(name, AparatacaoSpell.dimensionOf(player), player.getX(), player.getY(), player.getZ(), player.getYRot()));
        places.sendToAll();
        String created = name;
        ctx.getSource().sendSuccess(() -> Component.translatable("command.hogwartsmod.public_created", created), true);
        return 1;
    }

    private static int removePublic(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "nome").strip();
        PublicLocations places = PublicLocations.get(ctx.getSource().getServer());
        if (!places.remove(name)) {
            ctx.getSource().sendFailure(Component.translatable("command.hogwartsmod.public_unknown", name));
            return 0;
        }
        places.sendToAll();
        ctx.getSource().sendSuccess(() -> Component.translatable("command.hogwartsmod.public_removed", name), true);
        return 1;
    }

    private static int listPublic(CommandContext<CommandSourceStack> ctx) {
        List<ApparitionPoint> all = PublicLocations.get(ctx.getSource().getServer()).all();
        String list = all.stream().map(ApparitionPoint::name).collect(Collectors.joining(", "));
        ctx.getSource().sendSuccess(() -> Component.translatable("command.hogwartsmod.public_list", list.isEmpty() ? "-" : list), false);
        return all.size();
    }

    private static int removeZone(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "nome").strip();
        if (!com.gabriel.hogwartsmod.zone.ZoneData.get(ctx.getSource().getServer()).remove(name)) {
            ctx.getSource().sendFailure(Component.translatable("command.hogwartsmod.zone_unknown", name));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("message.hogwartsmod.zone_removed", name), true);
        return 1;
    }

    private static int zoneRule(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "nome").strip();
        com.gabriel.hogwartsmod.zone.ZoneData data = com.gabriel.hogwartsmod.zone.ZoneData.get(ctx.getSource().getServer());
        com.gabriel.hogwartsmod.zone.Zone zone = data.find(name);
        com.gabriel.hogwartsmod.zone.ZoneRule rule = com.gabriel.hogwartsmod.zone.ZoneRule.byId(StringArgumentType.getString(ctx, "regra"));
        if (zone == null) {
            ctx.getSource().sendFailure(Component.translatable("command.hogwartsmod.zone_unknown", name));
            return 0;
        }
        if (rule == null) {
            ctx.getSource().sendFailure(Component.translatable("command.hogwartsmod.zone_unknown_rule"));
            return 0;
        }
        boolean on = BoolArgumentType.getBool(ctx, "ativa");
        data.put(zone.with(rule, on));
        ctx.getSource().sendSuccess(() -> Component.translatable(on ? "command.hogwartsmod.zone_rule_on" : "command.hogwartsmod.zone_rule_off",
                Component.translatable(rule.key()), zone.name()), true);
        return 1;
    }

    private static int listZones(CommandContext<CommandSourceStack> ctx) {
        List<com.gabriel.hogwartsmod.zone.Zone> all = com.gabriel.hogwartsmod.zone.ZoneData.get(ctx.getSource().getServer()).all();
        String list = all.stream().map(com.gabriel.hogwartsmod.zone.Zone::name).collect(Collectors.joining(", "));
        ctx.getSource().sendSuccess(() -> Component.translatable("command.hogwartsmod.zone_list", list.isEmpty() ? "-" : list), false);
        return all.size();
    }

    private static ProfessorNpcEntity nearestNpc(CommandSourceStack source) throws CommandSyntaxException {
        Vec3 pos = source.getPosition();
        return source.getLevel().getEntitiesOfClass(ProfessorNpcEntity.class, new AABB(pos, pos).inflate(8)).stream()
                .min(Comparator.comparingDouble(npc -> npc.distanceToSqr(pos)))
                .orElseThrow(NO_NPC::create);
    }

    private static int createNpc(CommandContext<CommandSourceStack> ctx, @Nullable String name) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ProfessorNpcEntity npc = ModEntities.PROFESSOR_NPC.get().create(source.getLevel());
        if (npc == null) {
            return 0;
        }
        Vec3 pos = source.getPosition();
        float yaw = source.getRotation().y + 180f;
        npc.moveTo(pos.x, pos.y, pos.z, yaw, 0f);
        npc.setYHeadRot(yaw);
        npc.setYBodyRot(yaw);
        npc.setCustomName(Component.literal(name != null ? name : Component.translatable("entity.hogwartsmod.professor_npc").getString()));
        npc.setCustomNameVisible(true);
        source.getLevel().addFreshEntity(npc);
        source.sendSuccess(() -> Component.translatable("command.hogwartsmod.npc_created", npc.getNpcName()), true);
        return 1;
    }

    private static int createMissionNpc(CommandContext<CommandSourceStack> ctx, @Nullable String name) {
        CommandSourceStack source = ctx.getSource();
        com.gabriel.hogwartsmod.mission.MissionNpcEntity npc = ModEntities.MISSION_NPC.get().create(source.getLevel());
        if (npc == null) {
            return 0;
        }
        Vec3 pos = source.getPosition();
        float yaw = source.getRotation().y + 180f;
        npc.moveTo(pos.x, pos.y, pos.z, yaw, 0f);
        npc.setYHeadRot(yaw);
        npc.setYBodyRot(yaw);
        npc.setCustomName(Component.literal(name != null ? name : Component.translatable("entity.hogwartsmod.npc_missoes").getString()));
        npc.setCustomNameVisible(true);
        source.getLevel().addFreshEntity(npc);
        source.sendSuccess(() -> Component.translatable("command.hogwartsmod.mission_npc_created", npc.getNpcName()), true);
        return 1;
    }

    private static int createShopNpc(CommandContext<CommandSourceStack> ctx, @Nullable String name) {
        CommandSourceStack source = ctx.getSource();
        com.gabriel.hogwartsmod.shop.ShopNpcEntity npc = ModEntities.SHOP_NPC.get().create(source.getLevel());
        if (npc == null) {
            return 0;
        }
        Vec3 pos = source.getPosition();
        float yaw = source.getRotation().y + 180f;
        npc.moveTo(pos.x, pos.y, pos.z, yaw, 0f);
        npc.setYHeadRot(yaw);
        npc.setYBodyRot(yaw);
        npc.setCustomName(Component.literal(name != null ? name : Component.translatable("entity.hogwartsmod.npc_loja").getString()));
        npc.setCustomNameVisible(true);
        source.getLevel().addFreshEntity(npc);
        source.sendSuccess(() -> Component.translatable("command.hogwartsmod.shop_npc_created", npc.getNpcName()), true);
        return 1;
    }

    private static int createCreatureShop(CommandContext<CommandSourceStack> ctx, com.gabriel.hogwartsmod.shop.ShopKind kind, @Nullable String name) {
        CommandSourceStack source = ctx.getSource();
        com.gabriel.hogwartsmod.shop.ShopNpcEntity npc = ModEntities.SHOP_NPC.get().create(source.getLevel());
        if (npc == null) {
            return 0;
        }
        boolean smuggler = kind == com.gabriel.hogwartsmod.shop.ShopKind.CONTRABANDISTA;
        npc.setKind(kind);
        boolean helper = kind == com.gabriel.hogwartsmod.shop.ShopKind.AUXILIAR_VARINHAS;
        if (helper) {
            npc.setModel("auxiliar_varinhas");
        }
        npc.setOffers(smuggler ? com.gabriel.hogwartsmod.shop.Smugglers.get(source.getServer()).template()
                : helper ? com.gabriel.hogwartsmod.shop.Smugglers.wandHelperDefaults() : com.gabriel.hogwartsmod.shop.Smugglers.merchantDefaults());
        Vec3 pos = source.getPosition();
        float yaw = source.getRotation().y + 180f;
        npc.moveTo(pos.x, pos.y, pos.z, yaw, 0f);
        npc.setYHeadRot(yaw);
        npc.setYBodyRot(yaw);
        npc.setCustomName(Component.literal(name != null ? name
                : Component.translatable(smuggler ? "entity.hogwartsmod.contrabandista" : helper ? "entity.hogwartsmod.auxiliar_varinhas" : "entity.hogwartsmod.comerciante").getString()));
        npc.setCustomNameVisible(true);
        source.getLevel().addFreshEntity(npc);
        source.sendSuccess(() -> Component.translatable("command.hogwartsmod.shop_npc_created", npc.getNpcName()), true);
        return 1;
    }

    private static int setNpcModel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Vec3 pos = ctx.getSource().getPosition();
        com.gabriel.hogwartsmod.npc.HogwartsNpc npc = ctx.getSource().getLevel()
                .getEntitiesOfClass(com.gabriel.hogwartsmod.npc.HogwartsNpc.class, new AABB(pos, pos).inflate(8)).stream()
                .min(Comparator.comparingDouble(n -> n.distanceToSqr(pos)))
                .orElseThrow(NO_NPC::create);
        npc.setModel(StringArgumentType.getString(ctx, "modelo"));
        ctx.getSource().sendSuccess(() -> Component.translatable("command.hogwartsmod.npc_model", npc.getNpcName(), npc.getModel()), true);
        return 1;
    }

    private static int renameNpc(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ProfessorNpcEntity npc = nearestNpc(ctx.getSource());
        npc.setCustomName(Component.literal(StringArgumentType.getString(ctx, "nome")));
        npc.setCustomNameVisible(true);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.hogwartsmod.npc_renamed", npc.getNpcName()), true);
        return 1;
    }

    private static int npcSpell(CommandContext<CommandSourceStack> ctx, boolean add) throws CommandSyntaxException {
        ProfessorNpcEntity npc = nearestNpc(ctx.getSource());
        List<Spell> spells = resolve(StringArgumentType.getString(ctx, "feitico"));
        for (Spell spell : spells) {
            if (add) {
                npc.addSpell(spell);
            } else {
                npc.removeSpell(spell);
            }
        }
        String list = npc.getSpells().stream().map(id -> {
            Spell spell = Spells.get(id);
            return spell == null ? id : spell.getDisplayName().getString();
        }).collect(Collectors.joining(", "));
        ctx.getSource().sendSuccess(() -> Component.translatable("command.hogwartsmod.npc_spells", npc.getNpcName(),
                list.isEmpty() ? "-" : list), true);
        return spells.size();
    }

    private static int removeNpc(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Vec3 pos = ctx.getSource().getPosition();
        com.gabriel.hogwartsmod.npc.HogwartsNpc npc = ctx.getSource().getLevel()
                .getEntitiesOfClass(com.gabriel.hogwartsmod.npc.HogwartsNpc.class, new AABB(pos, pos).inflate(8)).stream()
                .min(Comparator.comparingDouble(n -> n.distanceToSqr(pos)))
                .orElseThrow(NO_NPC::create);
        String name = npc.getNpcName();
        npc.discard();
        ctx.getSource().sendSuccess(() -> Component.translatable("command.hogwartsmod.npc_removed", name), true);
        return 1;
    }

    private static int giveWand(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "alvo");
        com.gabriel.hogwartsmod.wand.WandWood wood = com.gabriel.hogwartsmod.wand.WandWood.byId(StringArgumentType.getString(ctx, "madeira"));
        com.gabriel.hogwartsmod.wand.WandCore core = com.gabriel.hogwartsmod.wand.WandCore.byId(StringArgumentType.getString(ctx, "nucleo"));
        com.gabriel.hogwartsmod.wand.WandHandle handle = ctx.getNodes().stream().anyMatch(n -> n.getNode().getName().equals("cabo"))
                ? com.gabriel.hogwartsmod.wand.WandHandle.byId(StringArgumentType.getString(ctx, "cabo")) : com.gabriel.hogwartsmod.wand.WandHandle.NENHUM;
        if (wood == null || core == null || handle == null) {
            throw BAD_WAND.create();
        }
        int quarters = (int) Math.round(com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(ctx, "comprimento") * 4);
        net.minecraft.world.item.ItemStack wand = com.gabriel.hogwartsmod.wand.Wands.create(wood, core, quarters, handle, null);
        Component name = wand.getHoverName();
        if (!target.getInventory().add(wand)) {
            target.drop(wand, false);
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("command.hogwartsmod.wand_given", name, target.getDisplayName()), true);
        return 1;
    }

    private static int setProfession(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "alvo");
        Profession profession = Profession.byId(StringArgumentType.getString(ctx, "profissao"));
        if (profession == null) {
            throw UNKNOWN_PROFESSION.create();
        }
        data(target).setProfession(profession);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.hogwartsmod.profession_set", target.getDisplayName(), profession.getDisplayName()), true);
        return 1;
    }

    private static int setMastery(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "alvo");
        List<Spell> spells = resolve(StringArgumentType.getString(ctx, "feitico"));
        int level = IntegerArgumentType.getInteger(ctx, "nivel");
        MagicData data = data(target);
        int count = 0;
        for (Spell spell : spells) {
            if (data.knows(spell)) {
                data.setMastery(spell, level);
                count++;
            }
        }
        int changed = count;
        ctx.getSource().sendSuccess(() -> Component.translatable("command.hogwartsmod.mastery_set", changed, target.getDisplayName(), level), true);
        return changed;
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

    private static int lives(CommandContext<CommandSourceStack> ctx, int mode, int amount) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "alvo");
        MagicData data = data(target);
        if (mode != 0) {
            data.setLives(mode == 2 ? amount : data.getLives() + mode * amount);
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("command.hogwartsmod.lives", target.getDisplayName(), data.getLives()), mode != 0);
        return data.getLives();
    }

    private static int thestralSight(CommandContext<CommandSourceStack> ctx, boolean value) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "alvo");
        data(target).setSeenDeath(value);
        ctx.getSource().sendSuccess(() -> Component.translatable(value ? "command.hogwartsmod.thestral_sight_on" : "command.hogwartsmod.thestral_sight_off",
                target.getDisplayName()), true);
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

    private static boolean canGivePoints(CommandSourceStack source) {
        if (source.hasPermission(2)) {
            return true;
        }
        MagicData data = source.getPlayer() == null ? null : MagicData.get(source.getPlayer());
        return data != null && data.isProfessor();
    }

    private static com.gabriel.hogwartsmod.house.House houseArg(CommandContext<CommandSourceStack> ctx, String arg) throws CommandSyntaxException {
        String value = StringArgumentType.getString(ctx, arg);
        com.gabriel.hogwartsmod.house.House house = com.gabriel.hogwartsmod.house.House.byId(value);
        if (house != null) {
            return house;
        }
        ServerPlayer player = ctx.getSource().getServer().getPlayerList().getPlayerByName(value);
        if (player == null) {
            throw UNKNOWN_HOUSE.create();
        }
        house = com.gabriel.hogwartsmod.house.HouseManager.of(player);
        if (house == null) {
            throw NO_HOUSE.create(player.getGameProfile().getName());
        }
        return house;
    }

    private static int points(CommandContext<CommandSourceStack> ctx, int sign, @Nullable String reason) throws CommandSyntaxException {
        com.gabriel.hogwartsmod.house.House house = houseArg(ctx, "alvo");
        int amount = IntegerArgumentType.getInteger(ctx, "quantidade");
        com.gabriel.hogwartsmod.house.HousePoints data = com.gabriel.hogwartsmod.house.HousePoints.get(ctx.getSource().getServer());
        int total = data.add(house, sign * amount);
        Component who = ctx.getSource().getDisplayName();
        Component houseName = house.getDisplayName().copy().withStyle(net.minecraft.ChatFormatting.BOLD);
        net.minecraft.network.chat.MutableComponent message = Component.translatable(
                sign > 0 ? "message.hogwartsmod.points_given" : "message.hogwartsmod.points_taken", who, amount, houseName)
                .withStyle(sign > 0 ? net.minecraft.ChatFormatting.GOLD : net.minecraft.ChatFormatting.RED);
        if (reason != null && !reason.isBlank()) {
            message.append(Component.literal(" — " + reason.strip()).withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC));
        }
        ctx.getSource().getServer().getPlayerList().broadcastSystemMessage(message, false);
        for (ServerPlayer member : ctx.getSource().getServer().getPlayerList().getPlayers()) {
            if (com.gabriel.hogwartsmod.house.HouseManager.of(member) == house) {
                member.playNotifySound(sign > 0 ? net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP : net.minecraft.sounds.SoundEvents.VILLAGER_NO,
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.7f, sign > 0 ? 1.2f : 0.9f);
            }
        }
        return total;
    }

    private static int showPoints(CommandContext<CommandSourceStack> ctx) {
        com.gabriel.hogwartsmod.house.HousePoints data = com.gabriel.hogwartsmod.house.HousePoints.get(ctx.getSource().getServer());
        ctx.getSource().sendSuccess(() -> Component.translatable("command.hogwartsmod.points_header"), false);
        List<com.gabriel.hogwartsmod.house.House> order = new ArrayList<>(List.of(com.gabriel.hogwartsmod.house.House.values()));
        order.sort(Comparator.comparingInt(h -> -data.get(h)));
        for (com.gabriel.hogwartsmod.house.House house : order) {
            ctx.getSource().sendSuccess(() -> Component.translatable("command.hogwartsmod.points_line", house.getDisplayName(), data.get(house)), false);
        }
        return 1;
    }

    private static int setPoints(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        com.gabriel.hogwartsmod.house.House house = com.gabriel.hogwartsmod.house.House.byId(StringArgumentType.getString(ctx, "casa"));
        if (house == null) {
            throw UNKNOWN_HOUSE.create();
        }
        int value = IntegerArgumentType.getInteger(ctx, "quantidade");
        com.gabriel.hogwartsmod.house.HousePoints.get(ctx.getSource().getServer()).set(house, value);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.hogwartsmod.points_line", house.getDisplayName(), value), true);
        return 1;
    }

    private static int resetPoints(CommandContext<CommandSourceStack> ctx) {
        com.gabriel.hogwartsmod.house.HousePoints.get(ctx.getSource().getServer()).reset();
        ctx.getSource().sendSuccess(() -> Component.translatable("command.hogwartsmod.points_reset"), true);
        return 1;
    }

    private static int finishCup(CommandContext<CommandSourceStack> ctx) {
        net.minecraft.server.MinecraftServer server = ctx.getSource().getServer();
        com.gabriel.hogwartsmod.house.HousePoints data = com.gabriel.hogwartsmod.house.HousePoints.get(server);
        com.gabriel.hogwartsmod.house.House winner = data.leader();
        if (winner == null) {
            ctx.getSource().sendFailure(Component.translatable("command.hogwartsmod.points_no_winner"));
            return 0;
        }
        int total = data.get(winner);
        data.finish(winner);
        Component name = winner.getDisplayName().copy().withStyle(net.minecraft.ChatFormatting.BOLD);
        server.getPlayerList().broadcastSystemMessage(Component.translatable("message.hogwartsmod.cup_winner", name, total)
                .withStyle(net.minecraft.ChatFormatting.GOLD), false);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(10, 70, 20));
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(name));
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(
                    Component.translatable("message.hogwartsmod.cup_subtitle").withStyle(net.minecraft.ChatFormatting.GOLD)));
            player.playNotifySound(net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, net.minecraft.sounds.SoundSource.PLAYERS, 1f, 1f);
        }
        return 1;
    }

    private static int setHouse(CommandContext<CommandSourceStack> ctx, boolean announce) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "alvo");
        data(target);
        String id = StringArgumentType.getString(ctx, "casa");
        com.gabriel.hogwartsmod.house.House house = com.gabriel.hogwartsmod.house.House.byId(id);
        if (house == null && !id.equalsIgnoreCase("nenhuma")) {
            throw UNKNOWN_HOUSE.create();
        }
        com.gabriel.hogwartsmod.house.HouseManager.set(target, house);
        if (house != null && announce) {
            com.gabriel.hogwartsmod.house.SortingManager.announce(ctx.getSource().getServer(), target, house);
        } else if (house != null) {
            target.displayClientMessage(Component.translatable("message.hogwartsmod.house_set", house.getDisplayName()), false);
        }
        Component label = house == null ? Component.translatable("house.hogwartsmod.none") : house.getDisplayName();
        ctx.getSource().sendSuccess(() -> Component.translatable("command.hogwartsmod.house_set", target.getGameProfile().getName(), label), true);
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "alvo");
        MagicData data = data(target);
        CommandSourceStack source = ctx.getSource();
        source.sendSuccess(() -> Component.translatable("command.hogwartsmod.info_header", target.getDisplayName(),
                data.getLevel(), data.getXp(), MagicData.xpForNextLevel(data.getLevel()),
                Math.round(data.getReserve()), Math.round(data.getMaxReserve()), data.getProfession().getDisplayName()), false);
        source.sendSuccess(() -> Component.translatable("command.hogwartsmod.info_house",
                data.getHouse() == null ? Component.translatable("house.hogwartsmod.none") : data.getHouse().getDisplayName()), false);
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
