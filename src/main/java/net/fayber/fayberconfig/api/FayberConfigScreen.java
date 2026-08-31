package net.fayber.fayberconfig.api;

import net.fayber.faybergui.render.Theme;
import net.fayber.faybergui.render.Ui;
import net.fayber.faybergui.widget.FlatButton;
import net.fayber.fayberconfig.gui.ConfigEntryList;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

/**
 * A Fayber Config screen: a dimmed backdrop with a centred column of option cards floating
 * directly on it (no panel box), a bold title on top and a Cancel/Save footer of separate card
 * buttons. The same language as the waypoint screens, so mods using the library look like one
 * product.
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
 * to line up with the centred column, not the window: the list is positioned explicitly with
 * {@code updateSizeAndPosition(width, height, x, y)}, and the list is wider than the cards so the
 * scrollbar sits in a gutter outside the visible column
 * ({@code scrollBarX() == getRowRight() + scrollbarWidth() + 2}).
 */
public class FayberConfigScreen extends Screen {
    /** Width of the card column in GUI pixels, clamped to the window. */
    private static final int CONTENT_WIDTH = 380;
    /** Title baseline from the top of the screen. */
    private static final int TITLE_Y = 18;
    /** Top of the scrolling card column. */
    private static final int LIST_TOP = 40;
    /** Footer button height, and the margin below the buttons. */
    private static final int BUTTON_HEIGHT = 28;
    private static final int FOOTER_MARGIN = 14;
    /** Below this window height (GUI pixels) the roomy metrics do not fit and compact ones kick in. */
    private static final int COMPACT_BELOW = 300;
    /** Room to each side of the cards for the scrollbar. */
    private static final int SCROLL_GUTTER = 14;

    @Nullable
    private final Screen parent;
    @Nullable
    private final Runnable onSave;
    private final List<ConfigEntry> entries;

    /** The fayber-gui palette; the default is the same neutral ramp 1.0.x drew with. */
    private final Theme theme = Theme.dark();

    private ConfigEntryList list;
    private boolean closed = false;

    private int columnX;
    private int columnW;
    private int titleY;

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

    /** The scrollable body; exposed so the preview workbench can hold it at scroll offsets. */
    public ConfigEntryList entryList() {
        return this.list;
    }

    @Override
    protected void init() {
        for (ConfigEntry entry : this.entries) {
            entry.snapshot();
        }

        // 720p at GUI scale 3 is only 240 GUI pixels tall: tighter footer, closer list.
        boolean compact = this.height < COMPACT_BELOW;
        this.titleY = compact ? 14 : TITLE_Y;
        int listTop = compact ? 32 : LIST_TOP;
        int buttonH = compact ? 24 : BUTTON_HEIGHT;
        int footerMargin = compact ? 8 : FOOTER_MARGIN;
        int buttonGap = compact ? 6 : 8;

        this.columnW = Math.min(CONTENT_WIDTH, Math.max(220, this.width - 32));
        this.columnX = (this.width - this.columnW) / 2;

        List<ConfigEntryList.Row> rows = new ArrayList<>();
        for (ConfigEntry entry : this.entries) {
            rows.add(entry.createRow());
        }

        int buttonY = this.height - buttonH - footerMargin;
        int listH = Math.max(ConfigEntryList.ROW_HEIGHT, buttonY - 12 - listTop);

        // The list is wider than the visible card column: the cards are centred in it and the
        // scrollbar lives in the gutter outside the column.
        int listX = this.columnX - SCROLL_GUTTER;
        int listW = this.columnW + 2 * SCROLL_GUTTER;

        this.list = new ConfigEntryList(this.minecraft, listW, listH, listTop, this.columnW, rows);
        this.list.updateSizeAndPosition(listW, listH, listX, listTop);
        this.addRenderableWidget(this.list);

        int buttonW = (this.columnW - buttonGap) / 2;
        this.addRenderableWidget(new FlatButton(this.columnX, buttonY, buttonW, buttonH,
                Component.literal("Cancel"), this::onClose, FlatButton.Style.GHOST));
        this.addRenderableWidget(new FlatButton(this.columnX + this.columnW - buttonW, buttonY, buttonW, buttonH,
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
        // Backdrop and title must be extracted BEFORE super (which extracts the widgets), because
        // within one stratum extraction order is draw order.
        gfx.fill(0, 0, this.width, this.height, this.theme.scrim);

        Ui.text(gfx, Ui.uiBold(this.title), this.columnX, this.titleY, this.theme.text);

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

        /**
         * An option with a fixed set of values, shown as a cycle card that steps through them on
         * click (left forward, right backward). Typical for enums; {@code namer} turns a value
         * into its display text.
         */
        public <T> Builder cycle(String label, Supplier<T> getter, Consumer<T> setter, T[] values,
                                 Function<T, String> namer) {
            this.entries.add(new ConfigEntry.Cycle<>(label, getter, setter, values, namer));
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
