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
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new OpenShopPacket(npc.getId(), npc.getNpcName(), npc.getKind().ordinal(), npc.getOffers()));
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

    public static boolean needsLicense(ShopKind kind, ShopOffer offer) {
        return kind == ShopKind.COMERCIANTE && (offer.species() != null || offer.isBag());
    }

    @org.jetbrains.annotations.Nullable
    public static String blocker(Player player, ShopKind kind, ShopOffer offer) {
        if (offer.soldOut()) {
            return offer.sell() ? "screen.hogwartsmod.shop.not_buying" : "screen.hogwartsmod.shop.sold_out";
        }
        if (offer.sell()) {
            return null;
        }
        com.gabriel.hogwartsmod.creature.Species species = offer.species();
        com.gabriel.hogwartsmod.magic.MagicData data = com.gabriel.hogwartsmod.magic.MagicData.get(player);
        if (species != null && !hasBag(player)) {
            return "screen.hogwartsmod.shop.need_bag";
        }
        if (species != null && data != null && data.getCreatures().get(species) != null) {
            return "screen.hogwartsmod.shop.has_creature";
        }
        if (kind == ShopKind.AUXILIAR_VARINHAS && offer.isWandBox() && data != null && data.hasFirstWand()) {
            return "screen.hogwartsmod.shop.has_first_wand";
        }
        if (offer.isLicense() && CreatureLicenseItem.has(player)) {
            return "screen.hogwartsmod.shop.has_license";
        }
        if (needsLicense(kind, offer) && !CreatureLicenseItem.has(player)) {
            return "screen.hogwartsmod.shop.need_license";
        }
        return null;
    }

    public static boolean hasBag(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof com.gabriel.hogwartsmod.creature.CreatureBagItem) {
                return true;
            }
        }
        return player.getOffhandItem().getItem() instanceof com.gabriel.hogwartsmod.creature.CreatureBagItem;
    }

    public static boolean canBuy(Player player, ShopKind kind, ShopOffer offer) {
        return blocker(player, kind, offer) == null && canPay(player, offer);
    }

    public static boolean canPay(Player player, ShopOffer offer) {
        if (offer.sell()) {
            return count(player, offer.product().item()) >= offer.product().count();
        }
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
        String blocked = blocker(player, npc.getKind(), offer);
        if (blocked != null) {
            player.displayClientMessage(Component.translatable(blocked).withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (offer.sell()) {
            sell(player, npc, offer, times);
            return;
        }
        int max = offer.available(offer.single() ? 1 : Math.max(1, Math.min(BULK, times)));
        for (int i = 0; i < max; i++) {
            if (!canPay(player, offer) || !Wallet.pay(player, offer.galeoes(), offer.sicles(), offer.nuques())) {
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
            if (offer.species() != null) {
                com.gabriel.hogwartsmod.creature.CreatureManager.giveTamed(player, offer.species());
            } else if (offer.isLicense()) {
                ItemStack license = new ItemStack(offer.product().item());
                CreatureLicenseItem.bind(license, player);
                if (!player.getInventory().add(license)) {
                    player.drop(license, false);
                }
            } else {
                give(player, offer.product());
            }
            if (npc.getKind() == ShopKind.AUXILIAR_VARINHAS && offer.isWandBox()) {
                com.gabriel.hogwartsmod.magic.MagicData data = com.gabriel.hogwartsmod.magic.MagicData.get(player);
                if (data != null) {
                    data.setFirstWand(true);
                }
            }
            bought++;
        }
        if (bought > 0 && offer.limited()) {
            npc.replace(offer.withStock(offer.stock() - bought));
        }
        if (bought == 0) {
            player.displayClientMessage(Component.translatable("message.hogwartsmod.shop_cannot").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        npc.talk();
        player.level().playSound(null, player.blockPosition(), SoundEvents.CHAIN_STEP, SoundSource.PLAYERS, 1f, 1.6f);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6f, 1.2f);
        if (offer.species() == null) player.displayClientMessage(Component.translatable("message.hogwartsmod.shop_bought",
                offer.product().count() * bought, offer.product().item().getDescription()).withStyle(ChatFormatting.GOLD), true);
        player.inventoryMenu.broadcastChanges();
        open(player, npc);
    }

    private static void sell(ServerPlayer player, ShopNpcEntity npc, ShopOffer offer, int times) {
        int max = offer.available(Math.max(1, Math.min(BULK, times)));
        Item item = offer.product().item();
        int sold = 0;
        for (int i = 0; i < max && count(player, item) >= offer.product().count(); i++) {
            int left = offer.product().count();
            for (ItemStack stack : player.getInventory().items) {
                if (left > 0 && stack.is(item) && !stack.hasTag()) {
                    int take = Math.min(left, stack.getCount());
                    stack.shrink(take);
                    left -= take;
                }
            }
            sold++;
        }
        if (sold == 0) {
            player.displayClientMessage(Component.translatable("message.hogwartsmod.shop_cannot_sell").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        long total = offer.price() * sold;
        int g = (int) (total / (17 * 29));
        total -= (long) g * 17 * 29;
        int s = (int) (total / 29);
        total -= (long) s * 29;
        com.gabriel.hogwartsmod.money.MoneyHelper.give(player, g, s, (int) total);
        if (offer.limited()) {
            npc.replace(offer.withStock(offer.stock() - sold));
        }
        npc.talk();
        player.level().playSound(null, player.blockPosition(), SoundEvents.CHAIN_STEP, SoundSource.PLAYERS, 1f, 1.6f);
        player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 1.4f);
        player.displayClientMessage(Component.translatable("message.hogwartsmod.shop_sold",
                offer.product().count() * sold, item.getDescription()).withStyle(ChatFormatting.GOLD), true);
        player.inventoryMenu.broadcastChanges();
        open(player, npc);
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
        if (npc.getKind() == ShopKind.CONTRABANDISTA) {
            Smugglers.get(player.server).setTemplate(npc.getOffers());
            player.sendSystemMessage(Component.translatable("message.hogwartsmod.smuggler_template").withStyle(ChatFormatting.GRAY));
        }
        player.displayClientMessage(Component.translatable("message.hogwartsmod.mission_npc_saved", npc.getNpcName())
                .withStyle(ChatFormatting.GOLD), true);
    }
}
