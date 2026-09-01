package net.fayber.modernconfig.gui;

import net.fayber.moderngui.list.CardList;
import net.fayber.moderngui.render.Theme;
import net.fayber.moderngui.render.Ui;
import net.fayber.moderngui.widget.Badge;
import net.fayber.moderngui.widget.Dropdown;
import net.fayber.moderngui.widget.StyledSlider;
import net.fayber.moderngui.widget.TextField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * The scrollable body of a Modern Config screen. Since 1.1.0 the scrolling mechanics (one rounded
 * card per row, momentum wheel, sub-pixel row motion, slim scrollbar) live in the embedded
 * {@code modern-gui} library ({@link CardList}); this class is the thin modernconfig shell that
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
     * Builder-side decorations an option row can carry: the restart badge, an enable condition
     * (the row renders dimmed and ignores input while it is false) and a per-entry error (red
     * border, message shown in the tooltip). None of these change how a value is written,
     * snapshotted or restored; the screen attaches one state per row after building them.
     */
    public record OptionState(boolean restart, @Nullable BooleanSupplier condition,
                              @Nullable Supplier<@Nullable String> errorSupplier) {
        /** The unmodified state every entry starts with. */
        public static final OptionState DEFAULT = new OptionState(false, null, null);

        /** False while a condition is present and reports false. */
        public boolean enabled() {
            return this.condition == null || this.condition.getAsBoolean();
        }

        /** The current error message, or null. Evaluated on every read, so keep it cheap. */
        @Nullable
        public String error() {
            return this.errorSupplier == null ? null : this.errorSupplier.get();
        }
    }

    /**
     * Base row: card drawing and theme access come from {@link CardList.Row}, plus tab
     * visibility and the per-option decorations. A hidden row has zero height, no children and
     * draws nothing, so the list lays out, scrolls and hit-tests as if it were absent: 26.1's
     * {@code AbstractSelectionList} calls {@code Entry.getHeight()} on every reposition, and
     * {@code setScrollAmount} repositions, which is the re-layout trigger after switching tabs.
     */
    public abstract static class Row extends CardList.Row {
        /** Height of the Restart badge chip. */
        protected static final int BADGE_HEIGHT = 16;
        /** Same corner radius CardList draws row cards with. */
        private static final float CARD_RADIUS = 6.0f;
        /** Border colour while the option's error supplier reports a message. */
        static final int ERROR_BORDER = TextField.ERROR_COLOR;

        private boolean visible = true;
        @Nullable
        private OptionState state;
        @Nullable
        private Badge restartBadge;
        /** Last error message seen by {@link #syncTooltip}, so the swap only runs on change. */
        @Nullable
        private String lastError;
        @Nullable
        private Component entryTooltip;

        public final void setVisible(boolean visible) {
            this.visible = visible;
        }

        public final boolean isVisible() {
            return this.visible;
        }

        /** Applies the Builder's per-option decorations (restart badge, condition, error). */
        public void optionState(@Nullable OptionState state) {
            this.state = state;
            this.restartBadge = state != null && state.restart()
                    ? Badge.warning(0, 0, Component.translatable("modernconfig.restart"))
                    : null;
        }

        /** False while the option's condition says otherwise: the row renders dimmed and inert. */
        protected final boolean optionEnabled() {
            return this.state == null || this.state.enabled();
        }

        /** The option's current error message, or null. */
        @Nullable
        protected final String optionError() {
            return this.state == null ? null : this.state.error();
        }

        /** The entry's own tooltip, remembered so the error message can replace it temporarily. */
        protected final void entryTooltip(@Nullable Component tooltip) {
            this.entryTooltip = tooltip;
        }

        /**
         * The widget the option's tooltip rides on. Only option rows have one; header and note
         * rows never reach the tooltip swap, so null is fine there.
         */
        @Nullable
        protected AbstractWidget tooltipWidget() {
            return null;
        }

        /**
         * Points the tooltip widget's tooltip at the error message while one is present, and back
         * at the entry's tooltip otherwise. Called every extract; the change check keeps the
         * vanilla Tooltip allocation off the steady-state path.
         */
        protected final void syncTooltip() {
            String error = this.optionError();
            if (Objects.equals(error, this.lastError)) {
                return;
            }
            this.lastError = error;
            AbstractWidget widget = this.tooltipWidget();
            if (widget == null) {
                return;
            }
            if (error != null && !error.isEmpty()) {
                widget.setTooltip(Tooltip.create(Ui.ui(error)));
            } else if (this.entryTooltip != null) {
                widget.setTooltip(Tooltip.create(Ui.ui(this.entryTooltip)));
            } else {
                widget.setTooltip(null);
            }
        }

        /**
         * Draws the Restart badge after the label text when the option declares one, as long as
         * the chip fits between the label and the option's widget area.
         */
        protected final void drawRestartBadge(GuiGraphicsExtractor gfx, int mouseX, int mouseY,
                                              float partialTick, int labelRight, int rightLimit, int y) {
            if (this.restartBadge == null) {
                return;
            }
            int x = labelRight + 6;
            if (x + this.restartBadge.getWidth() > rightLimit) {
                return;
            }
            this.restartBadge.setPosition(x, y);
            this.restartBadge.extractRenderState(gfx, mouseX, mouseY, partialTick);
        }

        /** Vertical centre of the restart badge inside a standard-height card. */
        protected final int badgeY() {
            return this.cardY() + (CARD_HEIGHT - BADGE_HEIGHT) / 2;
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
            // An option whose condition is false takes no input at all, reset button included;
            // extractContent dims the card contents to match.
            return this.visible && this.optionEnabled() ? this.visibleChildren() : List.of();
        }

        // children() gates mouse input, but the vanilla container routes keyPressed/charTyped to
        // the focused child without checking children(), so a row that loses its condition while
        // holding keyboard focus would still take keys. Gate the keyboard entry points too.

        @Override
        public boolean keyPressed(KeyEvent event) {
            if (!this.optionEnabled()) {
                return false;
            }
            return super.keyPressed(event);
        }

        @Override
        public boolean keyReleased(KeyEvent event) {
            if (!this.optionEnabled()) {
                return false;
            }
            return super.keyReleased(event);
        }

        @Override
        public boolean charTyped(CharacterEvent event) {
            if (!this.optionEnabled()) {
                return false;
            }
            return super.charTyped(event);
        }

        @Override
        public int getHeight() {
            return this.visible ? super.getHeight() : 0;
        }

        /** Row card with the border switched to the error colour while the option reports one. */
        protected final void drawRowCard(GuiGraphicsExtractor gfx, boolean hovered, boolean error) {
            if (!error) {
                this.drawRowCard(gfx, hovered);
                return;
            }
            Ui.roundRectBorder(gfx, this.getX(), this.cardY(), this.getWidth(), CARD_HEIGHT, CARD_RADIUS,
                    hovered ? this.theme().cardHover : this.theme().card, ERROR_BORDER, 1.0f);
        }

        /** Label colour: dimmed while the option's condition is false, matching the slider. */
        protected final int labelColor() {
            return this.optionEnabled() ? this.theme().text : Theme.darken(this.theme().text, 0.45f);
        }
    }

    /** Category title row: a small muted all-caps label, no card. */
    public static class HeaderRow extends Row {
        private final Component title;

        public HeaderRow(Component title) {
            this.title = title;
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
            // Upper-cased and font-wrapped per frame so a translatable title tracks language
            // changes; headers are rare rows, so the small per-frame cost is fine.
            Component drawn = Ui.uiBold(Component.literal(this.title.getString().toUpperCase(Locale.ROOT)));
            int y = this.cardY() + CARD_HEIGHT - Ui.font().lineHeight - 4;
            Ui.text(gfx, drawn, this.getX() + 2, y, this.theme().textMuted);
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
     * the declared default, and a Restart badge after the label.
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
            this.entryTooltip(tooltip);
            this.syncTooltip();
        }

        @Override
        protected AbstractWidget tooltipWidget() {
            return this.widget;
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
            this.drawRowCard(gfx, hovered, this.optionError() != null);
            Ui.text(gfx, this.label, this.getX() + CARD_PADDING, this.textY(), this.labelColor());
            this.widget.setPosition(
                    this.getX() + this.getWidth() - this.widget.getWidth() - CARD_PADDING,
                    this.cardY() + (CARD_HEIGHT - this.widget.getHeight()) / 2);
            this.widget.active = this.optionEnabled();
            this.syncTooltip();
            // Reset and badge positions are settled before either extracts, so the badge's fit
            // check sees fresh geometry even on the first frame after the reset button appears.
            boolean resetShown = this.reset != null && this.reset.shown();
            if (resetShown) {
                AbstractWidget button = this.reset.button();
                button.setPosition(this.widget.getX() - 6 - button.getWidth(),
                        this.widget.getY() + (this.widget.getHeight() - button.getHeight()) / 2);
            }
            int rightLimit = resetShown ? this.reset.button().getX() - 6 : this.widget.getX() - 6;
            this.drawRestartBadge(gfx, mouseX, mouseY, partialTick,
                    this.getX() + CARD_PADDING + Ui.font().width(this.label), rightLimit, this.badgeY());
            this.widget.extractRenderState(gfx, mouseX, mouseY, partialTick);
            if (resetShown) {
                this.reset.button().extractRenderState(gfx, mouseX, mouseY, partialTick);
            }
        }
    }

    /**
     * Row with a label above a taller interactive widget (list editors). The card is as tall as
     * its contents, which works because 26.1's {@code AbstractSelectionList} consults
     * {@code Entry.getHeight()} per entry when positioning, hit-testing and computing scroll
     * bounds, so the override is honoured everywhere without touching the list itself. The height
     * is re-derived every time it is asked, so an editor whose row count changes (add/remove)
     * grows and shrinks the card live.
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

        public TallWidgetRow(Component label, Component tooltip, AbstractWidget widget) {
            this(label, tooltip, widget, null);
        }

        public TallWidgetRow(Component label, Component tooltip, AbstractWidget widget, @Nullable Reset reset) {
            this.label = Ui.ui(label);
            this.widget = widget;
            this.reset = reset;
            this.entryTooltip(tooltip);
            this.syncTooltip();
        }

        @Override
        protected AbstractWidget tooltipWidget() {
            return this.widget;
        }

        @Override
        public int getHeight() {
            if (!this.isVisible()) {
                return 0;
            }
            return TOP_PAD + Ui.font().lineHeight + LABEL_GAP + this.widget.getHeight() + BOTTOM_PAD;
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
            boolean error = this.optionError() != null;
            int border = error ? ERROR_BORDER
                    : (hovered ? this.theme().cardBorderHover : this.theme().cardBorder);
            Ui.roundRectBorder(gfx, this.getX(), this.cardY(), this.getWidth(), this.getHeight(), CARD_RADIUS,
                    hovered ? this.theme().cardHover : this.theme().card, border, 1.0f);
            int labelY = this.cardY() + TOP_PAD;
            Ui.text(gfx, this.label, this.getX() + CARD_PADDING, labelY, this.labelColor());
            this.widget.setPosition(this.getX() + CARD_PADDING,
                    this.cardY() + TOP_PAD + Ui.font().lineHeight + LABEL_GAP);
            this.widget.setWidth(this.getWidth() - 2 * CARD_PADDING);
            this.widget.active = this.optionEnabled();
            this.syncTooltip();
            boolean resetShown = this.reset != null && this.reset.shown();
            if (resetShown) {
                AbstractWidget button = this.reset.button();
                button.setPosition(this.getX() + this.getWidth() - button.getWidth() - CARD_PADDING,
                        this.cardY() + TOP_PAD - (button.getHeight() - Ui.font().lineHeight) / 2);
            }
            // The badge rides the label line; the reset button owns the top right when present.
            int rightLimit = resetShown
                    ? this.reset.button().getX() - 6
                    : this.getX() + this.getWidth() - CARD_PADDING;
            this.drawRestartBadge(gfx, mouseX, mouseY, partialTick,
                    this.getX() + CARD_PADDING + Ui.font().width(this.label), rightLimit,
                    labelY + (Ui.font().lineHeight - BADGE_HEIGHT) / 2 + 1);
            this.widget.extractRenderState(gfx, mouseX, mouseY, partialTick);
            if (resetShown) {
                this.reset.button().extractRenderState(gfx, mouseX, mouseY, partialTick);
            }
        }
    }

    /**
     * Row whose single slider spans the full card: the slider draws label, snapped value, track
     * and knob itself. With a reset button present the slider gives up a strip at the right edge
     * for it. The Restart badge sits after the slider's own label, with room reserved for the
     * value text at the right end of the label line.
     */
    public static class SliderRow extends Row {
        /** Card right-edge inset of the reset button, plus its gap to the slider. */
        private static final int RESET_ROOM = 24;
        /** Horizontal room kept clear for the slider's value text when placing the badge. */
        private static final int VALUE_ROOM = 48;

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
        protected AbstractWidget tooltipWidget() {
            return this.slider;
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
            this.drawRowCard(gfx, hovered, this.optionError() != null);
            this.slider.setPosition(this.getX(), this.cardY());
            this.slider.setWidth(this.getWidth() - (this.reset != null ? RESET_ROOM : 0));
            this.slider.active = this.optionEnabled();
            this.syncTooltip();
            boolean resetShown = this.reset != null && this.reset.shown();
            if (resetShown) {
                AbstractWidget button = this.reset.button();
                button.setPosition(this.getX() + this.getWidth() - button.getWidth() - 4,
                        this.cardY() + (CARD_HEIGHT - button.getHeight()) / 2);
            }
            int rightLimit = resetShown
                    ? this.reset.button().getX() - 6
                    : this.getX() + this.slider.getWidth() - 12 - VALUE_ROOM;
            this.drawRestartBadge(gfx, mouseX, mouseY, partialTick,
                    this.slider.labelRight(), rightLimit, this.badgeY());
            this.slider.extractRenderState(gfx, mouseX, mouseY, partialTick);
            if (resetShown) {
                this.reset.button().extractRenderState(gfx, mouseX, mouseY, partialTick);
            }
        }
    }

    /**
     * Row for a colour option: label on the left, then a colour swatch and the hex text field on
     * the right. The swatch reads the entry's getter every frame (so it tracks the last valid
     * value while the hex text is being typed) and, once the screen attaches a
     * {@link #pickerOpener}, clicking it opens the colour picker modal. Writes always go through
     * the hex field (its responder writes through), so picker drags preview live and the text
     * stays the one source of truth.
     */
    public static class ColorRow extends Row {
        private static final int SWATCH_SIZE = 14;
        private static final int SWATCH_GAP = 6;

        private final Component label;
        private final TextField field;
        private final ColorSwatch swatch;
        private final IntSupplier color;
        /** Consumer for a picked colour; the entry formats it into the hex field. */
        private final IntConsumer applyColor;
        @Nullable
        private final Reset reset;
        @Nullable
        private Runnable pickerOpener;

        public ColorRow(Component label, Component tooltip, TextField field, IntSupplier color,
                        IntConsumer applyColor, @Nullable Reset reset) {
            this.label = Ui.ui(label);
            this.field = field;
            this.color = color;
            this.applyColor = applyColor;
            this.swatch = new ColorSwatch(0, 0, SWATCH_SIZE, color, () -> {
                if (this.pickerOpener != null) {
                    this.pickerOpener.run();
                }
            }).theme(Theme.dark()).tooltip(Component.translatable("modernconfig.pick_color"));
            this.reset = reset;
            this.entryTooltip(tooltip);
            this.syncTooltip();
        }

        /** The entry's current colour, captured when the picker opens (the cancel-restore point). */
        public int currentColor() {
            return this.color.getAsInt();
        }

        /** Writes a picked colour through the hex field (its responder applies it). */
        public void applyColor(int argb) {
            this.applyColor.accept(argb);
        }

        /**
         * Attaches the open-the-picker action; the screen wires this after building the rows
         * because the popup host does not exist yet when entries create their rows.
         */
        public void pickerOpener(@Nullable Runnable pickerOpener) {
            this.pickerOpener = pickerOpener;
        }

        /** The row label, so the picker modal can carry the option's name. */
        public Component rowLabel() {
            return this.label;
        }

        @Override
        protected AbstractWidget tooltipWidget() {
            return this.field;
        }

        @Override
        protected List<? extends GuiEventListener> visibleChildren() {
            List<AbstractWidget> children = new ArrayList<>(3);
            if (this.reset != null && this.reset.shown()) {
                children.add(this.reset.button());
            }
            children.add(this.swatch);
            children.add(this.field);
            return children;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor gfx, int mouseX, int mouseY, boolean hovered, float partialTick) {
            if (!this.isVisible()) {
                return;
            }
            this.drawRowCard(gfx, hovered, this.optionError() != null);
            Ui.text(gfx, this.label, this.getX() + CARD_PADDING, this.textY(), this.labelColor());
            this.field.setPosition(
                    this.getX() + this.getWidth() - this.field.getWidth() - CARD_PADDING,
                    this.cardY() + (CARD_HEIGHT - this.field.getHeight()) / 2);
            this.field.active = this.optionEnabled();
            this.syncTooltip();
            boolean resetShown = this.reset != null && this.reset.shown();
            int swatchX = this.field.getX() - SWATCH_GAP - SWATCH_SIZE;
            int swatchY = this.cardY() + (CARD_HEIGHT - SWATCH_SIZE) / 2;
            this.swatch.setPosition(swatchX, swatchY);
            this.swatch.active = this.optionEnabled();
            if (resetShown) {
                AbstractWidget button = this.reset.button();
                button.setPosition(swatchX - 6 - button.getWidth(),
                        this.cardY() + (CARD_HEIGHT - button.getHeight()) / 2);
            }
            int rightLimit = resetShown ? this.reset.button().getX() - 6 : swatchX - 6;
            this.drawRestartBadge(gfx, mouseX, mouseY, partialTick,
                    this.getX() + CARD_PADDING + Ui.font().width(this.label), rightLimit, this.badgeY());
            this.field.extractRenderState(gfx, mouseX, mouseY, partialTick);
            this.swatch.extractRenderState(gfx, mouseX, mouseY, partialTick);
            if (resetShown) {
                this.reset.button().extractRenderState(gfx, mouseX, mouseY, partialTick);
            }
        }
    }
}
