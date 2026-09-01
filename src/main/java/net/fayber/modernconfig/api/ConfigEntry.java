package net.fayber.modernconfig.api;

import net.fayber.moderngui.render.Theme;
import net.fayber.moderngui.render.Ui;
import net.fayber.moderngui.widget.CycleButton;
import net.fayber.moderngui.widget.Dropdown;
import net.fayber.moderngui.widget.FlatButton;
import net.fayber.moderngui.widget.Icons;
import net.fayber.moderngui.widget.IconButton;
import net.fayber.moderngui.widget.KeybindField;
import net.fayber.moderngui.widget.ListEditor;
import net.fayber.moderngui.widget.PillToggle;
import net.fayber.moderngui.widget.TextField;
import net.fayber.modernconfig.gui.ConfigEntryList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

/**
 * One entry on a Modern Config screen: either a category header or a bindable option. Options
 * read and write through consumer-supplied getter/setter functions (live preview), and snapshot
 * their old value when the screen opens so Cancel/ESC can restore it.
 *
 * <p>Options may declare a default value (through the Builder's overloads). A declared default
 * adds a reset button to the option's card, shown while the current value differs from the
 * default; clicking it writes the default through the setter, so it previews live and
 * Cancel/ESC still restores the value the screen opened with.
 *
 * <p>Entries are created through {@link ModernConfigScreen.Builder}; each builder method returns
 * the entry handle for chaining {@code .tooltip(...)}.
 */
public interface ConfigEntry {
    /** The modern-gui palette, shared by every entry's widgets. */
    Theme THEME = Theme.dark();

    /** Input field metrics, sized to sit inside a {@link ConfigEntryList#CARD_HEIGHT} card. */
    int FIELD_HEIGHT = 22;
    int FIELD_WIDTH = 132;
    /** Side length of the per-row reset button. */
    int RESET_SIZE = 14;

    Component label();

    @Nullable
    Component tooltip();

    /**
     * Sets the tooltip text and returns the entry for chaining. The Builder's
     * {@code .tooltip(...)} calls this on the most recently added entry. Stored as given, so a
     * translatable resolves against the current language whenever the tooltip renders.
     */
    ConfigEntry tooltip(String tooltip);

    /** Component variant of {@link #tooltip(String)}; a translatable resolves at draw time. */
    ConfigEntry tooltip(Component tooltip);

    /** Records the current value (called once when the screen opens). */
    void snapshot();

    /** Writes the snapshotted value back through the setter (Cancel/ESC). */
    void restore();

    ConfigEntryList.Row createRow();

    /**
     * The reset-to-default button for an option row, or null when no default is declared. The
     * write goes through the same setter as a user edit, so it previews live and Cancel/ESC
     * still restores the value the screen opened with.
     *
     * @param defaultValue the declared default; null means the option has none
     */
    static <T> ConfigEntryList.Reset resetOf(@Nullable T defaultValue, Supplier<T> currentValue,
                                             Consumer<T> applyDefault) {
        if (defaultValue == null) {
            return null;
        }
        IconButton button = new IconButton(0, 0, RESET_SIZE, Icons.ROTATE_CCW,
                () -> applyDefault.accept(defaultValue))
                .theme(THEME)
                .tooltip(Component.translatable("modernconfig.reset_default"));
        return new ConfigEntryList.Reset(button, () -> !Objects.equals(currentValue.get(), defaultValue));
    }

    /** Hex text shape accepted by {@link Color}: an optional hash, then up to 8 hex digits. */
    Predicate<String> COLOR_TEXT = s -> s.matches("#?[0-9a-fA-F]{0,8}");

    /** An ARGB colour as hex text; 6 digits when fully opaque, 8 otherwise. */
    static String hexOf(int argb) {
        return (argb >>> 24) == 0xFF
                ? String.format("#%06X", argb & 0xFFFFFF)
                : String.format("#%08X", argb);
    }

    /** Parses "#RRGGBB" (opaque) or "#AARRGGBB"; the hash is optional. Empty when unparseable. */
    static java.util.OptionalInt parseHexColor(String text) {
        String hex = text.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (hex.length() != 6 && hex.length() != 8) {
            return java.util.OptionalInt.empty();
        }
        try {
            int value = (int) Long.parseLong(hex, 16);
            return java.util.OptionalInt.of(hex.length() == 6 ? value | 0xFF000000 : value);
        } catch (NumberFormatException e) {
            return java.util.OptionalInt.empty();
        }
    }

