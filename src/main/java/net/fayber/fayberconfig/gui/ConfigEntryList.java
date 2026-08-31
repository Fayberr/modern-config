package net.fayber.fayberconfig.gui;

import net.fayber.faybergui.list.CardList;
import net.fayber.faybergui.render.Theme;
import net.fayber.faybergui.render.Ui;
import net.fayber.faybergui.widget.StyledSlider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

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

    /** Base row: the card drawing and theme access come from {@link CardList.Row}. */
    public abstract static class Row extends CardList.Row {
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
            Ui.text(gfx, this.title, this.getX() + 2, y, this.theme().textMuted);
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
            Ui.text(gfx, this.label, this.getX() + CARD_PADDING, this.textY(), this.theme().text);
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
