package net.fayber.fayberconfig.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Pill toggle: a capsule track (near-white when on, dark when off) with a round knob that slides
 * between the ends. Reads its state through the entry's getter every frame, so external changes
 * show immediately, and writes through on press (live preview).
 *
 * <p>The knob eases towards its target each frame, which is what makes a toggle feel like an app
 * control rather than a checkbox.
 */
public class PillToggleWidget extends AbstractButton {
    private static final int TRACK_W = 34;
    private static final int TRACK_H = 18;
    private static final float KNOB_INSET = 2.5f;

    private final Supplier<Boolean> getter;
    private final Consumer<Boolean> setter;

    /** 0 = off position, 1 = on position; negative means "not initialised yet". */
    private float knobPos = -1.0f;

    public PillToggleWidget(int x, int y, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        super(x, y, TRACK_W, TRACK_H, Component.empty());
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        this.setter.accept(!Boolean.TRUE.equals(this.getter.get()));
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        boolean on = Boolean.TRUE.equals(this.getter.get());
        float target = on ? 1.0f : 0.0f;
        if (this.knobPos < 0.0f) {
            this.knobPos = target; // opening the screen should not animate every toggle
        } else {
            this.knobPos += (target - this.knobPos) * 0.35f;
        }

        boolean hovered = this.isHoveredOrFocused();
        int track = on
                ? (hovered ? GuiUtil.ACCENT_HOVER : GuiUtil.ACCENT)
                : (hovered ? GuiUtil.SLIDER_TRACK_HOVER : GuiUtil.OFF_TRACK);
        Ui.pill(gfx, this.getX(), this.getY(), this.getWidth(), this.getHeight(), track);

        // Dark knob on the light on-track, light knob on the dark off-track.
        int knob = on ? GuiUtil.TEXT_ON_ACCENT : GuiUtil.TEXT;
        float knobRadius = this.getHeight() / 2.0f - KNOB_INSET;
        float left = this.getX() + KNOB_INSET + knobRadius;
        float right = this.getX() + this.getWidth() - KNOB_INSET - knobRadius;
        float cx = left + (right - left) * this.knobPos;
        Ui.circle(gfx, cx, this.getY() + this.getHeight() / 2.0f, knobRadius, knob);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
