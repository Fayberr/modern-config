package net.fayber.fayberconfig.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

/**
 * Flat dark card button: fill + 1px rounded border, centered label, hover brightens the card and
 * tints the border cyan. No vanilla button sprites.
 *
 * <p>{@link AbstractButton#extractWidgetRenderState} is final but only dispatches to
 * {@link #extractContents}, so overriding {@code extractContents} fully replaces the vanilla
 * sprite rendering while keeping click sounds, enter/space activation and narration.
 */
public class FlatButton extends AbstractButton {
    private final Runnable onPress;

    public FlatButton(int x, int y, int w, int h, Component message, Runnable onPress) {
        super(x, y, w, h, message);
        this.onPress = onPress;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        this.onPress.run();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        boolean hovered = this.isHoveredOrFocused();
        int fill = hovered ? GuiUtil.CARD_HOVER : GuiUtil.CARD;
        int border = hovered ? GuiUtil.ACCENT : GuiUtil.PANEL_BORDER;
        GuiUtil.fillRoundCard(gfx, this.getX(), this.getY(), this.getWidth(), this.getHeight(), 3, border, fill);
        var font = Minecraft.getInstance().font;
        int textY = this.getY() + (this.getHeight() - font.lineHeight) / 2;
        gfx.centeredText(font, this.getMessage(), this.getX() + this.getWidth() / 2, textY, GuiUtil.TEXT);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
