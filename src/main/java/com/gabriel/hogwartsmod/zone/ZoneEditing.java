package com.gabriel.hogwartsmod.zone;

import com.gabriel.hogwartsmod.network.ZoneEditPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class ZoneEditing {

    private ZoneEditing() {
    }

    public static boolean isStaff(ServerPlayer player) {
        return player.hasPermissions(2);
    }

    public static void handle(ServerPlayer player, ZoneEditPacket packet) {
        if (!isStaff(player)) {
            return;
        }
        ZoneData data = ZoneData.get(player.server);
        String name = packet.name().strip();
        switch (packet.action()) {
            case CREATE -> create(player, name);
            case REDEFINE -> redefine(player, name);
            case REMOVE -> {
                if (data.remove(name)) {
                    feedback(player, Component.translatable("message.hogwartsmod.zone_removed", name));
                }
            }
            case RULE -> {
                Zone zone = data.find(name);
                ZoneRule rule = ZoneRule.byId(packet.rule());
                if (zone != null && rule != null) {
                    data.put(zone.with(rule, packet.value()));
                }
            }
            case CLEAR_SELECTION -> ZoneSelections.clear(player);
            case EXPAND -> expand(player, name);
            case CUT -> cut(player, name);
            case UNDO_PART -> {
                Zone zone = data.find(name);
                if (zone != null && zone.boxes().size() > 1) {
                    data.put(zone.withoutLast());
                    feedback(player, Component.translatable("message.hogwartsmod.zone_undone", zone.name()));
                }
            }
        }
    }

    public static boolean create(ServerPlayer player, String name) {
        ZoneSelections.Selection selection = ZoneSelections.get(player);
        if (selection == null || !selection.complete() || selection.cut()) {
            feedback(player, Component.translatable("message.hogwartsmod.zone_need_selection"));
            return false;
        }
        if (!validName(name)) {
            feedback(player, Component.translatable("message.hogwartsmod.zone_bad_name"));
            return false;
        }
        ZoneData data = ZoneData.get(player.server);
        if (data.find(name) != null) {
            feedback(player, Component.translatable("message.hogwartsmod.zone_exists", name));
            return false;
        }
        data.put(Zone.of(name, selection.dimension(), selection.first(), selection.second(), 0));
        ZoneSelections.clear(player);
        feedback(player, Component.translatable("message.hogwartsmod.zone_created", name));
        return true;
    }

    public static boolean redefine(ServerPlayer player, String name) {
        ZoneSelections.Selection selection = ZoneSelections.get(player);
        ZoneData data = ZoneData.get(player.server);
        Zone zone = data.find(name);
        if (zone == null) {
            return false;
        }
        if (selection == null || !selection.complete() || selection.cut()) {
            feedback(player, Component.translatable("message.hogwartsmod.zone_need_selection"));
            return false;
        }
        data.put(Zone.of(zone.name(), selection.dimension(), selection.first(), selection.second(), zone.rules()));
        ZoneSelections.clear(player);
        feedback(player, Component.translatable("message.hogwartsmod.zone_redefined", zone.name()));
        return true;
    }

    public static boolean expand(ServerPlayer player, String name) {
        ZoneSelections.Selection selection = ZoneSelections.get(player);
        ZoneData data = ZoneData.get(player.server);
        Zone zone = data.find(name);
        if (zone == null) {
            return false;
        }
        if (selection == null || !selection.complete() || selection.cut()) {
            feedback(player, Component.translatable("message.hogwartsmod.zone_need_selection"));
            return false;
        }
        if (!zone.contains(selection.dimension(), selection.first()) && !zone.contains(selection.dimension(), selection.second())) {
            feedback(player, Component.translatable("message.hogwartsmod.zone_not_touching", zone.name()));
            return false;
        }
        if (zone.boxes().size() >= Zone.MAX_PARTS) {
            feedback(player, Component.translatable("message.hogwartsmod.zone_too_many", Zone.MAX_PARTS));
            return false;
        }
        data.put(zone.plus(selection.first(), selection.second()));
        ZoneSelections.clear(player);
        feedback(player, Component.translatable("message.hogwartsmod.zone_expanded", zone.name()));
        return true;
    }

    public static boolean cut(ServerPlayer player, String name) {
        ZoneSelections.Selection selection = ZoneSelections.get(player);
        ZoneData data = ZoneData.get(player.server);
        Zone zone = data.find(name);
        if (zone == null) {
            return false;
        }
        if (selection == null || !selection.complete() || !selection.cut()) {
            feedback(player, Component.translatable("message.hogwartsmod.zone_need_cut"));
            return false;
        }
        if (!zone.touches(selection.dimension(), Zone.Box.of(selection.first(), selection.second()))) {
            feedback(player, Component.translatable("message.hogwartsmod.zone_cut_outside", zone.name()));
            return false;
        }
        if (zone.boxes().size() >= Zone.MAX_PARTS) {
            feedback(player, Component.translatable("message.hogwartsmod.zone_too_many", Zone.MAX_PARTS));
            return false;
        }
        Zone result = zone.plus(selection.first(), selection.second(), true);
        if (result.volume() == 0) {
            feedback(player, Component.translatable("message.hogwartsmod.zone_cut_all", zone.name()));
            return false;
        }
        data.put(result);
        ZoneSelections.clear(player);
        feedback(player, Component.translatable("message.hogwartsmod.zone_cut_done", zone.name()));
        return true;
    }

    public static boolean validName(String name) {
        return !name.isEmpty() && name.length() <= ZoneEditPacket.MAX_NAME;
    }

    private static void feedback(ServerPlayer player, Component message) {
        player.displayClientMessage(message.copy().withStyle(ChatFormatting.GOLD), true);
    }
}
