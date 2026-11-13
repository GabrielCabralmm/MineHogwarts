package com.gabriel.hogwartsmod.shop;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record ShopOffer(String id, Goods product, int galeoes, int sicles, int nuques, List<Goods> costs,
                        boolean sell, int max, int stock, int restockDays, long lastRestock) {
    public static final int MAX_COSTS = 3;
    public static final int MAX_STOCK = 9999;
    public static final int MAX_DAYS = 30;

    public ShopOffer(String id, Goods product, int galeoes, int sicles, int nuques, List<Goods> costs) {
        this(id, product, galeoes, sicles, nuques, costs, false, 0, 0, 0, 0);
    }

    public record Goods(String id, int count) {
        public Item item() {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            Item item = rl == null ? Items.AIR : BuiltInRegistries.ITEM.get(rl);
            return item == null ? Items.AIR : item;
        }

        public ItemStack stack() {
            return new ItemStack(item(), Math.max(1, Math.min(count, item().getMaxStackSize())));
        }

        public Goods withCount(int value) {
            return new Goods(id, value);
        }

        public static Goods of(Item item, int count) {
            return new Goods(BuiltInRegistries.ITEM.getKey(item).toString(), count);
        }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Id", id);
            tag.putInt("Quantidade", count);
            return tag;
        }

        static Goods load(CompoundTag tag) {
            return new Goods(tag.getString("Id"), tag.getInt("Quantidade"));
        }
    }

    public ShopOffer {
        costs = costs == null ? List.of() : List.copyOf(costs);
    }

    public static ShopOffer blank() {
        return new ShopOffer(UUID.randomUUID().toString().substring(0, 8), new Goods("minecraft:bread", 1), 0, 1, 0, List.of());
    }

    public static ShopOffer blankSell() {
        Item first = BuiltInRegistries.ITEM.stream().filter(ShopOffer::sellable).findFirst().orElse(Items.PAPER);
        return new ShopOffer(UUID.randomUUID().toString().substring(0, 8), Goods.of(first, 1), 0, 0, 5, List.of(), true, 0, 0, 0, 0);
    }

    public static boolean sellable(Item item) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        return key != null && key.getNamespace().equals(com.gabriel.hogwartsmod.HogwartsMod.MOD_ID)
                && !(item instanceof com.gabriel.hogwartsmod.potion.PotionBookItem)
                && com.gabriel.hogwartsmod.money.Coin.of(new ItemStack(item)) == null;
    }

    public boolean limited() {
        return max > 0;
    }

    public boolean soldOut() {
        return limited() && stock <= 0;
    }

    public int available(int wanted) {
        return limited() ? Math.min(wanted, Math.max(0, stock)) : wanted;
    }

    public ShopOffer withStock(int value) { return new ShopOffer(id, product, galeoes, sicles, nuques, costs, sell, max, Math.max(0, Math.min(max, value)), restockDays, lastRestock); }
    public ShopOffer withLimit(int newMax, int days) {
        int m = Math.max(0, Math.min(MAX_STOCK, newMax));
        int st = m == 0 ? 0 : (max == 0 ? m : Math.min(stock, m));
        return new ShopOffer(id, product, galeoes, sicles, nuques, costs, sell, m, st, Math.max(0, Math.min(MAX_DAYS, days)), lastRestock);
    }
    public ShopOffer restocked(long day) { return new ShopOffer(id, product, galeoes, sicles, nuques, costs, sell, max, max, restockDays, day); }

    @org.jetbrains.annotations.Nullable
    public com.gabriel.hogwartsmod.creature.Species species() {
        return com.gabriel.hogwartsmod.creature.Species.ofEgg(product.item());
    }

    public boolean isLicense() {
        return product.item() instanceof CreatureLicenseItem;
    }

    public boolean isBag() {
        return product.item() instanceof com.gabriel.hogwartsmod.creature.CreatureBagItem;
    }

    public boolean isWandBox() {
        return product.item() instanceof com.gabriel.hogwartsmod.wand.WandBoxItem;
    }

    public boolean single() {
        return species() != null || isLicense() || isWandBox();
    }

    public long price() {
        return (long) galeoes * 17 * 29 + (long) sicles * 29 + nuques;
    }

    public boolean free() {
        return price() <= 0 && costs.isEmpty();
    }

    public ShopOffer sanitized() {
        Goods p = product.item() == Items.AIR ? new Goods("minecraft:bread", 1) : product;
        p = p.withCount(Math.max(1, Math.min(p.item().getMaxStackSize() * 4, p.count())));
        if (isSingle(p)) {
            p = p.withCount(1);
        }
        List<Goods> cs = sell ? List.of() : costs.stream().filter(c -> c.item() != Items.AIR).limit(MAX_COSTS)
                .map(c -> c.withCount(Math.max(1, Math.min(c.item().getMaxStackSize() * 4, c.count())))).toList();
        int m = Math.max(0, Math.min(MAX_STOCK, max));
        return new ShopOffer(id, p, clamp(galeoes), clamp(sicles), clamp(nuques), cs, sell, m, m == 0 ? 0 : Math.max(0, Math.min(m, stock)),
                Math.max(0, Math.min(MAX_DAYS, restockDays)), lastRestock);
    }

    private static boolean isSingle(Goods p) {
        return com.gabriel.hogwartsmod.creature.Species.ofEgg(p.item()) != null || p.item() instanceof CreatureLicenseItem;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(9999, v));
    }

    public ShopOffer withProduct(Goods v) { return new ShopOffer(id, v, galeoes, sicles, nuques, costs, sell, max, stock, restockDays, lastRestock); }
    public ShopOffer withPrice(int g, int s, int n) { return new ShopOffer(id, product, g, s, n, costs, sell, max, stock, restockDays, lastRestock); }
    public ShopOffer withCosts(List<Goods> v) { return new ShopOffer(id, product, galeoes, sicles, nuques, v, sell, max, stock, restockDays, lastRestock); }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Id", id);
        tag.put("Produto", product.save());
        tag.putInt("Galeoes", galeoes);
        tag.putInt("Sicles", sicles);
        tag.putInt("Nuques", nuques);
        ListTag list = new ListTag();
        costs.forEach(c -> list.add(c.save()));
        tag.put("Pede", list);
        tag.putBoolean("Compra", sell);
        tag.putInt("EstoqueMax", max);
        tag.putInt("Estoque", stock);
        tag.putInt("ReporDias", restockDays);
        tag.putLong("UltimaReposicao", lastRestock);
        return tag;
    }

    public static ShopOffer load(CompoundTag tag) {
        List<Goods> costs = new ArrayList<>();
        ListTag list = tag.getList("Pede", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            costs.add(Goods.load(list.getCompound(i)));
        }
        return new ShopOffer(tag.getString("Id"), Goods.load(tag.getCompound("Produto")), tag.getInt("Galeoes"), tag.getInt("Sicles"),
                tag.getInt("Nuques"), costs, tag.getBoolean("Compra"), tag.getInt("EstoqueMax"), tag.getInt("Estoque"),
                tag.getInt("ReporDias"), tag.getLong("UltimaReposicao"));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeNbt(save());
    }

    public static ShopOffer read(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return load(tag == null ? new CompoundTag() : tag);
    }
}
