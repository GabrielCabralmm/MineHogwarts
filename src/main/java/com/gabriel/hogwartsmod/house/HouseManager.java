package com.gabriel.hogwartsmod.house;

import com.gabriel.hogwartsmod.HogwartsMod;
import com.gabriel.hogwartsmod.magic.MagicData;
import com.gabriel.hogwartsmod.network.HousePacket;
import com.gabriel.hogwartsmod.network.ModNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = HogwartsMod.MOD_ID)
public final class HouseManager {
    public static final Map<UUID, House> CLIENT_HOUSES = new ConcurrentHashMap<>();

    private HouseManager() {
    }

    @Nullable
    public static House of(Player player) {
        if (player.level().isClientSide) {
            return CLIENT_HOUSES.get(player.getUUID());
        }
        MagicData data = MagicData.get(player);
        return data == null ? null : data.getHouse();
    }

    public static void set(ServerPlayer player, @Nullable House house) {
        MagicData data = MagicData.get(player);
        if (data == null) {
            return;
        }
        data.setHouse(house);
        ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), new HousePacket(player.getUUID(), house == null ? "" : house.getId()));
        player.refreshDisplayName();
        player.refreshTabListName();
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
            House house = of(other);
            if (house != null) {
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new HousePacket(other.getUUID(), house.getId()));
            }
        }
        House own = of(player);
        if (own != null) {
            ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), new HousePacket(player.getUUID(), own.getId()));
        }
        player.refreshDisplayName();
        player.refreshTabListName();
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void nameFormat(PlayerEvent.NameFormat event) {
        House house = of(event.getEntity());
        if (house != null) {
            event.setDisplayname(color(event.getDisplayname(), house));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void tabFormat(PlayerEvent.TabListNameFormat event) {
        House house = of(event.getEntity());
        if (house != null) {
            Component base = event.getDisplayName() != null ? event.getDisplayName() : event.getEntity().getName();
            event.setDisplayName(color(base, house));
        }
    }

    private static Component color(Component name, House house) {
        MutableComponent copy = name.copy();
        return copy.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(house.getColor())));
    }
}
