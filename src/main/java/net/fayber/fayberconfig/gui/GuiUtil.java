package net.fayber.fayberconfig.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Shared palette and drawing helpers for the Fayber Config dark-card aesthetic. Everything is
 * drawn with plain fills and text; no textures, sprites or custom render pipelines.
 *
 * <p>26.1's GuiGraphicsExtractor has no rounded-rect primitive, so {@link #fillRound} builds one
 * from stacked horizontal fill bands with per-row insets following a quarter-circle arc.
 */
public final class GuiUtil {
    private GuiUtil() {}

    // Palette. The panel/row colors intentionally match the Waypoints in-world label card
    // (near-black fill with a subtle lighter border) so the config UI feels like the mod family.
    public static final int PANEL = 0xFF12141B;
    public static final int PANEL_BORDER = 0xFF525A63;
    public static final int CARD = 0xFF1A1D26;
    public static final int CARD_HOVER = 0xFF232733;
    public static final int CARD_BORDER = 0xFF2A2F3A;
    public static final int CARD_BORDER_HOVER = 0xFF3A4150;
    public static final int TEXT = 0xFFFFFFFF;
    public static final int TEXT_SECONDARY = 0xFFA8B0BA;
    public static final int ACCENT = 0xFF00E5FF;
    public static final int OFF_TRACK = 0xFF3A3F4A;
    public static final int SLIDER_TRACK = 0xFF2A2F3A;
    public static final int SLIDER_TRACK_HOVER = 0xFF343B48;

    /**
     * Rounded rectangle via horizontal fill bands. Radius is clamped to half the shorter side.
     * Row insets follow the circle equation so corners read as a true arc, not a diagonal cut.
     */
    public static void fillRound(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int radius, int color) {
        int r = Math.min(radius, Math.min(w, h) / 2);
        if (r <= 0) {
            gfx.fill(x, y, x + w, y + h, color);
            return;
        }
        // Middle band (full width).
        gfx.fill(x, y + r, x + w, y + h - r, color);
        // Corner bands: one fill per row, inset per the arc.
        for (int i = 0; i < r; i++) {
            // Distance of this row's pixel center from the corner circle's center line.
            double dy = r - i - 0.5;
            int inset = r - (int) Math.floor(Math.sqrt(r * r - dy * dy));
            inset = Math.min(inset, r);
            gfx.fill(x + inset, y + i, x + w - inset, y + i + 1, color);          // top
            gfx.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, color);  // bottom
        }
    }

    /**
     * Card with a 1px rounded border ring: outer shape in borderColor, inner shape inset by 1px
     * in fillColor. The inner radius shrinks by 1 to keep the ring width even around corners.
     */
    public static void fillRoundCard(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int radius, int borderColor, int fillColor) {
        fillRound(gfx, x, y, w, h, radius, borderColor);
        fillRound(gfx, x + 1, y + 1, w - 2, h - 2, Math.max(0, radius - 1), fillColor);
    }
}
