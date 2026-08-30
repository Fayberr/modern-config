package net.fayber.fayberconfig.gui;

import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Ranged float slider: snaps to {@code step}, writes through the entry's Consumer<Float>. */
public class FloatSliderWidget extends StyledSlider {
    private final Supplier<Float> getter;
    private final Consumer<Float> setter;

    public FloatSliderWidget(int x, int y, int w, Component label, float min, float max, float step, Supplier<Float> getter, Consumer<Float> setter) {
        super(x, y, w, label, min, max, step, getter.get());
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    protected void updateMessage() {
        this.setMessage(Component.literal(format(this.snappedValue())));
    }

    @Override
    protected void applyValue() {
        float v = (float) this.snappedValue();
        this.setter.accept(v);
        // No knob re-snap here: the knob follows the raw drag position (StyledSlider) and only
        // the written value snaps, so dragging feels smooth.
    }

    @Override
    protected String format(double value) {
        return String.format("%.2f", value);
    }
}
