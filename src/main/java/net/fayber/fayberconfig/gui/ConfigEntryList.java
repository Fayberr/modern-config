package net.fayber.fayberconfig.gui;

import net.fayber.faybergui.list.CardList;
import net.fayber.faybergui.render.Theme;
import net.fayber.faybergui.render.Ui;
import net.fayber.faybergui.widget.Dropdown;
import net.fayber.faybergui.widget.StyledSlider;
import net.fayber.faybergui.widget.TextField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

/**
 * The scrollable body of a Fayber Config screen. Since 1.1.0 the scrolling mechanics (one rounded
 * card per row, momentum wheel, sub-pixel row motion, slim scrollbar) live in the embedded
 * {@code fayber-gui} library ({@link CardList}); this class is the thin fayberconfig shell that
 * adds the three config-specific row types: {@link HeaderRow}, {@link WidgetRow} and
 * {@link SliderRow}.
 *
 * <p>Rows are {@link CardList.Row}s whose real child widgets are hit-tested/focused through the
 * entry's {@code children()} dispatch, exactly like the vanilla KeyBindsList pattern:
 * {@code extractContent} repositions children from the row's content coords (scroll/resize-safe)
 * and then calls their final {@code extractRenderState}.
 */
public class ConfigEntryList extends CardList {
    // Re-exported so row code (and source compatibility with 1.0.x) can use the bare names.
    public static final int CARD_HEIGHT = CardList.CARD_HEIGHT;
    public static final int ROW_GAP = CardList.ROW_GAP;
    public static final int ROW_HEIGHT = CardList.ROW_HEIGHT;
    public static final int CARD_PADDING = CardList.CARD_PADDING;

    public ConfigEntryList(Minecraft mc, int width, int height, int y0, int rowWidth, List<Row> rows) {
        super(mc, width, height, y0, rowWidth);
        for (Row row : rows) {
            this.addEntry(row);
        }
    }

    @Override
    public ConfigEntryList theme(Theme theme) {
        super.theme(theme);
        return this;
    }

    /**
     * The reset-to-default button an option row can carry: applies the entry's declared default,
     * shown only while the current value differs from it. The entry supplies the pre-built
     * button and the visibility check (see {@code ConfigEntry.resetOf}).
     */
    public record Reset(AbstractWidget button, BooleanSupplier visible) {
        public boolean shown() {
            return this.visible.getAsBoolean();
        }
    }

    /**
     * Base row: card drawing and theme access come from {@link CardList.Row}, plus tab
     * visibility. A hidden row has zero height, no children and draws nothing, so the list lays
     * out, scrolls and hit-tests as if it were absent: 26.1's {@code AbstractSelectionList}
     * calls {@code Entry.getHeight()} on every reposition, and {@code setScrollAmount}
     * repositions, which is the re-layout trigger after switching tabs.
     */
    public abstract static class Row extends CardList.Row {
        private boolean visible = true;

        public final void setVisible(boolean visible) {
            this.visible = visible;
        }

        public final boolean isVisible() {
            return this.visible;
        }

        /**
         * The dropdown this row hosts, if any. The screen attaches its PopupHost after building
         * the rows, so a dropdown's menu floats above every card instead of being painted over
         * by the rows extracted after this one.
         */
        @Nullable
        public Dropdown dropdown() {
            return null;
        }

        /** The row's interactive children; subclasses implement this instead of {@link #children()}. */
        protected abstract List<? extends GuiEventListener> visibleChildren();

        @Override
        public final List<? extends GuiEventListener> children() {
            return this.visible ? this.visibleChildren() : List.of();
        }

        @Override
        public int getHeight() {
            return this.visible ? super.getHeight() : 0;
        }
    }

    /** Category title row: a small muted all-caps label, no card. */
    public static class HeaderRow extends Row {
        private final Component title;

        public HeaderRow(Component title) {
            this.title = Ui.uiBold(title.getString().toUpperCase(Locale.ROOT));
        }

