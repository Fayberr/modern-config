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

/**
 * The scrollable body of a Fayber Config screen: uniform 26px rows, each drawn as its own rounded
 * dark card (hover brightens). Rows are {@link ContainerObjectSelectionList.Entry}s whose real
 * child widgets are hit-tested/focused through the entry's {@code children()} dispatch, exactly
 * like the vanilla KeyBindsList pattern: {@code extractContent} repositions children from the
 * row's content coords (scroll/resize-safe) and then calls their final
 * {@code extractRenderState}.
 *
 * <p>The vanilla list background and separators are overridden to no-ops; the screen draws its
 * own rounded panel behind the whole list.
 */
public class ConfigEntryList extends ContainerObjectSelectionList<ConfigEntryList.Row> {
    public static final int ROW_HEIGHT = 26;

    public ConfigEntryList(Minecraft mc, int width, int height, int y0, List<Row> rows) {
        super(mc, width, height, y0, ROW_HEIGHT);
        for (Row row : rows) {
            this.addEntry(row);
        }
    }

    @Override
    public int getRowWidth() {
        return Math.min(380, this.getWidth() - 24);
    }

    @Override
    protected void extractListBackground(GuiGraphicsExtractor gfx) {
        // The screen draws its own rounded panel; no vanilla list background.
    }

    @Override
    protected void extractListSeparators(GuiGraphicsExtractor gfx) {
        // No vanilla row separators.
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

        protected void drawRowCard(GuiGraphicsExtractor gfx, boolean hovered) {
            GuiUtil.fillRoundCard(gfx,
                    this.getX(), this.getY() + 1, this.getWidth(), this.getHeight() - 2, 3,
                    hovered ? GuiUtil.CARD_BORDER_HOVER : GuiUtil.CARD_BORDER,
                    hovered ? GuiUtil.CARD_HOVER : GuiUtil.CARD);
        }
    }

    /** Category title row (non-interactive). */
    public static class HeaderRow extends Row {
        private final Component title;

        public HeaderRow(Component title) {
            this.title = title;
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of();
        }

        @Override
        public void extractContent(GuiGraphicsExtractor gfx, int mouseX, int mouseY, boolean hovered, float partialTick) {
            gfx.text(Minecraft.getInstance().font, this.title,
                    this.getContentX() + 6, this.getContentYMiddle() - 4, GuiUtil.ACCENT);
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
            this.label = label;
            this.widget = widget;
            if (tooltip != null) {
                widget.setTooltip(Tooltip.create(tooltip));
            }
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(this.widget);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor gfx, int mouseX, int mouseY, boolean hovered, float partialTick) {
            this.drawRowCard(gfx, hovered);
            gfx.text(Minecraft.getInstance().font, this.label,
                    this.getX() + 10, this.getContentYMiddle() - 4, GuiUtil.TEXT);
            this.widget.setPosition(
                    this.getX() + this.getWidth() - this.widget.getWidth() - 10,
                    this.getContentYMiddle() - this.widget.getHeight() / 2);
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
            this.slider.setPosition(this.getX() + 8, this.getY() + 1);
            this.slider.setWidth(this.getWidth() - 16);
            this.slider.extractRenderState(gfx, mouseX, mouseY, partialTick);
        }
    }
}
