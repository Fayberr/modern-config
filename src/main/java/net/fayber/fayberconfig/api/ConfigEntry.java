package net.fayber.fayberconfig.api;

import net.fayber.fayberconfig.gui.ConfigEntryList;
import net.fayber.fayberconfig.gui.DoubleSliderWidget;
import net.fayber.fayberconfig.gui.FlatButton;
import net.fayber.fayberconfig.gui.FloatSliderWidget;
import net.fayber.fayberconfig.gui.IntSliderWidget;
import net.fayber.fayberconfig.gui.PillToggleWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

/**
 * One entry on a Fayber Config screen: either a category header or a bindable option. Options
 * read and write through consumer-supplied getter/setter functions (live preview), and snapshot
 * their old value when the screen opens so Cancel/ESC can restore it.
 *
 * <p>Entries are created through {@link FayberConfigScreen.Builder}; each builder method returns
 * the entry handle for chaining {@code .tooltip(...)}.
 */
public interface ConfigEntry {
    Component label();

    @Nullable
    Component tooltip();

    /**
     * Sets the tooltip text and returns the entry for chaining. The Builder's
     * {@code .tooltip(...)} calls this on the most recently added entry.
     */
    ConfigEntry tooltip(String tooltip);

    /** Records the current value (called once when the screen opens). */
    void snapshot();

    /** Writes the snapshotted value back through the setter (Cancel/ESC). */
    void restore();

    ConfigEntryList.Row createRow();

    /** Category header; not an option. */
    final class Header implements ConfigEntry {
        private final String name;

        public Header(String name) {
            this.name = name;
        }

        @Override
        public Component label() {
            return Component.literal(this.name);
        }

        @Override
        public Component tooltip() {
            return null;
        }

        @Override
        public Header tooltip(String tooltip) {
            return this; // headers have no tooltip
        }

        @Override
        public void snapshot() {
        }

        @Override
        public void restore() {
        }

        @Override
        public ConfigEntryList.Row createRow() {
            return new ConfigEntryList.HeaderRow(this.label());
        }
    }

    /** Boolean pill toggle. */
    final class Bool implements ConfigEntry {
        private final Component label;
        private final Supplier<Boolean> getter;
        private final Consumer<Boolean> setter;
        private Component tooltip;
        private Boolean oldValue;

        public Bool(String label, Supplier<Boolean> getter, Consumer<Boolean> setter) {
            this.label = Component.literal(label);
            this.getter = getter;
            this.setter = setter;
        }

        public Bool tooltip(String tooltip) {
            this.tooltip = Component.literal(tooltip);
            return this;
        }

        @Override
        public Component label() {
            return this.label;
        }

        @Override
        public Component tooltip() {
            return this.tooltip;
        }

        @Override
        public void snapshot() {
            this.oldValue = this.getter.get();
        }

        @Override
        public void restore() {
            this.setter.accept(this.oldValue);
        }

        @Override
        public ConfigEntryList.Row createRow() {
            return new ConfigEntryList.WidgetRow(this.label, this.tooltip,
                    new PillToggleWidget(0, 0, this.getter, this.setter));
        }
    }

    /** Ranged int slider. */
    final class IntSlider implements ConfigEntry {
        private final Component label;
        private final IntSupplier getter;
        private final IntConsumer setter;
        private final int min;
        private final int max;
        private final int step;
        private Component tooltip;
        private Integer oldValue;

        public IntSlider(String label, IntSupplier getter, IntConsumer setter, int min, int max, int step) {
            this.label = Component.literal(label);
            this.getter = getter;
            this.setter = setter;
            this.min = min;
            this.max = max;
            this.step = step;
        }

        public IntSlider tooltip(String tooltip) {
            this.tooltip = Component.literal(tooltip);
            return this;
        }

        @Override
        public Component label() {
            return this.label;
        }

        @Override
        public Component tooltip() {
            return this.tooltip;
        }

        @Override
        public void snapshot() {
            this.oldValue = this.getter.getAsInt();
        }

        @Override
        public void restore() {
            this.setter.accept(this.oldValue);
        }

        @Override
        public ConfigEntryList.Row createRow() {
            return new ConfigEntryList.SliderRow(new IntSliderWidget(0, 0, 100,
                    this.label, this.min, this.max, this.step, this.getter, this.setter));
        }
    }

    /** Ranged float slider. */
    final class FloatSlider implements ConfigEntry {
        private final Component label;
        private final Supplier<Float> getter;
        private final Consumer<Float> setter;
        private final float min;
        private final float max;
        private final float step;
        private Component tooltip;
        private Float oldValue;