        @Override
        protected List<? extends GuiEventListener> visibleChildren() {
            return List.of();
        }

        @Override
        public void extractContent(GuiGraphicsExtractor gfx, int mouseX, int mouseY, boolean hovered, float partialTick) {
            if (!this.isVisible()) {
                return;
            }
            int y = this.cardY() + CARD_HEIGHT - Ui.font().lineHeight - 4;
            Ui.text(gfx, this.title, this.getX() + 2, y, this.theme().textMuted);
        }
    }

    /**
     * Static wrapped paragraph (documentation text, Cloth's description entries): no card, no
     * interaction, secondary text colour. The row grows with the wrapped line count, which works
     * because 26.1's {@code AbstractSelectionList} re-asks {@code Entry.getHeight()} on every
     * reposition (the same mechanism collapses hidden tab rows to 0). The wrap width follows the
     * row width, so a note re-flows on resize.
     */
    public static class NoteRow extends Row {
        private static final int V_PADDING = 8;
        private final Component text;
        private int wrappedAt = -1;
        private List<FormattedCharSequence> lines = List.of();

        public NoteRow(Component text) {
            this.text = text;
        }

        /** Re-wraps when the available width changed; cached otherwise. */
        private void wrap() {
            int width = Math.max(24, this.getWidth() - CARD_PADDING);
            if (this.wrappedAt != width) {
                this.wrappedAt = width;
                this.lines = Ui.font().split(this.text, width);
            }
        }

        @Override
        public int getHeight() {
            if (!this.isVisible()) {
                return 0;
            }
            if (this.getWidth() > 0) {
                this.wrap();
            }
            return 2 * V_PADDING + this.lines.size() * Ui.font().lineHeight;
        }

        @Override
        protected List<? extends GuiEventListener> visibleChildren() {
            return List.of();
        }

        @Override
        public void extractContent(GuiGraphicsExtractor gfx, int mouseX, int mouseY, boolean hovered, float partialTick) {
            if (!this.isVisible()) {
                return;
            }
            this.wrap();
            int y = this.cardY() + V_PADDING;
            for (FormattedCharSequence line : this.lines) {
                gfx.text(Ui.font(), line, this.getX() + 2, y, this.theme().textSecondary, false);
                y += Ui.font().lineHeight;
            }
        }
    }

    /**
     * Row with a label on the left and one interactive widget on the right (pill toggle, flat
     * button, edit box). Draws the row card itself, then the child. Optionally a reset button,
     * which sits between the label and the widget and appears only while the value differs from
     * the declared default.
     */
    public static class WidgetRow extends Row {
        private final Component label;
        private final AbstractWidget widget;
        @Nullable
        private final Reset reset;

        public WidgetRow(Component label, Component tooltip, AbstractWidget widget) {
            this(label, tooltip, widget, null);
        }

        public WidgetRow(Component label, Component tooltip, AbstractWidget widget, @Nullable Reset reset) {
            this.label = Ui.ui(label);
            this.widget = widget;
            this.reset = reset;
            if (tooltip != null) {
                widget.setTooltip(Tooltip.create(Ui.ui(tooltip)));
            }
        }

        @Override
        @Nullable
        public Dropdown dropdown() {
            return this.widget instanceof Dropdown dropdown ? dropdown : null;
        }

        @Override
        protected List<? extends GuiEventListener> visibleChildren() {
            if (this.reset == null || !this.reset.shown()) {
                return List.of(this.widget);
            }
            return List.of(this.widget, this.reset.button());
        }

