package com.gabriel.hogwartsmod.wand;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

public enum WandCore {
    FENIX("fenix", "pena_fenix", false, 0xE8692A),
    DRAGAO("dragao", "corda_coracao_dragao", true, 0xA8202A),
    UNICORNIO("unicornio", "pelo_unicornio", true, 0xE6E4F2);

    private final String id;
    private final String itemId;
    private final boolean common;
    private final int color;
    private RegistryObject<Item> item;

    WandCore(String id, String itemId, boolean common, int color) {
        this.id = id;
        this.itemId = itemId;
        this.common = common;
        this.color = color;
    }

    public boolean isCommon() {
        return common;
    }

    public String getId() {
        return id;
    }

    public String getItemId() {
        return itemId;
    }

    public int getColor() {
        return color;
    }

    public Item getItem() {
        return item.get();
    }

    void bind(RegistryObject<Item> item) {
        this.item = item;
    }

    public Component getDisplayName() {
        return Component.translatable("core.hogwartsmod." + id);
    }

    @Nullable
    public static WandCore byId(String id) {
        for (WandCore core : values()) {
            if (core.id.equals(id)) {
                return core;
            }
        }
        return null;
    }

    @Nullable
    public static WandCore byItem(Item item) {
        for (WandCore core : values()) {
            if (core.item != null && core.item.get() == item) {
                return core;
            }
        }
        return null;
    }
}
