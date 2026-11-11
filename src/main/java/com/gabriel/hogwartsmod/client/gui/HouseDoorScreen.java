package com.gabriel.hogwartsmod.client.gui;

import com.gabriel.hogwartsmod.house.door.HouseDoors;
import com.gabriel.hogwartsmod.network.HouseDoorAttemptPacket;
import com.gabriel.hogwartsmod.network.ModNetwork;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public abstract class HouseDoorScreen extends Screen {
    protected final BlockPos pos;
    protected int left;
    protected int top;
    private boolean waiting;

    protected HouseDoorScreen(BlockPos pos, Component title) {
        super(title);
        this.pos = pos;
    }

    protected abstract int screenWidth();

    protected abstract int screenHeight();

    @Override
    protected void init() {
        left = (width - screenWidth()) / 2;
        top = (height - screenHeight()) / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    protected boolean isWaiting() {
        return waiting;
    }

    protected void send(String text, int[] values, int extra) {
        if (waiting) {
            return;
        }
        waiting = true;
        ModNetwork.CHANNEL.sendToServer(new HouseDoorAttemptPacket(pos, text, values, extra));
    }

    public void onResult(int result) {
        waiting = false;
        if (result != HouseDoors.RESULT_STEP) {
            onClose();
        }
    }

    protected void sound(SoundEvent sound, float pitch) {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch));
        }
    }

    protected void click() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
        }
    }

    protected static boolean over(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
