package net.fayber.fayberconfig.api;

import net.fayber.fayberconfig.gui.ConfigEntryList;
import net.fayber.fayberconfig.gui.FlatButton;
import net.fayber.fayberconfig.gui.GuiUtil;
import net.fayber.fayberconfig.gui.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

/**
 * A Fayber Config screen: a centred dark panel with a title bar, a scrolling column of option
 * cards, and a Cancel/Save footer.
 *
 * <p>Values write through the entries' setters immediately (live preview: sliders and toggles
 * visibly change the game while the screen is open). When the screen opens, every entry snapshots
 * its current value; Cancel or ESC restores all snapshots, Save runs the consumer's
 * {@code onSave} (which persists the config) and returns to the parent screen.
 *
 * <p>Build one through {@link #builder}: categories first, then entries; {@code .tooltip(...)}
 * attaches to the entry added just before it.
 *
 * <p>Layout is done by hand rather than with {@code HeaderAndFooterLayout} because everything has
 * to line up inside the panel, not the window: the list is positioned explicitly with
 * {@code updateSizeAndPosition(width, height, x, y)}, and the card width leaves a gutter on the
 * right wide enough for the scrollbar ({@code scrollBarX() == getRowRight() + scrollbarWidth() + 2}).
 */
public class FayberConfigScreen extends Screen {
    /** Panel width in GUI pixels, clamped to the window. */
    private static final int PANEL_WIDTH = 420;
    /** Space above and below the panel. */
    private static final int PANEL_MARGIN_Y = 24;
    /** Title bar height inside the panel. */
    private static final int HEADER_HEIGHT = 46;
    /** Button bar height inside the panel. */
    private static final int FOOTER_HEIGHT = 50;
    /** Panel edge to card edge. */
    private static final int CONTENT_INSET = 18;
    /** Room to the right of the cards for the scrollbar. */
    private static final int SCROLL_GUTTER = 14;
    private static final float PANEL_RADIUS = 10.0f;

    @Nullable
    private final Screen parent;
    @Nullable
    private final Runnable onSave;
    private final List<ConfigEntry> entries;

    private ConfigEntryList list;
    private boolean closed = false;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    FayberConfigScreen(Component title, @Nullable Screen parent, @Nullable Runnable onSave, List<ConfigEntry> entries) {
        // Kept raw: the Inter variant depends on the GUI scale, so it is applied at draw time.
        super(title);
        this.parent = parent;
        this.onSave = onSave;
        this.entries = entries;
    }

    public static Builder builder(Component title, @Nullable Screen parent, @Nullable Runnable onSave) {
        return new Builder(title, parent, onSave);
    }

    @Override
    protected void init() {
        for (ConfigEntry entry : this.entries) {
            entry.snapshot();
        }

        this.panelW = Math.min(PANEL_WIDTH, Math.max(200, this.width - 2 * PANEL_MARGIN_Y));
        this.panelH = Math.max(140, this.height - 2 * PANEL_MARGIN_Y);
        this.panelX = (this.width - this.panelW) / 2;
        this.panelY = (this.height - this.panelH) / 2;

        List<ConfigEntryList.Row> rows = new ArrayList<>();
        for (ConfigEntry entry : this.entries) {
            rows.add(entry.createRow());
        }

        // The list spans the panel interior; the cards are narrower so the scrollbar has a gutter.
        int listX = this.panelX + CONTENT_INSET - SCROLL_GUTTER;
        int listW = this.panelW - 2 * (CONTENT_INSET - SCROLL_GUTTER);
        int listY = this.panelY + HEADER_HEIGHT;
        int listH = this.panelH - HEADER_HEIGHT - FOOTER_HEIGHT;
        int cardWidth = listW - 2 * SCROLL_GUTTER;

        this.list = new ConfigEntryList(this.minecraft, listW, listH, listY, cardWidth, rows);
        this.list.updateSizeAndPosition(listW, listH, listX, listY);
        this.addRenderableWidget(this.list);

        int buttonW = 92;
        int buttonH = 24;
        int buttonY = this.panelY + this.panelH - FOOTER_HEIGHT + (FOOTER_HEIGHT - buttonH) / 2;
        int saveX = this.panelX + this.panelW - CONTENT_INSET - buttonW;
        int cancelX = saveX - 8 - buttonW;
        this.addRenderableWidget(new FlatButton(cancelX, buttonY, buttonW, buttonH,
                Component.literal("Cancel"), this::onClose, FlatButton.Style.GHOST));
        this.addRenderableWidget(new FlatButton(saveX, buttonY, buttonW, buttonH,
                Component.literal("Save"), this::saveAndClose, FlatButton.Style.PRIMARY));
    }

