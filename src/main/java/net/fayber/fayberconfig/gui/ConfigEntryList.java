package net.fayber.fayberconfig.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

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
 * <p>Mouse-wheel scrolling has momentum (vanilla's is instant): the wheel adds velocity, and the
 * scroll position coasts with an exponential decay, the way a website eases a wheel step to rest.
 * One notch travels {@code scrollRate()} pixels in total, but spread over a fraction of a second
 * of deceleration instead of snapping there; fast spins accumulate velocity (capped). The decay
 * is time-normalised, so the feel is identical at any frame rate. Authoritative scroll changes
 * that are not wheel-driven (scrollbar drag, keyboard, code) cancel the glide and stay instant.
 *
 * <p>Vanilla positions rows at {@code firstEntryY - (int) scrollAmount} and the scrollbar thumb
 * from an integer-division formula: both drop the fractional part of the scroll amount, which
 * makes any fractional scroll (this glide, or a trackpad) move content in whole-GUI-pixel
 * stair-steps. Rows are therefore drawn through {@link #extractItem} with the pose shifted back
 * by the dropped fraction (true sub-pixel motion; hit-testing keeps the int positions, the
 * difference is under one GUI pixel), and the thumb is drawn from the continuous formula.
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
    /** Exponential velocity decay of the wheel glide (per second); a notch coasts ~0.4s. */
    private static final double SCROLL_FRICTION = 10.0;
    /** Glide speed below which the coast has visibly ended and stops. */
    private static final double SCROLL_STOP = 6.0;
    /** Velocity cap so a fast spin does not launch the list off-screen. */
    private static final double SCROLL_MAX_SPEED = 4000.0;
    /** Frame gap cap so a stall never teleports the glide. */
    private static final double MAX_FRAME_SECONDS = 0.1;

    private final int rowWidth;

    /** Current glide velocity in GUI px/s; zero when the list is at rest. */
    private double glideVelocity;
    /** Timestamp of the last drawn frame, for the time-normalised decay. */
    private long lastFrameMs = -1L;

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
     * Wheel input adds glide velocity; the position coasts in {@link #advanceGlide}. Sign matches
     * vanilla's own wheel handling ({@code scrollAmount - yDelta * scrollRate()}), and one notch
     * travels {@code scrollRate()} pixels in total because v0 = distance * friction for an
     * exponential decay.
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double xDelta, double yDelta) {
        if (!this.scrollable()) {
            return super.mouseScrolled(mouseX, mouseY, xDelta, yDelta);
        }
        this.glideVelocity = Math.clamp(
                this.glideVelocity - yDelta * this.scrollRate() * SCROLL_FRICTION,
                -SCROLL_MAX_SPEED, SCROLL_MAX_SPEED);
        return true;
    }

    /**
     * GUI pixels per wheel notch. Vanilla's own rate is {@code entryHeight / 2} (bytecode-verified:
     * the list constructor passes that to the scrollbar settings), half a row, which reads as
     * sluggish on a sparse config screen. Two rows per notch feels like a modern app.
     */
    @Override
    protected double scrollRate() {
        return 2.0 * ROW_HEIGHT;
    }

    /**
     * Every scroll change that is not ours (scrollbar drag, keyboard, scrollToEntry) is
     * authoritative: cancel the glide and apply instantly, so the thumb keeps up with the pointer.
     */
    @Override
    public void setScrollAmount(double amount) {
        this.glideVelocity = 0.0;
        super.setScrollAmount(amount);
    }

    /**
     * Jumps to a scroll offset, cancelling any glide. Kept for the preview workbench, which pins
     * the list at exact offsets for screenshots.
     */
    public void smoothScrollTo(double target) {
        this.setScrollAmount(target);
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        // Coast before super so the rows, scrollbar and separators all extract from the freshly
        // advanced amount in the same frame.
        this.advanceGlide();
        super.extractWidgetRenderState(gfx, mouseX, mouseY, partialTick);
    }

    /** Advances the momentum glide and applies it to the list. */
    private void advanceGlide() {
        // Track the frame gap on every frame so it is fresh when a wheel event arrives after
        // a long idle period (a stale gap would teleport the glide on the first frame).
        long now = Util.getMillis();
        double dt = this.lastFrameMs < 0
                ? 0.0
                : Math.min((now - this.lastFrameMs) / 1000.0, MAX_FRAME_SECONDS);
        this.lastFrameMs = now;
        if (this.glideVelocity == 0.0 || !this.scrollable()) {
            return;
        }
        double scrolled = this.scrollAmount() + this.glideVelocity * dt;
        this.glideVelocity *= Math.exp(-dt * SCROLL_FRICTION);
        if (Math.abs(this.glideVelocity) < SCROLL_STOP) {
            this.glideVelocity = 0.0;
        }
        double clamped = Math.clamp(scrolled, 0.0, this.maxScrollAmount());
        if (clamped != scrolled) {
            // Reached an end of the list: stop dead instead of pressing against the edge.
            this.glideVelocity = 0.0;
        }
        // The super setter, not the override: the glide must not cancel itself.
        super.setScrollAmount(clamped);
    }

    /**
     * Draws each row shifted by the scroll fraction vanilla drops: rows sit at
     * {@code firstEntryY - (int) scrollAmount}, so the true (fractional) position is
     * {@code rowY - frac}. Without this the glide moves content in whole-GUI-pixel stair-steps
     * (3 screen px per GUI px at scale 3) instead of coasting smoothly.
     */
    @Override
    protected void extractItem(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick, Row entry) {
        double frac = this.scrollAmount() - Math.floor(this.scrollAmount());
        gfx.pose().pushMatrix();
        gfx.pose().translate(0.0f, (float) -frac);
        super.extractItem(gfx, mouseX, mouseY, partialTick, entry);
        gfx.pose().popMatrix();
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
        // Slim rounded scrollbar instead of the vanilla sprite one. The thumb is drawn from the
        // continuous position instead of vanilla's scrollBarY(), which truncates the scroll
        // amount to an int and integer-divides, so it stair-steps during the animation.
        float w = 4.0f;
        float x = this.scrollBarX() + (this.scrollbarWidth() - w) / 2.0f;
        boolean hovered = mouseX >= x - 3.0f && mouseX <= x + w + 3.0f
                && mouseY >= this.getY() && mouseY <= this.getY() + this.getHeight();
        float trackTop = this.getY() + 2.0f;
        float trackH = this.getHeight() - 4.0f;
        Ui.pill(gfx, x, trackTop, w, trackH, GuiUtil.CARD);
        float span = trackH - this.scrollerHeight();
        float thumbY = trackTop + (float) (this.scrollAmount() * span / this.maxScrollAmount());
        Ui.pill(gfx, x, thumbY, w, this.scrollerHeight(),
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
