package net.fayber.fayberconfig.gui;

import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Ranged double slider: snaps to {@code step}, writes through the entry's Consumer<Double>. */
public class DoubleSliderWidget extends StyledSlider {
    private final Supplier<Double> getter;
    private final Consumer<Double> setter;

    public DoubleSliderWidget(int x, int y, int w, Component label, double min, double max, double step, Supplier<Double> getter, Consumer<Double> setter) {
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
        double v = this.snappedValue();
        this.setter.accept(v);
        // No knob re-snap here: the knob follows the raw drag position (StyledSlider) and only
        // the written value snaps, so dragging feels smooth.
    }

    @Override
    protected String format(double value) {
        return String.format("%.2f", value);
    }
}
