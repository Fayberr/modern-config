package net.fayber.fayberconfig.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The Fayber Config palette, plus thin wrappers over {@link Ui} for the two shapes every widget
 * needs. Shapes are drawn by {@link Ui} at physical-pixel resolution through an anti-aliased
 * corner shader, so nothing here lands on the chunky GUI-pixel grid.
 *
 * <p>The palette is a genuinely neutral dark ramp: the surface greys have equal R/G/B so nothing
 * reads as tinted, and the only colour in the whole screen is one soft blue on the interactive
 * bits (toggles, slider fill, the confirming button).
 */
public final class GuiUtil {
    private GuiUtil() {
    }

    /** Dim laid over the world/menu behind the panel. */
    public static final int SCRIM = 0xB3000000;

    public static final int PANEL = 0xFF121212;
    public static final int PANEL_BORDER = 0xFF2A2A2A;

    public static final int CARD = 0xFF1A1A1A;
    public static final int CARD_HOVER = 0xFF222222;
    public static final int CARD_BORDER = 0xFF262626;
    public static final int CARD_BORDER_HOVER = 0xFF343434;

    public static final int TEXT = 0xFFF0F0F0;
    public static final int TEXT_SECONDARY = 0xFFA3A3A3;
    public static final int TEXT_MUTED = 0xFF6E6E6E;
    /** Dark label for text sitting on top of the accent fill. */
    public static final int TEXT_ON_ACCENT = 0xFF0F1319;

    public static final int ACCENT = 0xFF7AA2F7;
    public static final int ACCENT_HOVER = 0xFF98B7FF;

    public static final int OFF_TRACK = 0xFF3A3A3A;
    public static final int SLIDER_TRACK = 0xFF2E2E2E;
    public static final int SLIDER_TRACK_HOVER = 0xFF383838;
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
