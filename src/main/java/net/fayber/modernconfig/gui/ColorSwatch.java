package net.fayber.modernconfig.gui;

import net.fayber.moderngui.render.Theme;
import net.fayber.moderngui.render.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.IntSupplier;

/**
 * A small colour chip that reads its fill from a supplier every frame (so it tracks the value
 * while the hex text is being typed) and runs an action on click. The config rows use it as the
 * click target that opens the colour picker modal.
 */
public class ColorSwatch extends AbstractWidget {
    private static final float RADIUS = 4.0f;

    private final IntSupplier color;
    private final Runnable onPress;
    private Theme theme = Theme.dark();

    public ColorSwatch(int x, int y, int size, IntSupplier color, Runnable onPress) {
        super(x, y, size, size, Component.empty());
        this.color = color;
        this.onPress = onPress;
    }

    public ColorSwatch theme(Theme theme) {
        this.theme = theme;
        return this;
    }

    /** Hover tooltip (vanilla style), used for the open-the-picker hint. */
    public ColorSwatch tooltip(Component tooltip) {
        this.setTooltip(Tooltip.create(tooltip));
        return this;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.isActive() && this.isMouseOver(event.x(), event.y())) {
            this.onPress.run();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        boolean hovered = this.isActive() && this.isHoveredOrFocused();
        Ui.roundRectBorder(gfx, this.getX(), this.getY(), this.getWidth(), this.getHeight(), RADIUS,
                this.color.getAsInt(), hovered ? this.theme.cardBorderHover : this.theme.cardBorder, 1.0f);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Ui.ui(Component.literal("Color")));
    }
}
