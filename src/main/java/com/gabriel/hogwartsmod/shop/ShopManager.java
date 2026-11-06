package com.gabriel.hogwartsmod.shop;

import com.gabriel.hogwartsmod.money.Wallet;
import com.gabriel.hogwartsmod.network.ModNetwork;
import com.gabriel.hogwartsmod.network.OpenShopEditorPacket;
import com.gabriel.hogwartsmod.network.OpenShopPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;

public final class ShopManager {
    private static final double REACH = 6.0;
    public static final int BULK = 10;

    private ShopManager() {
    }

    // -- Telas

    public static void open(ServerPlayer player, ShopNpcEntity npc) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenShopPacket(npc.getId(), npc.getNpcName(), npc.getOffers()));
    }

    public static void openEditor(ServerPlayer player, ShopNpcEntity npc) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new OpenShopEditorPacket(npc.getId(), npc.getNpcName(), npc.getModel(), npc.getOffers()));
    }

    // -- Compra

    public static int count(Player player, Item item) {
        int n = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item) && !stack.hasTag()) {
                n += stack.getCount();
            }
        }
        return n;
    }

    public static boolean canBuy(Player player, ShopOffer offer) {
        if (Wallet.value(player) < offer.price()) {
            return false;
        }
        for (ShopOffer.Goods cost : offer.costs()) {
            if (count(player, cost.item()) < cost.count()) {
                return false;
            }
        }
        return true;
    }

    public static void buy(ServerPlayer player, int entityId, String offerId, int times) {
        if (!(player.level().getEntity(entityId) instanceof ShopNpcEntity npc) || npc.distanceTo(player) > REACH) {
            return;
        }
        ShopOffer offer = npc.offer(offerId);
        if (offer == null) {
            return;
        }
        int bought = 0;
        for (int i = 0; i < Math.max(1, Math.min(BULK, times)); i++) {
            if (!canBuy(player, offer) || !Wallet.pay(player, offer.galeoes(), offer.sicles(), offer.nuques())) {
                break;
            }
            for (ShopOffer.Goods cost : offer.costs()) {
                int left = cost.count();
                for (ItemStack stack : player.getInventory().items) {
                    if (left > 0 && stack.is(cost.item()) && !stack.hasTag()) {
                        int take = Math.min(left, stack.getCount());
                        stack.shrink(take);
                        left -= take;
                    }
                }
            }
            give(player, offer.product());
            bought++;
        }
        if (bought == 0) {
            player.displayClientMessage(Component.translatable("message.hogwartsmod.shop_cannot").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        npc.talk();
        player.level().playSound(null, player.blockPosition(), SoundEvents.CHAIN_STEP, SoundSource.PLAYERS, 1f, 1.6f);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6f, 1.2f);
        player.displayClientMessage(Component.translatable("message.hogwartsmod.shop_bought",
                offer.product().count() * bought, offer.product().item().getDescription()).withStyle(ChatFormatting.GOLD), true);
        player.inventoryMenu.broadcastChanges();
    }

    private static void give(ServerPlayer player, ShopOffer.Goods goods) {
        int left = goods.count();
        Item item = goods.item();
        while (left > 0) {
            int n = Math.min(item.getMaxStackSize(), left);
            ItemStack stack = new ItemStack(item, n);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            left -= n;
        }
    }

    // -- Edição da staff

    public static void save(ServerPlayer player, int entityId, String name, String model, List<ShopOffer> offers) {
        if (!player.hasPermissions(2) || !(player.level().getEntity(entityId) instanceof ShopNpcEntity npc)) {
            return;
        }
        String clean = name.strip();
        if (!clean.isEmpty()) {
            npc.setCustomName(Component.literal(clean.length() > 32 ? clean.substring(0, 32) : clean));
            npc.setCustomNameVisible(true);
        }
        npc.setModel(model);
        npc.setOffers(offers);
        player.displayClientMessage(Component.translatable("message.hogwartsmod.mission_npc_saved", npc.getNpcName())
                .withStyle(ChatFormatting.GOLD), true);
    }
}