    private void saveAndClose() {
        this.closed = true;
        if (this.onSave != null) {
            this.onSave.run();
        }
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void onClose() {
        if (!this.closed) {
            // Cancel/ESC: revert every live-written value back to the snapshot.
            this.closed = true;
            for (ConfigEntry entry : this.entries) {
                entry.restore();
            }
        }
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        // Panel chrome must be extracted BEFORE super (which extracts the widgets), because within
        // one stratum extraction order is draw order.
        gfx.fill(0, 0, this.width, this.height, GuiUtil.SCRIM);

        Ui.shadow(gfx, this.panelX, this.panelY, this.panelW, this.panelH, PANEL_RADIUS, 6.0f, 4);
        Ui.roundRectBorder(gfx, this.panelX, this.panelY, this.panelW, this.panelH, PANEL_RADIUS,
                GuiUtil.PANEL, GuiUtil.PANEL_BORDER, 1.0f);

        Ui.text(gfx, Ui.uiBold(this.title), this.panelX + CONTENT_INSET,
                this.panelY + (HEADER_HEIGHT - Ui.font().lineHeight) / 2, GuiUtil.TEXT);

        // Hairlines separating the title bar and the button bar from the scrolling body.
        Ui.rect(gfx, this.panelX + 1, this.panelY + HEADER_HEIGHT, this.panelW - 2, 0.5f, GuiUtil.PANEL_BORDER);
        Ui.rect(gfx, this.panelX + 1, this.panelY + this.panelH - FOOTER_HEIGHT, this.panelW - 2, 0.5f,
                GuiUtil.PANEL_BORDER);

        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        // Config screens feel like overlays; do not pause singleplayer.
        return false;
    }

    /** Fluent builder: {@code FayberConfigScreen.builder(title, parent, onSave).category(...)...build()}. */
    public static final class Builder {
        private final Component title;
        @Nullable
        private final Screen parent;
        @Nullable
        private final Runnable onSave;
        private final List<ConfigEntry> entries = new ArrayList<>();

        private Builder(Component title, @Nullable Screen parent, @Nullable Runnable onSave) {
            this.title = title;
            this.parent = parent;
            this.onSave = onSave;
        }

        /** Starts a new category section (inserts a header row). */
        public Builder category(String name) {
            this.entries.add(new ConfigEntry.Header(name));
            return this;
        }

        /**
         * Attaches a tooltip to the most recently added entry. Kept as a separate builder step
         * so entry methods can return the Builder and chain fluently.
         */
        public Builder tooltip(String tooltip) {
            if (!this.entries.isEmpty()) {
                this.entries.get(this.entries.size() - 1).tooltip(tooltip);
            }
            return this;
        }

        public Builder bool(String label, Supplier<Boolean> getter, Consumer<Boolean> setter) {
            this.entries.add(new ConfigEntry.Bool(label, getter, setter));
            return this;
        }

        public Builder intSlider(String label, IntSupplier getter, IntConsumer setter, int min, int max, int step) {
            this.entries.add(new ConfigEntry.IntSlider(label, getter, setter, min, max, step));
            return this;
        }

        public Builder floatSlider(String label, Supplier<Float> getter, Consumer<Float> setter, float min, float max, float step) {
            this.entries.add(new ConfigEntry.FloatSlider(label, getter, setter, min, max, step));
            return this;
        }

        public Builder doubleSlider(String label, Supplier<Double> getter, Consumer<Double> setter, double min, double max, double step) {
            this.entries.add(new ConfigEntry.DoubleSlider(label, getter, setter, min, max, step));
            return this;
        }

        public Builder text(String label, Supplier<String> getter, Consumer<String> setter, int maxLength) {
            this.entries.add(new ConfigEntry.Text(label, getter, setter, maxLength));
            return this;
        }

        public Builder button(String label, Runnable onPress) {
            this.entries.add(new ConfigEntry.Button(label, onPress));
            return this;
        }

        public FayberConfigScreen build() {
            return new FayberConfigScreen(this.title, this.parent, this.onSave, List.copyOf(this.entries));
        }
    }
}
