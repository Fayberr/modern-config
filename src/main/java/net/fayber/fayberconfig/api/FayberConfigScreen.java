package net.fayber.fayberconfig.api;

import net.fayber.fayberconfig.gui.ConfigEntryList;
import net.fayber.fayberconfig.gui.FlatButton;
import net.fayber.fayberconfig.gui.GuiUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
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
 * A Fayber Config screen: dark rounded panel, scrollable card rows, Cancel/Save footer.
 *
 * <p>Values write through the entries' setters immediately (live preview: sliders and toggles
 * visibly change the game while the screen is open). When the screen opens, every entry
 * snapshots its current value; Cancel or ESC restores all snapshots, Save runs the consumer's
 * {@code onSave} (which persists the config) and returns to the parent screen.
 *
 * <p>Build one through {@link #builder}: categories first, then entries; each entry method
 * returns a handle for chaining {@code .tooltip(...)}.
 */
public class FayberConfigScreen extends Screen {
    private static final int HEADER_HEIGHT = 30;
    private static final int FOOTER_HEIGHT = 36;

    @Nullable
    private final Screen parent;
    @Nullable
    private final Runnable onSave;
    private final List<ConfigEntry> entries;

    private ConfigEntryList list;
    private HeaderAndFooterLayout layout;
    private boolean closed = false;

    FayberConfigScreen(Component title, @Nullable Screen parent, @Nullable Runnable onSave, List<ConfigEntry> entries) {
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

        this.layout = new HeaderAndFooterLayout(this);
        this.layout.setHeaderHeight(HEADER_HEIGHT);
        this.layout.setFooterHeight(FOOTER_HEIGHT);

        List<ConfigEntryList.Row> rows = new ArrayList<>();
        for (ConfigEntry entry : this.entries) {
            rows.add(entry.createRow());
        }
        this.list = new ConfigEntryList(this.minecraft, this.width, this.layout.getContentHeight(),
                this.layout.getHeaderHeight(), rows);
        this.layout.addToContents(this.list);
        this.layout.arrangeElements();
        this.list.updateSize(this.height, this.layout);
        this.addRenderableWidget(this.list);

        // Footer buttons get fixed bounds (the layout would stretch single footer children).
        int buttonY = this.height - FOOTER_HEIGHT + 8;
        this.addRenderableWidget(new FlatButton(this.width / 2 - 155, buttonY, 150, 20,
                Component.literal("Cancel"), this::onClose));
        this.addRenderableWidget(new FlatButton(this.width / 2 + 5, buttonY, 150, 20,
                Component.literal("Save"), this::saveAndClose));
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
        // The rounded panel must be extracted BEFORE super (which extracts the widgets), because
        // within one stratum extraction order == draw order.
        int top = HEADER_HEIGHT;
        int bottom = this.height - FOOTER_HEIGHT;
        int panelH = bottom - top;
        GuiUtil.fillRound(gfx, this.width / 2 - 210, top - 6, 420, panelH + 10, 5, GuiUtil.PANEL_BORDER);
        GuiUtil.fillRound(gfx, this.width / 2 - 209, top - 5, 418, panelH + 8, 4, GuiUtil.PANEL);
        gfx.centeredText(this.font, this.title, this.width / 2, 10, GuiUtil.TEXT);

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
