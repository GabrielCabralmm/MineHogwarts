package com.gabriel.hogwartsmod.client.house;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.house.HouseManager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = HogwartsMod.MOD_ID, value = Dist.CLIENT)
public final class ClientSorting {
    private static final Map<Integer, Long> WEARING = new HashMap<>();

    private ClientSorting() {
    }

    public static void setWearing(int entity, boolean on) {
        if (on) {
            WEARING.putIfAbsent(entity, now());
        } else {
            WEARING.remove(entity);
        }
    }

    public static boolean isWearing(int entity) {
        return WEARING.containsKey(entity);
    }

    public static float age(int entity, float partial) {
        Long start = WEARING.get(entity);
        return start == null ? 0 : now() - start + partial;
    }

    private static long now() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 0 : mc.level.getGameTime();
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        WEARING.clear();
        HouseManager.CLIENT_HOUSES.clear();
    }
}
