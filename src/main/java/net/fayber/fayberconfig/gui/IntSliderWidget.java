package net.fayber.fayberconfig.gui;

import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Ranged int slider: snaps to {@code step}, writes through the entry's IntConsumer. */
public class IntSliderWidget extends StyledSlider {
    private final IntSupplier getter;
    private final IntConsumer setter;

    public IntSliderWidget(int x, int y, int w, Component label, int min, int max, int step, IntSupplier getter, IntConsumer setter) {
        super(x, y, w, label, min, max, step, getter.getAsInt());
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    protected void updateMessage() {
        this.setMessage(Component.literal(trim(this.snappedValue())));
    }

    @Override
    protected void applyValue() {
        int v = (int) Math.round(this.snappedValue());
        this.setter.accept(v);
        // No knob re-snap here: the knob follows the raw drag position (StyledSlider) and only
        // the written value snaps, so dragging feels smooth.
    }

    @Override
    protected String format(double value) {
        return String.valueOf((long) value);
    }
}
