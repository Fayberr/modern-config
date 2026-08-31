package net.fayber.fayberconfig.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The Fayber Config palette, plus thin wrappers over {@link Ui} for the two shapes every widget
 * needs. Shapes are drawn by {@link Ui} at physical-pixel resolution through an anti-aliased
 * corner shader, so nothing here lands on the chunky GUI-pixel grid.
 *
 * <p>The palette is a genuinely neutral dark ramp, identical to the waypoint screens' Theme: the
 * surface greys have equal R/G/B so nothing reads as tinted, and there is deliberately
 * <b>no accent colour</b>. Emphasis is carried by lightness alone: a near-white fill marks the
 * confirming button and the "on" state of a toggle, which keeps the screen quiet and lets the
 * mod's own content be the only real colour on it. (The {@code ACCENT} names are kept for source
 * compatibility, but they now hold that neutral light fill.)
 */
public final class GuiUtil {
    private GuiUtil() {
    }

    /** Dim laid over the world/menu behind the cards. */
    public static final int SCRIM = 0xC6000000;

    public static final int CARD = 0xFF1A1A1A;
    public static final int CARD_HOVER = 0xFF222222;
    public static final int CARD_BORDER = 0xFF262626;
    public static final int CARD_BORDER_HOVER = 0xFF3A3A3A;

    public static final int TEXT = 0xFFF0F0F0;
    public static final int TEXT_SECONDARY = 0xFFA3A3A3;
    public static final int TEXT_MUTED = 0xFF6E6E6E;
    /** Dark label for text sitting on top of the light {@link #ACCENT} fill. */
    public static final int TEXT_ON_ACCENT = 0xFF121212;

    /** Near-white fill for the confirming button and the "on" state of a toggle. */
    public static final int ACCENT = 0xFFE6E6E6;
    public static final int ACCENT_HOVER = 0xFFFFFFFF;

    public static final int OFF_TRACK = 0xFF3A3A3A;
    public static final int SLIDER_TRACK = 0xFF2E2E2E;
    public static final int SLIDER_TRACK_HOVER = 0xFF3A3A3A;
    /** Filled part of a slider track, mid grey so the white knob reads against it. */
    public static final int SLIDER_FILL = 0xFF7A7A7A;
    public static final int SLIDER_FILL_HOVER = 0xFF9A9A9A;
    public static final int SCROLLBAR = 0xFF3A3A3A;
    public static final int SCROLLBAR_HOVER = 0xFF4D4D4D;

    /** Filled rounded rectangle. */
    public static void fillRound(GuiGraphicsExtractor gfx, float x, float y, float w, float h, float radius, int color) {
        Ui.roundRect(gfx, x, y, w, h, radius, color);
    }

    /** Rounded card with a hairline border. */
    public static void fillRoundCard(GuiGraphicsExtractor gfx, float x, float y, float w, float h, float radius,
                                     int borderColor, int fillColor) {
        Ui.roundRectBorder(gfx, x, y, w, h, radius, fillColor, borderColor, 1.0f);
    }
}
