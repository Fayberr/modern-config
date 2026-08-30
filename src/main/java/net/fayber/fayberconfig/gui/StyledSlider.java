package net.fayber.fayberconfig.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

/**
 * Styled ranged slider for the dark-card rows. Spans the whole row card: label on the left,
 * snapped value on the right, and a thin 2px track with a rounded cyan knob below.
 *
 * <p>All vanilla interaction is kept (mouse drag, arrow keys while focused); only the drawing is
 * replaced. The 0..1 {@code value} is mapped to the range and snapped to {@code step} in
 * {@link #applyValue()}, and the knob is re-snapped to the written value so it always sits on a
 * reachable step. Values write through immediately (live preview).
 */
public abstract class StyledSlider extends AbstractSliderButton {
    private static final int KNOB = 11;
    private static final int TRACK_THICKNESS = 2;

    protected final Component label;
    protected final double min;
    protected final double max;
    protected final double step;

    protected StyledSlider(int x, int y, int w, Component label, double min, double max, double step, double initial) {
        super(x, y, w, 26, label, to01(min, max, initial));
        this.label = label;
        this.min = min;
        this.max = max;
        this.step = Math.max(step, 1e-9);
        this.updateMessage();
    }

    /** Positions the label/value line and the track inside the 26px row height. */
    private static final int LABEL_Y = 3;
    private static final int TRACK_CENTER_Y = 18;

    protected static double to01(double min, double max, double v) {
        if (max <= min) {
            return 0.0;
        }
        return Math.clamp((v - min) / (max - min), 0.0, 1.0);
    }

    /** Snaps the current knob position to the nearest step and returns the written value. */
    protected double snappedValue() {
        double raw = this.min + this.value * (this.max - this.min);
        double snapped = Math.round(raw / this.step) * this.step;
        return Math.clamp(snapped, this.min, this.max);
    }

    /** Re-snaps the knob to the written value so it always sits on a reachable step. */
    protected void resnapKnob() {
        this.value = to01(this.min, this.max, this.snappedValue());
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        var font = Minecraft.getInstance().font;
        boolean hovered = this.isHoveredOrFocused();

        // Label left, value right, on the card's top line.
        gfx.text(font, this.label, this.getX() + 2, this.getY() + LABEL_Y, GuiUtil.TEXT_SECONDARY);
        String valueText = this.format(this.snappedValue());
        gfx.text(font, valueText, this.getX() + this.getWidth() - 2 - font.width(valueText), this.getY() + LABEL_Y, hovered ? GuiUtil.ACCENT : GuiUtil.TEXT);

        // Thin track + rounded knob.
        int centerY = this.getY() + TRACK_CENTER_Y;
        int trackX0 = this.getX() + 2;
        int trackX1 = this.getX() + this.getWidth() - 2;
        gfx.fill(trackX0, centerY - TRACK_THICKNESS / 2, trackX1, centerY + TRACK_THICKNESS / 2,
                hovered ? GuiUtil.SLIDER_TRACK_HOVER : GuiUtil.SLIDER_TRACK);
        int knobX = trackX0 + (int) Math.round(this.value * (trackX1 - trackX0 - KNOB));
        GuiUtil.fillRound(gfx, knobX, centerY - KNOB / 2, KNOB, KNOB, 3, GuiUtil.ACCENT);
    }

    protected abstract String format(double value);

    /** Formats the value without trailing ".0" for integral steps. */
    protected static String trim(double v) {
        if (v == Math.floor(v)) {
            return String.valueOf((long) v);
        }
        return String.valueOf(v);
    }
}
