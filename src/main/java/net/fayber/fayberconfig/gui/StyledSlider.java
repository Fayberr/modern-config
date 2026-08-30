package net.fayber.fayberconfig.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

/**
 * Ranged slider filling a whole row card: label on the left, snapped value on the right, and a
 * thin capsule track below with the travelled part in the accent colour and a round knob.
 *
 * <p>All vanilla interaction is kept (mouse drag, arrow keys while focused); the drawing and the
 * mouse-to-value mapping are replaced. The written value is ALWAYS snapped to {@code step}: while
 * dragging, the 0..1 {@code value} is driven from the raw mouse position and {@link #applyValue()}
 * writes the snapped value through immediately (live preview). The knob, however, is decoupled
 * from the written value: it follows the raw drag position 1:1 (so the motion is smooth instead of
 * hopping between steps), and glides the last bit onto the written step when the drag ends or
 * an arrow key steps, via the eased {@link #displayValue}. Arrow keys step exactly one
 * {@code step} (vanilla's own key handling moves by a screen-width-dependent fraction, which
 * would be wrong here), so the value still locks on the same reachable numbers as before.
 *
 * <p>The mouse mapping is replaced because vanilla assumes its own 8px handle: it maps the cursor
 * onto a knob trajectory spanning {@code [x + 4, x + width - 4]}, while the drawn knob travels
 * {@code [trackX + r, trackX + trackW - r]}. Left alone, clicks and drags land the knob up to
 * ~13px off the cursor at the ends of the track. {@link #onClick} and {@link #onDrag} therefore
 * map the cursor with the drawn geometry instead, so the knob sits exactly under it.
 */
public abstract class StyledSlider extends AbstractSliderButton {
    /** Row height this slider is laid out for. */
    public static final int HEIGHT = 34;

    private static final float KNOB_RADIUS = 5.5f;
    private static final float TRACK_THICKNESS = 3.0f;
    private static final int LABEL_Y = 6;
    private static final int TRACK_CENTER_Y = 25;
    private static final float SIDE_PADDING = 12.0f;
    /** Time-normalised knob glide speed (per second); 0.45/frame at 60fps equals ~36/s. */
    private static final double KNOB_SPEED = 36.0;
    /** Frame gap cap so a stall never teleports the glide. */
    private static final double MAX_FRAME_SECONDS = 0.1;

    protected final Component label;
    protected final double min;
    protected final double max;
    protected final double step;

    /** Knob position actually drawn; eased toward {@code value} except during a drag. */
    private double displayValue;
    /** True between mouse press and release: {@code value} is the raw mouse fraction then. */
    private boolean draggingKnob;
    /** Timestamp of the last drawn frame, for the time-normalised glide. */
    private long lastFrameMs = -1L;

    protected StyledSlider(int x, int y, int w, Component label, double min, double max, double step, double initial) {
        super(x, y, w, HEIGHT, Ui.ui(label), to01(min, max, initial));
        this.label = Ui.ui(label);
        this.min = min;
        this.max = max;
        this.step = Math.max(step, 1e-9);
        this.displayValue = to01(min, max, initial);
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

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        // Do NOT call super: it maps the cursor onto vanilla's own 8px handle trajectory
        // ((mouseX - getX() - 4) / (width - 8)), which disagrees with the drawn track and puts
        // the knob up to ~13px off the cursor at the track ends. Map with the drawn geometry.
        this.setValueFromCursor(event.x());
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double deltaX, double deltaY) {
        this.draggingKnob = true;
        this.setValueFromCursor(event.x());
    }

    /** Maps the cursor X onto the drawn knob trajectory so the knob sits exactly under it. */
    private void setValueFromCursor(double mouseX) {
        double trackX = this.getX() + SIDE_PADDING;
        double travel = this.getWidth() - SIDE_PADDING * 2.0 - KNOB_RADIUS * 2.0;
        if (travel <= 0.0) {
            return;
        }
        this.setValue(Math.clamp((mouseX - trackX - KNOB_RADIUS) / travel, 0.0, 1.0));
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        this.draggingKnob = false;
        // The written value was snapped during the drag; align the logical knob position with
        // it so the drawn knob glides onto the step instead of resting between two.
        this.value = to01(this.min, this.max, this.snappedValue());
        this.updateMessage();
        super.onRelease(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // Vanilla's key handling steps value by 1/(width-8), which depends on the widget width
        // and ignores our step. Replace it: one press = exactly one step on the written value.
        boolean left = event.isLeft();
        boolean right = event.isRight();
        if (!left && !right) {
            return super.keyPressed(event);
        }
        double next = Math.clamp(this.snappedValue() + (left ? -this.step : this.step), this.min, this.max);
        if (next != this.snappedValue()) {
            this.value = to01(this.min, this.max, next);
            this.applyValue();
            this.updateMessage();
        }
        return true;
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        // Raw 1:1 follow while dragging; glide onto the written step the rest of the time.
        // The glide is time-normalised so the feel is identical at any frame rate; the frame
        // gap is tracked on every frame so it is fresh when a drag ends.
        long now = Util.getMillis();
        double dt = this.lastFrameMs < 0
                ? 0.0
                : Math.min((now - this.lastFrameMs) / 1000.0, MAX_FRAME_SECONDS);
        this.lastFrameMs = now;
        if (this.draggingKnob) {
            this.displayValue = this.value;
        } else {
            this.displayValue += (this.value - this.displayValue) * (1.0 - Math.exp(-dt * KNOB_SPEED));
        }

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
        float knobCx = trackX + KNOB_RADIUS + (float) this.displayValue * travel;
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
