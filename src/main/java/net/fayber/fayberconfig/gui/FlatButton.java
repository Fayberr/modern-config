package net.fayber.fayberconfig.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

/**
 * Flat rounded button in two weights: {@link Style#PRIMARY} (near-white fill, dark label, for the
 * confirming action) and {@link Style#GHOST} (dark card, light label, for everything else).
 *
 * <p>{@link AbstractButton#extractWidgetRenderState} is final but only dispatches to
 * {@link #extractContents}, so overriding {@code extractContents} fully replaces the vanilla
 * sprite rendering while keeping click sounds, enter/space activation and narration.
 */
public class FlatButton extends AbstractButton {
    public enum Style {
        PRIMARY,
        GHOST
    }

    private static final float RADIUS = 5.0f;

    private final Runnable onPress;
    private final Style style;

    public FlatButton(int x, int y, int w, int h, Component message, Runnable onPress) {
        this(x, y, w, h, message, onPress, Style.GHOST);
    }

    public FlatButton(int x, int y, int w, int h, Component message, Runnable onPress, Style style) {
        super(x, y, w, h, Ui.ui(message));
        this.onPress = onPress;
        this.style = style;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        this.onPress.run();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        boolean hovered = this.isHoveredOrFocused();
        int textColor;
        if (this.style == Style.PRIMARY) {
            Ui.roundRect(gfx, this.getX(), this.getY(), this.getWidth(), this.getHeight(), RADIUS,
                    hovered ? GuiUtil.ACCENT_HOVER : GuiUtil.ACCENT);
            textColor = GuiUtil.TEXT_ON_ACCENT;
        } else {
            Ui.roundRectBorder(gfx, this.getX(), this.getY(), this.getWidth(), this.getHeight(), RADIUS,
                    hovered ? GuiUtil.CARD_HOVER : GuiUtil.CARD,
                    hovered ? GuiUtil.CARD_BORDER_HOVER : GuiUtil.CARD_BORDER, 1.0f);
            textColor = hovered ? GuiUtil.TEXT : GuiUtil.TEXT_SECONDARY;
        }

        int textY = this.getY() + (this.getHeight() - Ui.font().lineHeight) / 2 + 1;
        Ui.textCentered(gfx, this.getMessage(), this.getX() + this.getWidth() / 2, textY, textColor);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
