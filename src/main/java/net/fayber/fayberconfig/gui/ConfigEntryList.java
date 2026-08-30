package net.fayber.fayberconfig.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/**
 * The scrollable body of a Fayber Config screen: one rounded card per option, a slim rounded
 * scrollbar, and no vanilla list chrome (background, separators and the sprite scrollbar are all
 * replaced).
 *
 * <p>Rows are {@link ContainerObjectSelectionList.Entry}s whose real child widgets are
 * hit-tested/focused through the entry's {@code children()} dispatch, exactly like the vanilla
 * KeyBindsList pattern: {@code extractContent} repositions children from the row's content coords
 * (scroll/resize-safe) and then calls their final {@code extractRenderState}.
 *
 * <p>Mouse-wheel scrolling is animated (vanilla's is instant): the wheel only moves a target,
 * and every frame the actual scroll amount eases toward it. Authoritative scroll changes that
 * are not wheel-driven (scrollbar drag, keyboard, scrollToEntry) bypass the animation and stay
 * instant, so dragging the thumb keeps up with the pointer 1:1.
 */
public class ConfigEntryList extends ContainerObjectSelectionList<ConfigEntryList.Row> {
    /** Card height; the row pitch adds the gap on top of this. */
    public static final int CARD_HEIGHT = 34;
    /** Vertical gap between cards. */
    public static final int ROW_GAP = 4;
    public static final int ROW_HEIGHT = CARD_HEIGHT + ROW_GAP;
    /** Horizontal padding inside a card. */
    public static final int CARD_PADDING = 12;
    private static final float CARD_RADIUS = 6.0f;
    /** Per-frame ease of the actual scroll amount toward the wheel target. */
    private static final double SCROLL_EASE = 0.45;
    /** Below this many pixels of remaining travel the animation just settles. */
    private static final double SCROLL_SETTLE = 0.5;

    private final int rowWidth;

    /** Where the wheel wants the scroll to be; the actual amount eases toward this. */
    private double scrollTarget;
    /** The scroll amount last applied; eased toward {@link #scrollTarget} once per frame. */
    private double scrollEased;
    /** Set while the eased amount is being applied, so {@link #setScrollAmount} passes through. */
    private boolean applyingEasedScroll;

    public ConfigEntryList(Minecraft mc, int width, int height, int y0, int rowWidth, List<Row> rows) {
        super(mc, width, height, y0, ROW_HEIGHT);
        this.rowWidth = rowWidth;
        for (Row row : rows) {
            this.addEntry(row);
        }
    }

    @Override
    public int getRowWidth() {
        return Math.min(this.rowWidth, this.getWidth() - 24);
    }

    /**
     * Wheel input only moves the animation target; the actual scroll amount chases it in
     * {@link #extractWidgetRenderState}. Sign and rate match vanilla's own wheel handling
     * ({@code scrollAmount - yDelta * scrollRate()}).
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double xDelta, double yDelta) {
        if (!this.scrollable()) {
            return super.mouseScrolled(mouseX, mouseY, xDelta, yDelta);
        }
        this.scrollTarget = Math.clamp(this.scrollTarget - yDelta * this.scrollRate(),
                0.0, this.maxScrollAmount());
        return true;
    }

    /**
     * Every scroll change that is not ours (scrollbar drag, keyboard, scrollToEntry) is
     * authoritative: snap the animation state to it and apply it instantly.
     */
    @Override
    public void setScrollAmount(double amount) {
        if (this.applyingEasedScroll) {
            super.setScrollAmount(amount);
            return;
        }
        this.scrollEased = this.scrollTarget = amount;
        super.setScrollAmount(amount);
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        // Ease before super so the rows, scrollbar and separators all extract from the freshly
        // eased amount in the same frame.
        this.advanceSmoothScroll();
        super.extractWidgetRenderState(gfx, mouseX, mouseY, partialTick);
    }

    /** Moves the eased scroll amount toward the wheel target and applies it to the list. */
    private void advanceSmoothScroll() {
        if (this.scrollable()) {
            this.scrollTarget = Math.clamp(this.scrollTarget, 0.0, this.maxScrollAmount());
            if (Math.abs(this.scrollTarget - this.scrollEased) <= SCROLL_SETTLE) {
                this.scrollEased = this.scrollTarget;
            } else {
                this.scrollEased += (this.scrollTarget - this.scrollEased) * SCROLL_EASE;
            }
        } else {
            this.scrollTarget = 0.0;
            this.scrollEased = 0.0;
        }
        if (this.scrollEased != this.scrollAmount()) {
            this.applyingEasedScroll = true;
            super.setScrollAmount(this.scrollEased);
            this.applyingEasedScroll = false;
            // The super setter clamps; keep our state in sync with what actually applied.
            this.scrollEased = this.scrollTarget = this.scrollAmount();
        }
    }

