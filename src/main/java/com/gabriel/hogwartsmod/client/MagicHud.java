package com.gabriel.hogwartsmod.client;

import com.gabriel.hogwartsmod.client.gui.SpellIcons;
import com.gabriel.hogwartsmod.magic.MagicData;
import com.gabriel.hogwartsmod.spell.Spell;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public final class MagicHud implements IGuiOverlay {
    public static final MagicHud INSTANCE = new MagicHud();

    private static final int PARCHMENT = 0xE0ECDCB6;
    private static final int EDGE = 0xFF5C3A22;
    private static final int GOLD = 0xFFBE8C28;
    private static final int COOLDOWN = 0xA0281E14;
    private static final int RESERVE = 0xFF5A96E6;
    private static final int BAR_BACK = 0xB0281E14;
    private static final int SLOT = 18;
    private static final int GAP = 2;
    private static final int MARGIN = 4;

    private MagicHud() {
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || player.isSpectator()) {
            return;
        }
        MagicData data = MagicData.get(player);
        if (data == null) {
            return;
        }
        Font font = mc.font;
        long now = player.level().getGameTime();
        int totalWidth = MagicData.SLOT_COUNT * (SLOT + GAP) - GAP;
        int barY = height - MARGIN - 4;
        int nameY = barY - 11;
        int slotY = nameY - 2 - SLOT;

        for (int i = 0; i < MagicData.SLOT_COUNT; i++) {
            int x = MARGIN + i * (SLOT + GAP);
            boolean active = i == data.getActiveSlot();
            g.fill(x, slotY, x + SLOT, slotY + SLOT, active ? GOLD : EDGE);
            g.fill(x + 1, slotY + 1, x + SLOT - 1, slotY + SLOT - 1, PARCHMENT);
            if (active) {
                g.fill(x + 1, slotY + 1, x + SLOT - 1, slotY + 2, GOLD);
                g.fill(x + 1, slotY + SLOT - 2, x + SLOT - 1, slotY + SLOT - 1, GOLD);
                g.fill(x + 1, slotY + 1, x + 2, slotY + SLOT - 1, GOLD);
                g.fill(x + SLOT - 2, slotY + 1, x + SLOT - 1, slotY + SLOT - 1, GOLD);
            }
            Spell spell = data.getSlot(i);
            if (spell == null) {
                continue;
            }
            SpellIcons.draw(g, spell, x + 1, slotY + 1, 16);
            int remaining = data.getCooldownRemaining(spell, now);
            int total = data.getCooldownTotal(spell);
            if (remaining > 0 && total > 0) {
                int inner = SLOT - 2;
                int filled = Math.max(1, Math.round(inner * (remaining / (float) total)));
                g.fill(x + 1, slotY + 1 + inner - filled, x + SLOT - 1, slotY + SLOT - 1, COOLDOWN);
                String seconds = String.valueOf((int) Math.ceil(remaining / 20.0));
                g.drawString(font, seconds, x + (SLOT - font.width(seconds)) / 2 + 1, slotY + 5, 0xFFFFFFFF, true);
            }
        }

        int abilityY = slotY - 13;
        for (Spell ability : abilities(data)) {
            int remaining = data.getCooldownRemaining(ability, now);
            String key = keyFor(ability);
            String label = "[" + key + "] " + ability.getDisplayName().getString();
            int w = 12 + 3 + font.width(label) + 4;
            g.fill(MARGIN, abilityY, MARGIN + w, abilityY + 11, remaining > 0 ? 0x90281E14 : 0xC0281E14);
            SpellIcons.draw(g, ability, MARGIN + 1, abilityY + 1, 9);
            int color = remaining > 0 ? 0xFF8C8070 : 0xFFECDCB6;
            g.drawString(font, label, MARGIN + 13, abilityY + 2, color, false);
            if (remaining > 0) {
                String seconds = (int) Math.ceil(remaining / 20.0) + "s";
                g.drawString(font, seconds, MARGIN + w + 3, abilityY + 2, 0xFFECDCB6, true);
            }
            abilityY -= 13;
        }

        Spell active = data.getActiveSpell();
        if (active != null) {
            g.drawString(font, active.getDisplayName(), MARGIN, nameY, 0xFFECDCB6, true);
        }

        float max = data.getMaxReserve();
        float ratio = max <= 0 ? 0 : Math.max(0, Math.min(1, data.getReserve() / max));
        g.fill(MARGIN, barY, MARGIN + totalWidth, barY + 4, BAR_BACK);
        g.fill(MARGIN + 1, barY + 1, MARGIN + 1 + Math.round((totalWidth - 2) * ratio), barY + 3, RESERVE);
    }

    private static java.util.List<Spell> abilities(MagicData data) {
        java.util.List<Spell> list = new java.util.ArrayList<>();
        for (Spell spell : com.gabriel.hogwartsmod.spell.Spells.all()) {
            if (spell.isAbility() && data.knows(spell)) {
                list.add(spell);
            }
        }
        return list;
    }

    private static String keyFor(Spell ability) {
        return ModKeys.DODGE.getTranslatedKeyMessage().getString();
    }
}
