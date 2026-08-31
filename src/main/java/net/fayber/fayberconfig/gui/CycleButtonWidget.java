package net.fayber.fayberconfig.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Cycle button: a flat card that shows the current value and steps through a fixed list of values
 * on click (left click forward, right click backward). Reads its value through the getter every
 * frame, so external changes show immediately, and writes through on press (live preview).
 *
 * <p>Sizes itself to the widest value label plus padding, so a short enum does not carry a button
 * that is wider than its longest value. Styled like {@link FlatButton}'s GHOST weight, which is
 * what every other value-ish control on the right of a row uses.
 */
public class CycleButtonWidget<T> extends AbstractButton {
    private static final float RADIUS = 5.0f;
    /** Horizontal room either side of the widest value label. */
    private static final int PADDING = 10;

    private final Supplier<T> getter;
    private final Consumer<T> setter;
    private final T[] values;
    private final Function<T, Component> namer;

    public CycleButtonWidget(int x, int y, int height, Supplier<T> getter, Consumer<T> setter,
                             T[] values, Function<T, String> namer) {
        super(x, y, 20, height, Component.empty());
        this.getter = getter;
        this.setter = setter;
        this.values = values;
        this.namer = value -> Ui.ui(namer.apply(value));
        int widest = 0;
        for (T value : values) {
            widest = Math.max(widest, Ui.font().width(this.namer.apply(value)));
        }
        this.setWidth(widest + 2 * PADDING);
    }

    @Override
    public void onPress(InputWithModifiers input) {
        boolean backward = input instanceof MouseButtonEvent event
                && event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
        this.setter.accept(this.values[this.nextIndex(backward ? -1 : 1)]);
    }

    /** Index of the value {@code step} places past the current one, wrapping at the ends. */
    private int nextIndex(int step) {
        int current = -1;
        T value = this.getter.get();
        for (int i = 0; i < this.values.length; i++) {
            if (this.values[i] == value || this.values[i].equals(value)) {
                current = i;
                break;
            }
        }
        return current < 0 ? 0 : Math.floorMod(current + step, this.values.length);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        boolean hovered = this.isHoveredOrFocused();
        Ui.roundRectBorder(gfx, this.getX(), this.getY(), this.getWidth(), this.getHeight(), RADIUS,
                hovered ? GuiUtil.CARD_HOVER : GuiUtil.CARD,
                hovered ? GuiUtil.CARD_BORDER_HOVER : GuiUtil.CARD_BORDER, 1.0f);
        int textY = this.getY() + (this.getHeight() - Ui.font().lineHeight) / 2 + 1;
        Ui.textCentered(gfx, this.namer.apply(this.getter.get()),
                this.getX() + this.getWidth() / 2, textY, hovered ? GuiUtil.TEXT : GuiUtil.TEXT_SECONDARY);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