    @Override
    protected void extractListBackground(GuiGraphicsExtractor gfx) {
        // The screen draws its own rounded panel.
    }

    @Override
    protected void extractListSeparators(GuiGraphicsExtractor gfx) {
        // No vanilla row separators.
    }

    @Override
    protected void extractScrollbar(GuiGraphicsExtractor gfx, int mouseX, int mouseY) {
        if (!this.scrollable()) {
            return;
        }
        // Slim rounded scrollbar instead of the vanilla sprite one.
        float w = 4.0f;
        float x = this.scrollBarX() + (this.scrollbarWidth() - w) / 2.0f;
        boolean hovered = mouseX >= x - 3.0f && mouseX <= x + w + 3.0f
                && mouseY >= this.getY() && mouseY <= this.getY() + this.getHeight();
        Ui.pill(gfx, x, this.getY() + 2, w, this.getHeight() - 4, GuiUtil.CARD);
        Ui.pill(gfx, x, this.scrollBarY(), w, this.scrollerHeight(),
                hovered ? GuiUtil.SCROLLBAR_HOVER : GuiUtil.SCROLLBAR);
    }

    /** Base row: draws its own rounded card; interactive children live in {@link #children()}. */
    public abstract static class Row extends ContainerObjectSelectionList.Entry<Row> {
        public abstract List<? extends GuiEventListener> children();

        @Override
        @SuppressWarnings("unchecked")
        public List<? extends NarratableEntry> narratables() {
            // Row children are always AbstractWidgets, which are GuiEventListener AND
            // NarratableEntry, so the same list serves both dispatch paths.
            return (List<? extends NarratableEntry>) (List<?>) this.children();
        }

        /** Card top edge; the row pitch includes the gap, the card does not. */
        protected int cardY() {
            return this.getY();
        }

        protected void drawRowCard(GuiGraphicsExtractor gfx, boolean hovered) {
            Ui.roundRectBorder(gfx, this.getX(), this.cardY(), this.getWidth(), CARD_HEIGHT, CARD_RADIUS,
                    hovered ? GuiUtil.CARD_HOVER : GuiUtil.CARD,
                    hovered ? GuiUtil.CARD_BORDER_HOVER : GuiUtil.CARD_BORDER, 1.0f);
        }

        /** Baseline for a single line of text vertically centred in the card. */
        protected int textY() {
            return this.cardY() + (CARD_HEIGHT - Ui.font().lineHeight) / 2 + 1;
        }
    }

    /** Category title row: a small muted all-caps label, no card. */
    public static class HeaderRow extends Row {
        private final Component title;

        public HeaderRow(Component title) {
            this.title = Ui.uiBold(title.getString().toUpperCase(Locale.ROOT));
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of();
        }

        @Override
        public void extractContent(GuiGraphicsExtractor gfx, int mouseX, int mouseY, boolean hovered, float partialTick) {
            int y = this.cardY() + CARD_HEIGHT - Ui.font().lineHeight - 4;
            Ui.text(gfx, this.title, this.getX() + 2, y, GuiUtil.TEXT_MUTED);
        }
    }

    /**
     * Row with a label on the left and one interactive widget on the right (pill toggle, flat
     * button, edit box). Draws the row card itself, then the child.
     */
    public static class WidgetRow extends Row {
        private final Component label;
        private final AbstractWidget widget;

        public WidgetRow(Component label, Component tooltip, AbstractWidget widget) {
            this.label = Ui.ui(label);
            this.widget = widget;
            if (tooltip != null) {
                widget.setTooltip(Tooltip.create(Ui.ui(tooltip)));
            }
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(this.widget);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor gfx, int mouseX, int mouseY, boolean hovered, float partialTick) {
            this.drawRowCard(gfx, hovered);
            Ui.text(gfx, this.label, this.getX() + CARD_PADDING, this.textY(), GuiUtil.TEXT);
            this.widget.setPosition(
                    this.getX() + this.getWidth() - this.widget.getWidth() - CARD_PADDING,
                    this.cardY() + (CARD_HEIGHT - this.widget.getHeight()) / 2);
            this.widget.extractRenderState(gfx, mouseX, mouseY, partialTick);
        }
    }

    /**
     * Row whose single slider spans the full card: the slider draws label, snapped value, track
     * and knob itself.
     */
    public static class SliderRow extends Row {
        private final StyledSlider slider;

        public SliderRow(StyledSlider slider) {
            this.slider = slider;
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(this.slider);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor gfx, int mouseX, int mouseY, boolean hovered, float partialTick) {
            this.drawRowCard(gfx, hovered);
            this.slider.setPosition(this.getX(), this.cardY());
            this.slider.setWidth(this.getWidth());
            this.slider.extractRenderState(gfx, mouseX, mouseY, partialTick);
        }
    }
}
