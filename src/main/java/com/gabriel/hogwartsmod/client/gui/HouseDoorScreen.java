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

    // -- Campo de texto

    protected final StringBuilder typed = new StringBuilder();
    protected int maxTyped = 48;

    protected void drawField(net.minecraft.client.gui.GuiGraphics g, float cx, int y, int halfWidth, String hint) {
        g.fill((int) cx - halfWidth, y + 12, (int) cx + halfWidth, y + 13, MissionScreen.LINE);
        g.fill((int) cx - halfWidth + 10, y + 14, (int) cx + halfWidth - 10, y + 15, 0x30C4AA7C);
        if (typed.length() == 0) {
            HiRes.centered(g, hint, HiRes.Style.ITALIC, 7.5f, cx, y + 2, MissionScreen.MUTED);
        } else {
            String shown = HiRes.ellipsize(typed.toString(), HiRes.Style.REGULAR, halfWidth * 2 * HiRes.TEXT_SIZE / 9f);
            HiRes.centered(g, shown, HiRes.Style.REGULAR, 9f, cx, y + 1, MissionScreen.INK);
            if ((System.currentTimeMillis() / 500) % 2 == 0) {
                float w = HiRes.width(shown, HiRes.Style.REGULAR, 9f);
                g.fill((int) (cx + w / 2f) + 1, y + 1, (int) (cx + w / 2f) + 2, y + 11, MissionScreen.INK);
            }
        }
    }

    public void type(String text) {
        typed.setLength(0);
        typed.append(text, 0, Math.min(text.length(), maxTyped));
    }

    protected abstract void submit();

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (acceptsText() && net.minecraft.SharedConstants.isAllowedChatCharacter(c) && typed.length() < maxTyped) {
            typed.append(c);
            return true;
        }
        return super.charTyped(c, modifiers);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (acceptsText()) {
            if (key == 259 && typed.length() > 0) {
                typed.setLength(typed.length() - 1);
                return true;
            }
            if (key == 257 || key == 335) {
                submit();
                return true;
            }
            if (key == 86 && hasControlDown() && minecraft != null) {
                type(typed + minecraft.keyboardHandler.getClipboard());
                return true;
            }
        }
        return super.keyPressed(key, scan, modifiers);
    }

    protected boolean acceptsText() {
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
