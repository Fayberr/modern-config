package net.fayber.fayberconfig.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The Fayber Config palette, plus thin wrappers over {@link Ui} for the two shapes every widget
 * needs. Shapes are drawn by {@link Ui} at physical-pixel resolution through an anti-aliased
 * corner shader, so nothing here lands on the chunky GUI-pixel grid.
 *
 * <p>The palette is a dark neutral ramp with a single cyan accent, matching the Waypoints in-world
 * label card so the whole mod family reads as one product.
 */
public final class GuiUtil {
    private GuiUtil() {
    }

    /** Dim laid over the world/menu behind the panel. */
    public static final int SCRIM = 0xA6000000;

    public static final int PANEL = 0xFF0E1116;
    public static final int PANEL_BORDER = 0xFF232932;

    public static final int CARD = 0xFF161A21;
    public static final int CARD_HOVER = 0xFF1D222B;
    public static final int CARD_BORDER = 0xFF232932;
    public static final int CARD_BORDER_HOVER = 0xFF2E3643;

    public static final int TEXT = 0xFFF2F4F7;
    public static final int TEXT_SECONDARY = 0xFF98A2B0;
    public static final int TEXT_MUTED = 0xFF6C7684;
    public static final int TEXT_ON_ACCENT = 0xFF06222A;

    public static final int ACCENT = 0xFF00E5FF;
    public static final int ACCENT_HOVER = 0xFF5CF0FF;

    public static final int OFF_TRACK = 0xFF2B323C;
    public static final int SLIDER_TRACK = 0xFF262C36;
    public static final int SLIDER_TRACK_HOVER = 0xFF2E3642;
    public static final int SCROLLBAR = 0xFF2B323C;
    public static final int SCROLLBAR_HOVER = 0xFF3B4453;

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