        public FloatSlider(String label, Supplier<Float> getter, Consumer<Float> setter, float min, float max, float step) {
            this.label = Component.literal(label);
            this.getter = getter;
            this.setter = setter;
            this.min = min;
            this.max = max;
            this.step = step;
        }

        public FloatSlider tooltip(String tooltip) {
            this.tooltip = Component.literal(tooltip);
            return this;
        }

        @Override
        public Component label() {
            return this.label;
        }

        @Override
        public Component tooltip() {
            return this.tooltip;
        }

        @Override
        public void snapshot() {
            this.oldValue = this.getter.get();
        }

        @Override
        public void restore() {
            this.setter.accept(this.oldValue);
        }

        @Override
        public ConfigEntryList.Row createRow() {
            return new ConfigEntryList.SliderRow(new FloatSliderWidget(0, 0, 100,
                    this.label, this.min, this.max, this.step, this.getter, this.setter));
        }
    }

    /** Ranged double slider. */
    final class DoubleSlider implements ConfigEntry {
        private final Component label;
        private final Supplier<Double> getter;
        private final Consumer<Double> setter;
        private final double min;
        private final double max;
        private final double step;
        private Component tooltip;
        private Double oldValue;

        public DoubleSlider(String label, Supplier<Double> getter, Consumer<Double> setter, double min, double max, double step) {
            this.label = Component.literal(label);
            this.getter = getter;
            this.setter = setter;
            this.min = min;
            this.max = max;
            this.step = step;
        }

        public DoubleSlider tooltip(String tooltip) {
            this.tooltip = Component.literal(tooltip);
            return this;
        }

        @Override
        public Component label() {
            return this.label;
        }

        @Override
        public Component tooltip() {
            return this.tooltip;
        }

        @Override
        public void snapshot() {
            this.oldValue = this.getter.get();
        }

        @Override
        public void restore() {
            this.setter.accept(this.oldValue);
        }

        @Override
        public ConfigEntryList.Row createRow() {
            return new ConfigEntryList.SliderRow(new DoubleSliderWidget(0, 0, 100,
                    this.label, this.min, this.max, this.step, this.getter, this.setter));
        }
    }

    /** Single-line string field. */
    final class Text implements ConfigEntry {
        private final Component label;
        private final Supplier<String> getter;
        private final Consumer<String> setter;
        private final int maxLength;
        private Component tooltip;
        private String oldValue;

        public Text(String label, Supplier<String> getter, Consumer<String> setter, int maxLength) {
            this.label = Component.literal(label);
            this.getter = getter;
            this.setter = setter;
            this.maxLength = maxLength;
        }

        public Text tooltip(String tooltip) {
            this.tooltip = Component.literal(tooltip);
            return this;
        }

        @Override
        public Component label() {
            return this.label;
        }

        @Override
        public Component tooltip() {
            return this.tooltip;
        }

        @Override
        public void snapshot() {
            this.oldValue = this.getter.get();
        }

        @Override
        public void restore() {
            this.setter.accept(this.oldValue);
        }

        @Override
        public ConfigEntryList.Row createRow() {
            EditBox box = new EditBox(Minecraft.getInstance().font, 0, 0, 140, 16, this.label);
            box.setMaxLength(this.maxLength);
            box.setBordered(false);
            box.setTextColor(0xFFFFFFFF);
            box.setValue(this.getter.get());
            box.setResponder(this.setter::accept);
            return new ConfigEntryList.WidgetRow(this.label, this.tooltip, box);
        }
    }

    /** Action button (no config value). */
    final class Button implements ConfigEntry {
        private final Component label;
        private final Runnable onPress;
        private Component tooltip;

        public Button(String label, Runnable onPress) {
            this.label = Component.literal(label);
            this.onPress = onPress;
        }

        public Button tooltip(String tooltip) {
            this.tooltip = Component.literal(tooltip);
            return this;
        }

        @Override
        public Component label() {
            return this.label;
        }

        @Override
        public Component tooltip() {
            return this.tooltip;
        }

        @Override
        public void snapshot() {
        }

        @Override
        public void restore() {
        }

        @Override
        public ConfigEntryList.Row createRow() {
            FlatButton flat = new FlatButton(0, 0, 110, 16, this.label, this.onPress);
            return new ConfigEntryList.WidgetRow(Component.empty(), this.tooltip, flat);
        }
    }
}
