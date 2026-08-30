package net.fayber.fayberconfig.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * iOS-style pill toggle: rounded track (cyan when on, dark gray when off) with a white knob that
 * sits right when on and left when off. Reads its state through the entry's getter every frame,
 * so external changes show immediately, and writes through on press (live preview).
 */
public class PillToggleWidget extends AbstractButton {
    private static final int TRACK_W = 40;
    private static final int TRACK_H = 16;
    private static final int KNOB = 12;

    private final Supplier<Boolean> getter;
    private final Consumer<Boolean> setter;

    public PillToggleWidget(int x, int y, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        super(x, y, TRACK_W, TRACK_H, Component.empty());
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        this.setter.accept(!this.getter.get());
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        boolean on = Boolean.TRUE.equals(this.getter.get());
        int track = on ? GuiUtil.ACCENT : GuiUtil.OFF_TRACK;
        GuiUtil.fillRound(gfx, this.getX(), this.getY(), this.getWidth(), this.getHeight(), this.getHeight() / 2, track);
        int knobX = on
                ? this.getX() + this.getWidth() - KNOB - 2
                : this.getX() + 2;
        int knobY = this.getY() + (TRACK_H - KNOB) / 2;
        GuiUtil.fillRound(gfx, knobX, knobY, KNOB, KNOB, KNOB / 2, GuiUtil.TEXT);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
