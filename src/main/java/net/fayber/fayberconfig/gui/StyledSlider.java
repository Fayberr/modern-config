package net.fayber.fayberconfig.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

/**
 * Ranged slider filling a whole row card: label on the left, snapped value on the right, and a
 * thin capsule track below with the travelled part in the accent colour and a round knob.
 *
 * <p>All vanilla interaction is kept (mouse drag, arrow keys while focused); only the drawing is
 * replaced. The 0..1 {@code value} is mapped to the range and snapped to {@code step} in
 * {@link #applyValue()}, and the knob is re-snapped to the written value so it always sits on a
 * reachable step. Values write through immediately (live preview).
 */
public abstract class StyledSlider extends AbstractSliderButton {
    /** Row height this slider is laid out for. */
    public static final int HEIGHT = 34;

    private static final float KNOB_RADIUS = 5.5f;
    private static final float TRACK_THICKNESS = 3.0f;
    private static final int LABEL_Y = 6;
    private static final int TRACK_CENTER_Y = 25;
    private static final float SIDE_PADDING = 12.0f;

    protected final Component label;
    protected final double min;
    protected final double max;
    protected final double step;

    protected StyledSlider(int x, int y, int w, Component label, double min, double max, double step, double initial) {
        super(x, y, w, HEIGHT, Ui.ui(label), to01(min, max, initial));
        this.label = Ui.ui(label);
        this.min = min;
        this.max = max;
        this.step = Math.max(step, 1e-9);
        this.updateMessage();
    }

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
        boolean hovered = this.isHoveredOrFocused();

        // Label left, value right, on the card's top line.
        Ui.text(gfx, this.label, this.getX() + (int) SIDE_PADDING, this.getY() + LABEL_Y, GuiUtil.TEXT);
        Ui.textRight(gfx, Ui.ui(this.format(this.snappedValue())),
                this.getX() + this.getWidth() - (int) SIDE_PADDING, this.getY() + LABEL_Y,
                hovered ? GuiUtil.ACCENT : GuiUtil.TEXT_SECONDARY);

        // Capsule track: neutral remainder, accent up to the knob.
        float centerY = this.getY() + TRACK_CENTER_Y;
        float trackX = this.getX() + SIDE_PADDING;
        float trackW = this.getWidth() - SIDE_PADDING * 2.0f;
        float trackY = centerY - TRACK_THICKNESS / 2.0f;
        Ui.pill(gfx, trackX, trackY, trackW, TRACK_THICKNESS,
                hovered ? GuiUtil.SLIDER_TRACK_HOVER : GuiUtil.SLIDER_TRACK);

        float travel = trackW - KNOB_RADIUS * 2.0f;
        float knobCx = trackX + KNOB_RADIUS + (float) this.value * travel;
        if (knobCx > trackX + KNOB_RADIUS) {
            Ui.pill(gfx, trackX, trackY, knobCx - trackX, TRACK_THICKNESS, GuiUtil.ACCENT);
        }
        Ui.circle(gfx, knobCx, centerY, KNOB_RADIUS, hovered ? GuiUtil.TEXT : GuiUtil.ACCENT);
        if (hovered) {
            Ui.circle(gfx, knobCx, centerY, KNOB_RADIUS - 2.0f, GuiUtil.ACCENT);
        }
    }

    protected abstract String format(double value);

    /** Formats the value without a trailing ".0" for integral steps. */
    protected static String trim(double v) {
        if (v == Math.floor(v)) {
            return String.valueOf((long) v);
        }
        return String.valueOf(v);
    }
}