    /**
     * Splits list-editor text into elements, one per line: blank lines are skipped and any line
     * that does not parse makes the whole edit fail (null), so a half-typed number never
     * reaches the setter.
     */
    static <T> @Nullable List<T> parseLines(String text, Function<String, @Nullable T> parse) {
        return parseLineList(Arrays.asList(text.split("\n", -1)), parse);
    }

    /**
     * The {@link ListEditor} flavour of {@link #parseLines}: one raw line per editor row, blank
     * lines skipped, all-or-nothing (any unparseable line returns null and nothing is written).
     */
    static <T> @Nullable List<T> parseLineList(List<String> lines, Function<String, @Nullable T> parse) {
        List<T> out = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            T value = parse.apply(trimmed);
            if (value == null) {
                return null;
            }
            out.add(value);
        }
        return out;
    }

    /** A number list as list-editor text, one value per line. */
    static String joinLines(List<? extends Number> values) {
        return String.join("\n", linesOf(values));
    }

    /** A number list as editor rows, one value per row. */
    static List<String> linesOf(List<? extends Number> values) {
        List<String> lines = new ArrayList<>(values.size());
        for (Number value : values) {
            lines.add(String.valueOf(value));
        }
        return lines;
    }

    static @Nullable Integer parseIntOrNull(String text) {
        try {
            return Integer.valueOf(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static @Nullable Float parseFloatOrNull(String text) {
        try {
            return Float.parseFloat(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static @Nullable Double parseDoubleOrNull(String text) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Category header; not an option. */
    final class Header implements ConfigEntry {
        private final Component name;

        public Header(String name) {
            this(Component.literal(name));
        }

        /** Component variant: a translatable name resolves at draw time. */
        public Header(Component name) {
            this.name = name;
        }

        @Override
        public Component label() {
            return this.name;
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
        public Header tooltip(Component tooltip) {
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
        private final Component text;

        public Note(String text) {
            this(Component.literal(text));
        }

        /** Component variant: a translatable text resolves when the note is wrapped and drawn. */
        public Note(Component text) {
            this.text = text;
        }

        @Override
        public Component label() {
            return this.text;
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
        public Note tooltip(Component tooltip) {
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
        @Nullable
        private final Boolean defaultValue;
        private Component tooltip;
        private Boolean oldValue;

        public Bool(String label, Supplier<Boolean> getter, Consumer<Boolean> setter) {
            this(Component.literal(label), getter, setter);
        }

        /** Component variant: a translatable label resolves at draw time. */
        public Bool(Component label, Supplier<Boolean> getter, Consumer<Boolean> setter) {
            this(label, getter, setter, null);
        }

        public Bool(String label, Supplier<Boolean> getter, Consumer<Boolean> setter,
                    @Nullable Boolean defaultValue) {
            this(Component.literal(label), getter, setter, defaultValue);
        }

        /** Component variant: a translatable label resolves at draw time. */
        public Bool(Component label, Supplier<Boolean> getter, Consumer<Boolean> setter,
                    @Nullable Boolean defaultValue) {
            this.label = label;
            this.getter = getter;
            this.setter = setter;
            this.defaultValue = defaultValue;
        }

        public Bool tooltip(String tooltip) {
            return this.tooltip(Component.literal(tooltip));
        }

        /** Component variant: a translatable tooltip resolves when it renders. */
        public Bool tooltip(Component tooltip) {
            this.tooltip = tooltip;
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
                    new PillToggle(0, 0, this.getter, this.setter),
                    resetOf(this.defaultValue, this.getter, this.setter));
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
        @Nullable
        private final Integer defaultValue;
        private Component tooltip;
        private Integer oldValue;

        public IntSlider(String label, IntSupplier getter, IntConsumer setter, int min, int max, int step) {
            this(Component.literal(label), getter, setter, min, max, step);
        }

        /** Component variant: a translatable label resolves at draw time. */
        public IntSlider(Component label, IntSupplier getter, IntConsumer setter, int min, int max, int step) {
            this(label, getter, setter, min, max, step, null);
        }

        public IntSlider(String label, IntSupplier getter, IntConsumer setter, int min, int max, int step,
                         @Nullable Integer defaultValue) {
            this(Component.literal(label), getter, setter, min, max, step, defaultValue);
        }

        /** Component variant: a translatable label resolves at draw time. */
        public IntSlider(Component label, IntSupplier getter, IntConsumer setter, int min, int max, int step,
                         @Nullable Integer defaultValue) {
            this.label = label;
            this.getter = getter;
            this.setter = setter;
            this.min = min;
            this.max = max;
            this.step = step;
            this.defaultValue = defaultValue;
        }

        public IntSlider tooltip(String tooltip) {
            return this.tooltip(Component.literal(tooltip));
        }

        /** Component variant: a translatable tooltip resolves when it renders. */
        public IntSlider tooltip(Component tooltip) {
            this.tooltip = tooltip;
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
            return new ConfigEntryList.SliderRow(new net.fayber.moderngui.widget.IntSlider(0, 0, 100,
                            this.label, this.min, this.max, this.step, this.getter, this.setter),
                    resetOf(this.defaultValue, this.getter::getAsInt, this.setter::accept));
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
        @Nullable
        private final Float defaultValue;
        private Component tooltip;
        private Float oldValue;

        public FloatSlider(String label, Supplier<Float> getter, Consumer<Float> setter, float min, float max, float step) {
            this(Component.literal(label), getter, setter, min, max, step);
        }

        /** Component variant: a translatable label resolves at draw time. */
        public FloatSlider(Component label, Supplier<Float> getter, Consumer<Float> setter, float min, float max, float step) {
            this(label, getter, setter, min, max, step, null);
        }

        public FloatSlider(String label, Supplier<Float> getter, Consumer<Float> setter, float min, float max, float step,
                           @Nullable Float defaultValue) {
            this(Component.literal(label), getter, setter, min, max, step, defaultValue);
        }

        /** Component variant: a translatable label resolves at draw time. */
        public FloatSlider(Component label, Supplier<Float> getter, Consumer<Float> setter, float min, float max, float step,
                           @Nullable Float defaultValue) {
            this.label = label;
            this.getter = getter;
            this.setter = setter;
            this.min = min;
            this.max = max;
            this.step = step;
            this.defaultValue = defaultValue;
        }

        public FloatSlider tooltip(String tooltip) {
            return this.tooltip(Component.literal(tooltip));
        }

        /** Component variant: a translatable tooltip resolves when it renders. */
        public FloatSlider tooltip(Component tooltip) {
            this.tooltip = tooltip;
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
            return new ConfigEntryList.SliderRow(new net.fayber.moderngui.widget.FloatSlider(0, 0, 100,
                            this.label, this.min, this.max, this.step, this.getter, this.setter),
                    resetOf(this.defaultValue, this.getter, this.setter));
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
        @Nullable
        private final Double defaultValue;
        private Component tooltip;
        private Double oldValue;

        public DoubleSlider(String label, Supplier<Double> getter, Consumer<Double> setter, double min, double max, double step) {
            this(Component.literal(label), getter, setter, min, max, step);
        }

        /** Component variant: a translatable label resolves at draw time. */
        public DoubleSlider(Component label, Supplier<Double> getter, Consumer<Double> setter, double min, double max, double step) {
            this(label, getter, setter, min, max, step, null);
        }

        public DoubleSlider(String label, Supplier<Double> getter, Consumer<Double> setter, double min, double max, double step,
                            @Nullable Double defaultValue) {
            this(Component.literal(label), getter, setter, min, max, step, defaultValue);
        }

        /** Component variant: a translatable label resolves at draw time. */
        public DoubleSlider(Component label, Supplier<Double> getter, Consumer<Double> setter, double min, double max, double step,
                            @Nullable Double defaultValue) {
            this.label = label;
            this.getter = getter;
            this.setter = setter;
            this.min = min;
            this.max = max;
            this.step = step;
            this.defaultValue = defaultValue;
        }

        public DoubleSlider tooltip(String tooltip) {
            return this.tooltip(Component.literal(tooltip));
        }

        /** Component variant: a translatable tooltip resolves when it renders. */
        public DoubleSlider tooltip(Component tooltip) {
            this.tooltip = tooltip;
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
            return new ConfigEntryList.SliderRow(new net.fayber.moderngui.widget.DoubleSlider(0, 0, 100,
                            this.label, this.min, this.max, this.step, this.getter, this.setter),
                    resetOf(this.defaultValue, this.getter, this.setter));
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
        @Nullable
        private final String defaultValue;
        private Component tooltip;
        private String oldValue;

        public Text(String label, Supplier<String> getter, Consumer<String> setter, int maxLength) {
            this(Component.literal(label), getter, setter, maxLength);
        }

        /** Component variant: a translatable label resolves at draw time. */
        public Text(Component label, Supplier<String> getter, Consumer<String> setter, int maxLength) {
            this(label, getter, setter, maxLength, null, null);
        }

        public Text(String label, Supplier<String> getter, Consumer<String> setter, int maxLength,
                    @Nullable Predicate<String> validator) {
            this(Component.literal(label), getter, setter, maxLength, validator);
        }

        /** Component variant: a translatable label resolves at draw time. */
        public Text(Component label, Supplier<String> getter, Consumer<String> setter, int maxLength,
                    @Nullable Predicate<String> validator) {
            this(label, getter, setter, maxLength, validator, null);
        }

        public Text(String label, Supplier<String> getter, Consumer<String> setter, int maxLength,
                    @Nullable String defaultValue) {
            this(Component.literal(label), getter, setter, maxLength, defaultValue);
        }

        /** Component variant: a translatable label resolves at draw time. */
        public Text(Component label, Supplier<String> getter, Consumer<String> setter, int maxLength,
                    @Nullable String defaultValue) {
            this(label, getter, setter, maxLength, null, defaultValue);
        }

        public Text(String label, Supplier<String> getter, Consumer<String> setter, int maxLength,
                    @Nullable Predicate<String> validator, @Nullable String defaultValue) {
            this(Component.literal(label), getter, setter, maxLength, validator, defaultValue);
        }

        /** Component variant: a translatable label resolves at draw time. */
        public Text(Component label, Supplier<String> getter, Consumer<String> setter, int maxLength,
                    @Nullable Predicate<String> validator, @Nullable String defaultValue) {
            this.label = label;
            this.getter = getter;
            this.setter = setter;
            this.maxLength = maxLength;
            this.validator = validator;
            this.defaultValue = defaultValue;
        }

        public Text tooltip(String tooltip) {
            return this.tooltip(Component.literal(tooltip));
        }

        /** Component variant: a translatable tooltip resolves when it renders. */
        public Text tooltip(Component tooltip) {
            this.tooltip = tooltip;
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
            // Uses the toolkit's TextField, not a raw EditBox: a borderless EditBox draws its
            // text at the top-left corner (vanilla only insets it when bordered), so a raw box
            // right-aligned in the card left the value floating high and short of the edge.
            TextField field = new TextField(0, 0, FIELD_WIDTH, FIELD_HEIGHT)
                    .theme(THEME)
                    .maxLength(this.maxLength)
                    .value(this.getter.get())
                    .onChanged(this.setter);
            if (this.validator != null) {
                field.validator(this.validator);
            }
            // TextField.value fires the responder, which runs the setter: one call both
            // repaints the field and writes the default through.
            return new ConfigEntryList.WidgetRow(this.label, this.tooltip, field,
                    resetOf(this.defaultValue, this.getter, field::value));
        }
    }

    /**
     * ARGB colour option edited as hex text: "#RRGGBB" (opaque) or "#AARRGGBB", with or without
     * the leading hash. A live swatch left of the field tracks the last valid value while
     * typing; half-typed input is marked invalid and never written through.
     */
    final class Color implements ConfigEntry {
        private final Component label;
        private final IntSupplier getter;
        private final IntConsumer setter;
        @Nullable
        private final Integer defaultValue;
        private Component tooltip;
        private Integer oldValue;

        public Color(String label, IntSupplier getter, IntConsumer setter) {
            this(Component.literal(label), getter, setter);
        }

        /** Component variant: a translatable label resolves at draw time. */
        public Color(Component label, IntSupplier getter, IntConsumer setter) {
            this(label, getter, setter, null);
        }

        public Color(String label, IntSupplier getter, IntConsumer setter, @Nullable Integer defaultValue) {
            this(Component.literal(label), getter, setter, defaultValue);
        }

        /** Component variant: a translatable label resolves at draw time. */
        public Color(Component label, IntSupplier getter, IntConsumer setter, @Nullable Integer defaultValue) {
            this.label = label;
            this.getter = getter;
            this.setter = setter;
            this.defaultValue = defaultValue;
        }

        public Color tooltip(String tooltip) {
            return this.tooltip(Component.literal(tooltip));
        }

        /** Component variant: a translatable tooltip resolves when it renders. */
        public Color tooltip(Component tooltip) {
            this.tooltip = tooltip;
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
            TextField field = new TextField(0, 0, 110, FIELD_HEIGHT)
                    .theme(THEME)
                    .maxLength(10)
                    .value(hexOf(this.getter.getAsInt()))
                    .validator(COLOR_TEXT)
                    .onChanged(text -> parseHexColor(text).ifPresent(this.setter::accept));
            // Both the picker and the reset button write through the hex field, so its responder
            // stays the one write path and the text always matches the applied value.
            IntConsumer writeHex = value -> field.value(hexOf(value));
            return new ConfigEntryList.ColorRow(this.label, this.tooltip, field, this.getter::getAsInt,
                    writeHex, resetOf(this.defaultValue, this.getter::getAsInt, writeHex::accept));
        }
    }

    /** Fixed set of values stepped through with a cycle button (enums, modes, ...). */
    final class Cycle<T> implements ConfigEntry {
        private final Component label;
        private final Supplier<T> getter;
        private final Consumer<T> setter;
        private final T[] values;
        private final Function<T, Component> namer;
        @Nullable
        private final T defaultValue;
        private Component tooltip;
        private T oldValue;

        public Cycle(String label, Supplier<T> getter, Consumer<T> setter, T[] values,
                     Function<T, String> namer) {
            this(Component.literal(label), getter, setter, values, namer.andThen(Component::literal));
        }

        /**
         * Component variant: a translatable label and a namer returning translatables resolve at
         * draw time.
         */
        public Cycle(Component label, Supplier<T> getter, Consumer<T> setter, T[] values,
                     Function<T, Component> namer) {
            this(label, getter, setter, values, namer, null);
        }

        public Cycle(String label, Supplier<T> getter, Consumer<T> setter, T[] values,
                     Function<T, String> namer, @Nullable T defaultValue) {
            this(Component.literal(label), getter, setter, values, namer.andThen(Component::literal),
                    defaultValue);
        }

        /** Component variant: a translatable label and namer resolve at draw time. */
        public Cycle(Component label, Supplier<T> getter, Consumer<T> setter, T[] values,
                     Function<T, Component> namer, @Nullable T defaultValue) {
            this.label = label;
            this.getter = getter;
            this.setter = setter;
            this.values = values;
            this.namer = namer;
            this.defaultValue = defaultValue;
        }

        public Cycle<T> tooltip(String tooltip) {
            return this.tooltip(Component.literal(tooltip));
        }

        /** Component variant: a translatable tooltip resolves when it renders. */
        public Cycle<T> tooltip(Component tooltip) {
            this.tooltip = tooltip;
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
                    new CycleButton<>(0, 0, 20, this.getter, this.setter, this.values, this.namer),
                    resetOf(this.defaultValue, this.getter, this.setter));
        }
    }

    /**
     * Fixed set of values picked from a dropdown menu, the other presentation for enums and
     * modes next to {@link Cycle}. Reads better than a cycle for long value lists.
     */
    final class Select<T> implements ConfigEntry {
        private final Component label;
        private final Supplier<T> getter;
        private final Consumer<T> setter;
        private final T[] values;
        private final Function<T, Component> namer;
        @Nullable
        private final T defaultValue;
        private Component tooltip;
        private T oldValue;

        public Select(String label, Supplier<T> getter, Consumer<T> setter, T[] values,
                      Function<T, String> namer) {
            this(Component.literal(label), getter, setter, values, namer.andThen(Component::literal));
        }

        /**
         * Component variant: a translatable label and a namer returning translatables resolve at
         * draw time (the dropdown re-reads its option components every frame).
         */
        public Select(Component label, Supplier<T> getter, Consumer<T> setter, T[] values,
                      Function<T, Component> namer) {
            this(label, getter, setter, values, namer, null);
        }

        public Select(String label, Supplier<T> getter, Consumer<T> setter, T[] values,
                      Function<T, String> namer, @Nullable T defaultValue) {
            this(Component.literal(label), getter, setter, values, namer.andThen(Component::literal),
                    defaultValue);
        }

        /** Component variant: a translatable label and namer resolve at draw time. */
        public Select(Component label, Supplier<T> getter, Consumer<T> setter, T[] values,
                      Function<T, Component> namer, @Nullable T defaultValue) {
            this.label = label;
            this.getter = getter;
            this.setter = setter;
            this.values = values;
            this.namer = namer;
            this.defaultValue = defaultValue;
        }

        public Select<T> tooltip(String tooltip) {
            return this.tooltip(Component.literal(tooltip));
        }

        /** Component variant: a translatable tooltip resolves when it renders. */
        public Select<T> tooltip(Component tooltip) {
            this.tooltip = tooltip;
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
            // Raw components: the Dropdown applies the Inter font itself when it stores them.
            List<Component> options = new ArrayList<>(this.values.length);
            for (T value : this.values) {
                options.add(this.namer.apply(value));
            }
            Dropdown dropdown = new Dropdown(0, 0, FIELD_WIDTH, FIELD_HEIGHT, options,
                    () -> this.indexOf(this.getter.get()),
                    index -> this.setter.accept(this.values[index]))
                    .theme(THEME);
            return new ConfigEntryList.WidgetRow(this.label, this.tooltip, dropdown,
                    resetOf(this.defaultValue, this.getter, this.setter));
        }

        /** The option list position of a value, or -1 when it is not among the options. */
        private int indexOf(T value) {
            for (int i = 0; i < this.values.length; i++) {
                if (Objects.equals(this.values[i], value)) {
                    return i;
                }
            }
            return -1;
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
        @Nullable
        private final Integer defaultValue;
        private Component tooltip;
        private Integer oldValue;

        public Keybind(String label, IntSupplier getter, IntConsumer setter) {
            this(Component.literal(label), getter, setter);
        }

        /** Component variant: a translatable label resolves at draw time. */
        public Keybind(Component label, IntSupplier getter, IntConsumer setter) {
            this(label, getter, setter, null);
        }

        public Keybind(String label, IntSupplier getter, IntConsumer setter, @Nullable Integer defaultValue) {
            this(Component.literal(label), getter, setter, defaultValue);
        }

        /** Component variant: a translatable label resolves at draw time. */
        public Keybind(Component label, IntSupplier getter, IntConsumer setter, @Nullable Integer defaultValue) {
            this.label = label;
            this.getter = getter;
            this.setter = setter;
            this.defaultValue = defaultValue;
        }

        public Keybind tooltip(String tooltip) {
            return this.tooltip(Component.literal(tooltip));
        }

        /** Component variant: a translatable tooltip resolves when it renders. */
        public Keybind tooltip(Component tooltip) {
            this.tooltip = tooltip;
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
            return new ConfigEntryList.WidgetRow(this.label, this.tooltip, field,
                    resetOf(this.defaultValue, this.getter::getAsInt, this.setter::accept));
        }
    }

    /**
     * Multi-line string list editor: one item per row in a {@link ListEditor} (its own field plus
     * remove button per item, add row at the bottom) inside a {@link ConfigEntryList.TallWidgetRow}
     * that grows with the row count. Blank rows are skipped on write.
     */
    final class StringList implements ConfigEntry {
        private final Component label;
        private final Supplier<List<String>> getter;
        private final Consumer<List<String>> setter;
        @Nullable
        private final List<String> defaultValue;
        private Component tooltip;
        private List<String> oldValue;

        public StringList(String label, Supplier<List<String>> getter, Consumer<List<String>> setter) {
            this(Component.literal(label), getter, setter);
        }

        /** Component variant: a translatable label resolves at draw time. */
        public StringList(Component label, Supplier<List<String>> getter, Consumer<List<String>> setter) {
            this(label, getter, setter, null);
        }

        public StringList(String label, Supplier<List<String>> getter, Consumer<List<String>> setter,
                          @Nullable List<String> defaultValue) {
            this(Component.literal(label), getter, setter, defaultValue);
        }

        /** Component variant: a translatable label resolves at draw time. */
        public StringList(Component label, Supplier<List<String>> getter, Consumer<List<String>> setter,
                          @Nullable List<String> defaultValue) {
            this.label = label;
            this.getter = getter;
            this.setter = setter;
            this.defaultValue = defaultValue == null ? null : List.copyOf(defaultValue);
        }

        public StringList tooltip(String tooltip) {
            return this.tooltip(Component.literal(tooltip));
        }

        /** Component variant: a translatable tooltip resolves when it renders. */
        public StringList tooltip(Component tooltip) {
            this.tooltip = tooltip;
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
            ListEditor editor = new ListEditor(0, 0, 100, List.copyOf(this.getter.get()))
                    .theme(THEME)
                    .onChanged(lines -> {
                        List<String> parsed = parseLineList(lines, Function.identity());
                        if (parsed != null) {
                            this.setter.accept(parsed);
                        }
                    });
            return new ConfigEntryList.TallWidgetRow(this.label, this.tooltip, editor,
                    resetOf(this.defaultValue, this.getter, editor::value));
        }
    }

    /**
     * List of integers edited one item per row in a {@link ListEditor}. Blank rows are skipped;
     * a row that does not parse leaves the last valid value in place (all-or-nothing, so a
     * half-typed number never reaches the setter).
     */
    final class IntList implements ConfigEntry {
        private final Component label;
        private final Supplier<List<Integer>> getter;
        private final Consumer<List<Integer>> setter;
        @Nullable
        private final List<Integer> defaultValue;
        private Component tooltip;
        private List<Integer> oldValue;

        public IntList(String label, Supplier<List<Integer>> getter, Consumer<List<Integer>> setter) {
            this(Component.literal(label), getter, setter);
        }

        /** Component variant: a translatable label resolves at draw time. */
        public IntList(Component label, Supplier<List<Integer>> getter, Consumer<List<Integer>> setter) {
            this(label, getter, setter, null);
        }

        public IntList(String label, Supplier<List<Integer>> getter, Consumer<List<Integer>> setter,
                       @Nullable List<Integer> defaultValue) {
            this(Component.literal(label), getter, setter, defaultValue);
        }

        /** Component variant: a translatable label resolves at draw time. */
        public IntList(Component label, Supplier<List<Integer>> getter, Consumer<List<Integer>> setter,
                       @Nullable List<Integer> defaultValue) {
            this.label = label;
            this.getter = getter;
            this.setter = setter;
            this.defaultValue = defaultValue == null ? null : List.copyOf(defaultValue);
        }

        public IntList tooltip(String tooltip) {
            return this.tooltip(Component.literal(tooltip));
        }

        /** Component variant: a translatable tooltip resolves when it renders. */
        public IntList tooltip(Component tooltip) {
            this.tooltip = tooltip;
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
            ListEditor editor = new ListEditor(0, 0, 100, linesOf(this.getter.get()))
                    .theme(THEME)
                    .lineValidator(s -> parseIntOrNull(s) != null)
                    .onChanged(lines -> {
                        List<Integer> parsed = parseLineList(lines, ConfigEntry::parseIntOrNull);
                        if (parsed != null) {
                            this.setter.accept(parsed);
                        }
                    });
            return new ConfigEntryList.TallWidgetRow(this.label, this.tooltip, editor,
                    resetOf(this.defaultValue, this.getter, value -> editor.value(linesOf(value))));
        }
    }

    /** List of floats, edited like {@link IntList}. */
    final class FloatList implements ConfigEntry {
        private final Component label;
        private final Supplier<List<Float>> getter;
        private final Consumer<List<Float>> setter;
        @Nullable
        private final List<Float> defaultValue;
        private Component tooltip;
        private List<Float> oldValue;

        public FloatList(String label, Supplier<List<Float>> getter, Consumer<List<Float>> setter) {
            this(Component.literal(label), getter, setter);
        }

        /** Component variant: a translatable label resolves at draw time. */
        public FloatList(Component label, Supplier<List<Float>> getter, Consumer<List<Float>> setter) {
            this(label, getter, setter, null);
        }

        public FloatList(String label, Supplier<List<Float>> getter, Consumer<List<Float>> setter,
                         @Nullable List<Float> defaultValue) {
            this(Component.literal(label), getter, setter, defaultValue);
        }

        /** Component variant: a translatable label resolves at draw time. */
        public FloatList(Component label, Supplier<List<Float>> getter, Consumer<List<Float>> setter,
                         @Nullable List<Float> defaultValue) {
            this.label = label;
            this.getter = getter;
            this.setter = setter;
            this.defaultValue = defaultValue == null ? null : List.copyOf(defaultValue);
        }

        public FloatList tooltip(String tooltip) {
            return this.tooltip(Component.literal(tooltip));
        }

        /** Component variant: a translatable tooltip resolves when it renders. */
        public FloatList tooltip(Component tooltip) {
            this.tooltip = tooltip;
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
            ListEditor editor = new ListEditor(0, 0, 100, linesOf(this.getter.get()))
                    .theme(THEME)
                    .lineValidator(s -> parseFloatOrNull(s) != null)
                    .onChanged(lines -> {
                        List<Float> parsed = parseLineList(lines, ConfigEntry::parseFloatOrNull);
                        if (parsed != null) {
                            this.setter.accept(parsed);
                        }
                    });
            return new ConfigEntryList.TallWidgetRow(this.label, this.tooltip, editor,
                    resetOf(this.defaultValue, this.getter, value -> editor.value(linesOf(value))));
        }
    }

    /** List of doubles, edited like {@link IntList}. */
    final class DoubleList implements ConfigEntry {
        private final Component label;
        private final Supplier<List<Double>> getter;
        private final Consumer<List<Double>> setter;
        @Nullable
        private final List<Double> defaultValue;
        private Component tooltip;
        private List<Double> oldValue;

        public DoubleList(String label, Supplier<List<Double>> getter, Consumer<List<Double>> setter) {
            this(Component.literal(label), getter, setter);
        }

        /** Component variant: a translatable label resolves at draw time. */
        public DoubleList(Component label, Supplier<List<Double>> getter, Consumer<List<Double>> setter) {
            this(label, getter, setter, null);
        }

        public DoubleList(String label, Supplier<List<Double>> getter, Consumer<List<Double>> setter,
                          @Nullable List<Double> defaultValue) {
            this(Component.literal(label), getter, setter, defaultValue);
        }

        /** Component variant: a translatable label resolves at draw time. */
        public DoubleList(Component label, Supplier<List<Double>> getter, Consumer<List<Double>> setter,
                          @Nullable List<Double> defaultValue) {
            this.label = label;
            this.getter = getter;
            this.setter = setter;
            this.defaultValue = defaultValue == null ? null : List.copyOf(defaultValue);
        }

        public DoubleList tooltip(String tooltip) {
            return this.tooltip(Component.literal(tooltip));
        }

        /** Component variant: a translatable tooltip resolves when it renders. */
        public DoubleList tooltip(Component tooltip) {
            this.tooltip = tooltip;
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
            ListEditor editor = new ListEditor(0, 0, 100, linesOf(this.getter.get()))
                    .theme(THEME)
                    .lineValidator(s -> parseDoubleOrNull(s) != null)
                    .onChanged(lines -> {
                        List<Double> parsed = parseLineList(lines, ConfigEntry::parseDoubleOrNull);
                        if (parsed != null) {
                            this.setter.accept(parsed);
                        }
                    });
            return new ConfigEntryList.TallWidgetRow(this.label, this.tooltip, editor,
                    resetOf(this.defaultValue, this.getter, value -> editor.value(linesOf(value))));
        }
    }

    /** Action button (no config value). */
    final class Button implements ConfigEntry {
        private final Component label;
        private final Runnable onPress;
        private Component tooltip;

        public Button(String label, Runnable onPress) {
            this(Component.literal(label), onPress);
        }

        /** Component variant: a translatable label resolves at draw time. */
        public Button(Component label, Runnable onPress) {
            this.label = label;
            this.onPress = onPress;
        }

        public Button tooltip(String tooltip) {
            return this.tooltip(Component.literal(tooltip));
        }

        /** Component variant: a translatable tooltip resolves when it renders. */
        public Button tooltip(Component tooltip) {
            this.tooltip = tooltip;
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