        @Override
        public void extractContent(GuiGraphicsExtractor gfx, int mouseX, int mouseY, boolean hovered, float partialTick) {
            if (!this.isVisible()) {
                return;
            }
            this.drawRowCard(gfx, hovered);
            Ui.text(gfx, this.label, this.getX() + CARD_PADDING, this.textY(), this.theme().text);
            this.widget.setPosition(
                    this.getX() + this.getWidth() - this.widget.getWidth() - CARD_PADDING,
                    this.cardY() + (CARD_HEIGHT - this.widget.getHeight()) / 2);
            this.widget.extractRenderState(gfx, mouseX, mouseY, partialTick);
            if (this.reset != null && this.reset.shown()) {
                AbstractWidget button = this.reset.button();
                button.setPosition(this.widget.getX() - 6 - button.getWidth(),
                        this.widget.getY() + (this.widget.getHeight() - button.getHeight()) / 2);
                button.extractRenderState(gfx, mouseX, mouseY, partialTick);
            }
        }
    }

    /**
     * Row with a label above a taller interactive widget (multi-line editors). The card is as
     * tall as its contents, which works because 26.1's {@code AbstractSelectionList} consults
     * {@code Entry.getHeight()} per entry when positioning, hit-testing and computing scroll
     * bounds, so the override is honoured everywhere without touching the list itself.
     */
    public static class TallWidgetRow extends Row {
        private static final float CARD_RADIUS = 6.0f;
        private static final int TOP_PAD = 8;
        private static final int LABEL_GAP = 6;
        private static final int BOTTOM_PAD = 10;

        private final Component label;
        private final AbstractWidget widget;
        @Nullable
        private final Reset reset;
        private final int height;

        public TallWidgetRow(Component label, Component tooltip, AbstractWidget widget) {
            this(label, tooltip, widget, null);
        }

        public TallWidgetRow(Component label, Component tooltip, AbstractWidget widget, @Nullable Reset reset) {
            this.label = Ui.ui(label);
            this.widget = widget;
            this.reset = reset;
            if (tooltip != null) {
                widget.setTooltip(Tooltip.create(Ui.ui(tooltip)));
            }
            this.height = TOP_PAD + Ui.font().lineHeight + LABEL_GAP + widget.getHeight() + BOTTOM_PAD;
        }

        @Override
        public int getHeight() {
            return this.isVisible() ? this.height : 0;
        }

        @Override
        protected List<? extends GuiEventListener> visibleChildren() {
            if (this.reset == null || !this.reset.shown()) {
                return List.of(this.widget);
            }
            return List.of(this.widget, this.reset.button());
        }

        @Override
        public void extractContent(GuiGraphicsExtractor gfx, int mouseX, int mouseY, boolean hovered, float partialTick) {
            if (!this.isVisible()) {
                return;
            }
            Ui.roundRectBorder(gfx, this.getX(), this.cardY(), this.getWidth(), this.height, CARD_RADIUS,
                    hovered ? this.theme().cardHover : this.theme().card,
                    hovered ? this.theme().cardBorderHover : this.theme().cardBorder, 1.0f);
            Ui.text(gfx, this.label, this.getX() + CARD_PADDING, this.cardY() + TOP_PAD, this.theme().text);
            this.widget.setPosition(this.getX() + CARD_PADDING,
                    this.cardY() + TOP_PAD + Ui.font().lineHeight + LABEL_GAP);
            this.widget.setWidth(this.getWidth() - 2 * CARD_PADDING);
            this.widget.extractRenderState(gfx, mouseX, mouseY, partialTick);
            if (this.reset != null && this.reset.shown()) {
                AbstractWidget button = this.reset.button();
                button.setPosition(this.getX() + this.getWidth() - button.getWidth() - CARD_PADDING,
                        this.cardY() + TOP_PAD - (button.getHeight() - Ui.font().lineHeight) / 2);
                button.extractRenderState(gfx, mouseX, mouseY, partialTick);
            }
        }
    }

    /**
     * Row whose single slider spans the full card: the slider draws label, snapped value, track
     * and knob itself. With a reset button present the slider gives up a strip at the right edge
     * for it.
     */
    public static class SliderRow extends Row {
        /** Card right-edge inset of the reset button, plus its gap to the slider. */
        private static final int RESET_ROOM = 24;

