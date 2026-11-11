package com.gabriel.hogwartsmod.broom;

import com.gabriel.hogwartsmod.wand.WandWood;
import net.minecraft.world.item.Item;

public class BroomHandleItem extends Item {
    private final WandWood wood;

    public BroomHandleItem(WandWood wood, Properties properties) {
        super(properties);
        this.wood = wood;
    }

    public WandWood getWood() {
        return wood;
    }
}
