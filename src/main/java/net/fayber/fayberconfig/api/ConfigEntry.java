package net.fayber.fayberconfig.api;

import net.fayber.faybergui.render.Theme;
import net.fayber.faybergui.render.Ui;
import net.fayber.faybergui.widget.CycleButton;
import net.fayber.faybergui.widget.FlatButton;
import net.fayber.faybergui.widget.KeybindField;
import net.fayber.faybergui.widget.PillToggle;
import net.fayber.faybergui.widget.TextArea;
import net.fayber.faybergui.widget.TextField;
import net.fayber.fayberconfig.gui.ConfigEntryList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
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
    /** The fayber-gui palette, shared by every entry's widgets. */
    Theme THEME = Theme.dark();

    /** Input field metrics, sized to sit inside a {@link ConfigEntryList#CARD_HEIGHT} card. */
    int FIELD_HEIGHT = 22;
    int FIELD_WIDTH = 132;

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

    /** Static paragraph of documentation text; not an option and not interactive. */
    final class Note implements ConfigEntry {
        private final String text;

        public Note(String text) {
            this.text = text;
        }

        @Override
        public Component label() {
            return Component.literal(this.text);
        }

        @Override
        public Component tooltip() {
            return null;
        }

        @Override
        public Note tooltip(String tooltip) {
            return this; // notes have no tooltip
        }

        @Override
        public void snapshot() {
        }

        @Override
        public void restore() {
        }

        @Override
        public ConfigEntryList.Row createRow() {
            return new ConfigEntryList.NoteRow(this.label());
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
                    new PillToggle(0, 0, this.getter, this.setter));
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
            return new ConfigEntryList.SliderRow(new net.fayber.faybergui.widget.IntSlider(0, 0, 100,
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
            return new ConfigEntryList.SliderRow(new net.fayber.faybergui.widget.FloatSlider(0, 0, 100,
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
            return new ConfigEntryList.SliderRow(new net.fayber.faybergui.widget.DoubleSlider(0, 0, 100,
                    this.label, this.min, this.max, this.step, this.getter, this.setter));
        }
    }

    /** Single-line string field. */
    final class Text implements ConfigEntry {
        private final Component label;
        private final Supplier<String> getter;
        private final Consumer<String> setter;
        private final int maxLength;
        /** Marks non-conforming input with the error colour; null accepts anything. */
        @Nullable
        private final Predicate<String> validator;
        private Component tooltip;
        private String oldValue;

        public Text(String label, Supplier<String> getter, Consumer<String> setter, int maxLength) {
            this(label, getter, setter, maxLength, null);
        }

        public Text(String label, Supplier<String> getter, Consumer<String> setter, int maxLength,
                    @Nullable Predicate<String> validator) {
            this.label = Component.literal(label);
            this.getter = getter;
            this.setter = setter;
            this.maxLength = maxLength;
            this.validator = validator;
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
            // Uses the toolkit's TextField, not a raw EditBox. A borderless EditBox draws its text
            // at the widget's top-left corner (vanilla only centres it vertically when bordered and
            // only insets it when bordered), so a raw 150px box right-aligned in the card left the
            // value floating high and 150px shy of the right edge. TextField owns its own card.
            TextField field = new TextField(0, 0, FIELD_WIDTH, FIELD_HEIGHT)
                    .theme(THEME)
                    .maxLength(this.maxLength)
                    .value(this.getter.get())
                    .onChanged(this.setter);
            if (this.validator != null) {
                field.validator(this.validator);
            }
            return new ConfigEntryList.WidgetRow(this.label, this.tooltip, field);
        }
    }

    /** Fixed set of values stepped through with a cycle button (enums, modes, ...). */
    final class Cycle<T> implements ConfigEntry {
        private final Component label;
        private final Supplier<T> getter;
        private final Consumer<T> setter;
        private final T[] values;
        private final Function<T, String> namer;
        private Component tooltip;
        private T oldValue;

        public Cycle(String label, Supplier<T> getter, Consumer<T> setter, T[] values,
                     Function<T, String> namer) {
            this.label = Component.literal(label);
            this.getter = getter;
            this.setter = setter;
            this.values = values;
            this.namer = namer;
        }

        public Cycle<T> tooltip(String tooltip) {
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
                    new CycleButton<>(0, 0, 20, this.getter, this.setter, this.values, this.namer));
        }
    }

    /**
     * Key bind field: a ghost button that shows the current bind and captures the next key press
     * or mouse click when armed. Codes follow the toolkit's {@code KeybindField} convention:
     * GLFW keycodes as-is, {@code 1000 + button} for mouse buttons.
     */
    final class Keybind implements ConfigEntry {
        private final Component label;
        private final IntSupplier getter;
        private final IntConsumer setter;
        private Component tooltip;
        private Integer oldValue;

        public Keybind(String label, IntSupplier getter, IntConsumer setter) {
            this.label = Component.literal(label);
            this.getter = getter;
            this.setter = setter;
        }

        public Keybind tooltip(String tooltip) {
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
            KeybindField field = new KeybindField(0, 0, FIELD_WIDTH, this.getter::getAsInt, this.setter::accept)
                    .theme(THEME);
            return new ConfigEntryList.WidgetRow(this.label, this.tooltip, field);
        }
    }

    /**
     * Multi-line string list editor: one item per line in a {@link TextArea}. The widget is as
     * tall as the list needs (three lines minimum, eight maximum) and sits in a
     * {@link ConfigEntryList.TallWidgetRow}; it scrolls internally beyond that. Item strings are
     * not allowed to contain newlines by this representation.
     */
    final class StringList implements ConfigEntry {
        private final Component label;
        private final Supplier<List<String>> getter;
        private final Consumer<List<String>> setter;
        private Component tooltip;
        private List<String> oldValue;

        public StringList(String label, Supplier<List<String>> getter, Consumer<List<String>> setter) {
            this.label = Component.literal(label);
            this.getter = getter;
            this.setter = setter;
        }

        public StringList tooltip(String tooltip) {
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
            this.oldValue = List.copyOf(this.getter.get());
        }

        @Override
        public void restore() {
            this.setter.accept(this.oldValue);
        }

        @Override
        public ConfigEntryList.Row createRow() {
            // One line of headroom over the current item count so adding an item needs no resize;
            // TextArea pads 8 on top and bottom.
            int visibleLines = Math.clamp(this.getter.get().size() + 1, 3, 8);
            int areaHeight = 2 * 8 + visibleLines * Ui.font().lineHeight;
            TextArea area = new TextArea(0, 0, 100, areaHeight)
                    .theme(THEME)
                    .maxLength(4000)
                    .value(String.join("\n", this.getter.get()))
                    .onChanged(text -> this.setter.accept(
                            new ArrayList<>(Arrays.asList(text.split("\n", -1)))));
            return new ConfigEntryList.TallWidgetRow(this.label, this.tooltip, area);
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
            FlatButton flat = new FlatButton(0, 0, 110, 22, this.label, this.onPress);
            return new ConfigEntryList.WidgetRow(Component.empty(), this.tooltip, flat);
        }
    }
}
