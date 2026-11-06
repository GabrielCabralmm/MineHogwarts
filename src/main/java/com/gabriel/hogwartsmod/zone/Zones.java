package com.gabriel.hogwartsmod.zone;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class Zones {
    public static volatile List<Zone> clientZones = List.of();

    private Zones() {
    }

    public static List<Zone> all(Level level) {
        if (level.isClientSide) {
            return clientZones;
        }
        return level.getServer() == null ? List.of() : ZoneData.get(level.getServer()).all();
    }

    public static List<Zone> at(Level level, Vec3 pos) {
        String dim = level.dimension().location().toString();
        List<Zone> found = new ArrayList<>();
        for (Zone zone : all(level)) {
            if (zone.contains(dim, pos)) {
                found.add(zone);
            }
        }
        return found;
    }

    public static boolean blocks(Level level, Vec3 pos, ZoneRule rule) {
        for (Zone zone : at(level, pos)) {
            if (zone.has(rule)) {
                return true;
            }
        }
        return false;
    }

    public static boolean blocks(Entity entity, ZoneRule rule) {
        return blocks(entity.level(), entity.position(), rule);
    }

    public static void warn(Player player, ZoneRule rule) {
        if (!player.level().isClientSide) {
            player.displayClientMessage(Component.translatable(rule.blockedKey()).withStyle(ChatFormatting.GOLD), true);
        }
    }
}
