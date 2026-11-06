package com.gabriel.hogwartsmod.client.zone;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.zone.Zone;
import com.gabriel.hogwartsmod.zone.Zones;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = HogwartsMod.MOD_ID, value = Dist.CLIENT)
public final class ClientZones {
    public static final int BANNER_TICKS = 80;

    @Nullable
    public static BlockPos first;
    @Nullable
    public static BlockPos second;

    public static boolean cut;

    @Nullable
    public static Zone banner;
    public static boolean bannerLeaving;
    public static int bannerAge = -1;

    private static Set<String> inside = new LinkedHashSet<>();
    private static boolean primed;

    private ClientZones() {
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || mc.player == null || mc.level == null) {
            return;
        }
        if (bannerAge >= 0 && ++bannerAge > BANNER_TICKS) {
            bannerAge = -1;
            banner = null;
        }
        List<Zone> here = Zones.at(mc.level, mc.player.position());
        Set<String> now = new LinkedHashSet<>();
        here.forEach(z -> now.add(z.name()));
        if (!primed) {
            primed = true;
            inside = now;
            return;
        }
        Zone entered = null;
        for (Zone zone : here) {
            if (!inside.contains(zone.name())) {
                entered = zone;
            }
        }
        if (entered != null) {
            show(entered, false);
        } else if (now.isEmpty() && !inside.isEmpty()) {
            String left = inside.iterator().next();
            for (Zone zone : Zones.clientZones) {
                if (zone.name().equals(left)) {
                    show(zone, true);
                }
            }
        }
        inside = now;
    }

    private static void show(Zone zone, boolean leaving) {
        banner = zone;
        bannerLeaving = leaving;
        bannerAge = 0;
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        first = null;
        second = null;
        cut = false;
        banner = null;
        bannerAge = -1;
        inside = new LinkedHashSet<>();
        primed = false;
        Zones.clientZones = List.of();
    }

    public static List<Zone> inDimension() {
        Minecraft mc = Minecraft.getInstance();
        List<Zone> list = new ArrayList<>();
        if (mc.level == null) {
            return list;
        }
        String dim = mc.level.dimension().location().toString();
        for (Zone zone : Zones.clientZones) {
            if (zone.dimension().equals(dim)) {
                list.add(zone);
            }
        }
        return list;
    }
}
