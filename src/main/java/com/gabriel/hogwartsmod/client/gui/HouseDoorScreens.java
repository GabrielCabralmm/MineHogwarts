package com.gabriel.hogwartsmod.client.gui;

import com.gabriel.hogwartsmod.house.House;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public final class HouseDoorScreens {

    private HouseDoorScreens() {
    }

    public static void open(BlockPos pos, House house, int a, int b, String text) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(switch (house) {
            case GRIFINORIA -> new GryffindorDoorScreen(pos);
            case SONSERINA -> new SlytherinDoorScreen(pos, a, b);
            case CORVINAL -> new RavenclawDoorScreen(pos, a, text);
            case LUFA_LUFA -> new HufflepuffDoorScreen(pos, a);
        });
    }

    public static void result(int result) {
        if (Minecraft.getInstance().screen instanceof HouseDoorScreen screen) {
            screen.onResult(result);
        }
    }
}