        private final StyledSlider slider;
        @Nullable
        private final Reset reset;

        public SliderRow(StyledSlider slider) {
            this(slider, null);
        }

        public SliderRow(StyledSlider slider, @Nullable Reset reset) {
            this.slider = slider;
            this.reset = reset;
        }

        @Override
        protected List<? extends GuiEventListener> visibleChildren() {
            if (this.reset == null || !this.reset.shown()) {
                return List.of(this.slider);
            }
            return List.of(this.slider, this.reset.button());
        }

        @Override
        public void extractContent(GuiGraphicsExtractor gfx, int mouseX, int mouseY, boolean hovered, float partialTick) {
            if (!this.isVisible()) {
                return;
            }
            this.drawRowCard(gfx, hovered);
            this.slider.setPosition(this.getX(), this.cardY());
            this.slider.setWidth(this.getWidth() - (this.reset != null ? RESET_ROOM : 0));
            this.slider.extractRenderState(gfx, mouseX, mouseY, partialTick);
            if (this.reset != null && this.reset.shown()) {
                AbstractWidget button = this.reset.button();
                button.setPosition(this.getX() + this.getWidth() - button.getWidth() - 4,
                        this.cardY() + (CARD_HEIGHT - button.getHeight()) / 2);
                button.extractRenderState(gfx, mouseX, mouseY, partialTick);
            }
        }
    }

    /**
     * Row for a colour option: label on the left, a live colour swatch and the hex text field on
     * the right. The swatch is pure decoration drawn from the entry's getter every frame, so it
     * tracks the last valid value while the hex text is being typed; the field is the row's only
     * interactive child.
     */
    public static class ColorRow extends Row {
        private static final int SWATCH_SIZE = 14;
        private static final int SWATCH_GAP = 6;

        private final Component label;
        private final TextField field;
        private final IntSupplier color;
        @Nullable
        private final Reset reset;

        public ColorRow(Component label, Component tooltip, TextField field, IntSupplier color,
                        @Nullable Reset reset) {
            this.label = Ui.ui(label);
            this.field = field;
            this.color = color;
            this.reset = reset;
            if (tooltip != null) {
                field.setTooltip(Tooltip.create(Ui.ui(tooltip)));
            }
        }

        @Override
        protected List<? extends GuiEventListener> visibleChildren() {
            if (this.reset == null || !this.reset.shown()) {
                return List.of(this.field);
            }
            return List.of(this.field, this.reset.button());
        }

        @Override
        public void extractContent(GuiGraphicsExtractor gfx, int mouseX, int mouseY, boolean hovered, float partialTick) {
            if (!this.isVisible()) {
                return;
            }
            this.drawRowCard(gfx, hovered);
            Ui.text(gfx, this.label, this.getX() + CARD_PADDING, this.textY(), this.theme().text);
            this.field.setPosition(
                    this.getX() + this.getWidth() - this.field.getWidth() - CARD_PADDING,
                    this.cardY() + (CARD_HEIGHT - this.field.getHeight()) / 2);
            this.field.extractRenderState(gfx, mouseX, mouseY, partialTick);
            int swatchX = this.field.getX() - SWATCH_GAP - SWATCH_SIZE;
            int swatchY = this.cardY() + (CARD_HEIGHT - SWATCH_SIZE) / 2;
            Ui.roundRectBorder(gfx, swatchX, swatchY, SWATCH_SIZE, SWATCH_SIZE, 4.0f,
                    this.color.getAsInt(), this.theme().cardBorderHover, 1.0f);
            if (this.reset != null && this.reset.shown()) {
                AbstractWidget button = this.reset.button();
                button.setPosition(swatchX - 6 - button.getWidth(),
                        this.cardY() + (CARD_HEIGHT - button.getHeight()) / 2);
                button.extractRenderState(gfx, mouseX, mouseY, partialTick);
            }
        }
    }
}
