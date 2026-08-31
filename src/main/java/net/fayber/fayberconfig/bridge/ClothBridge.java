package net.fayber.fayberconfig.bridge;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.gui.entries.DoubleListEntry;
import me.shedaniel.clothconfig2.gui.entries.DropdownBoxEntry;
import me.shedaniel.clothconfig2.gui.entries.EnumListEntry;
import me.shedaniel.clothconfig2.gui.entries.FloatListEntry;
import me.shedaniel.clothconfig2.gui.entries.IntegerListEntry;
import me.shedaniel.clothconfig2.gui.entries.IntegerSliderEntry;
import me.shedaniel.clothconfig2.gui.entries.LongListEntry;
import me.shedaniel.clothconfig2.gui.entries.LongSliderEntry;
import me.shedaniel.clothconfig2.gui.entries.StringListEntry;
import me.shedaniel.clothconfig2.gui.entries.SubCategoryListEntry;
import me.shedaniel.clothconfig2.gui.entries.TextListEntry;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.fayber.fayberconfig.api.FayberConfigScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

/**
 * Translates a Cloth Config screen into a Fayber Config screen.
 *
 * <p>Design constraint that shapes everything here: a Cloth entry publishes its current value
 * ({@code ValueHolder.getValue()}) but not the consumer that persists it, and a slider's bounds
 * live on its builder rather than on the entry. Both are reachable only while the builder runs, so
 * {@link net.fayber.fayberconfig.bridge.mixin.FieldBuilderMixin} records them into
 * {@link #PLUMBING} as each entry is built, and this class reads them back when the finished
 * screen is intercepted.
 *
 * <p>Translation is all-or-nothing per screen: if a single entry kind is not understood, the
 * original Cloth screen is returned untouched. A partially translated screen would silently hide
 * options, which is worse than not translating at all.
 *
 * <p>Value flow differs from a normal Fayber screen. Fayber entries write live through their
 * setters, but Cloth's save consumers persist to disk, so the bridge writes edits into a staging
 * map and only runs Cloth's consumers on Save. Cancel therefore needs no restore: nothing was
 * written. The captured Cloth screen is kept so "Use original menu" can show it.
 */
public final class ClothBridge {
    public static final Logger LOGGER = LoggerFactory.getLogger("FayberConfig/ClothBridge");

    /**
     * Plumbing captured at build time, keyed by the entry it belongs to. Weak keys: entries live
     * as long as their screen, and a screen that is never translated must not be pinned in memory.
     */
    public static final Map<Object, EntryPlumbing> PLUMBING = java.util.Collections.synchronizedMap(new WeakHashMap<>());

    private ClothBridge() {
    }

    /**
     * Attempts to translate a built Cloth screen.
     *
     * @return the Fayber screen, or null when the screen cannot be translated faithfully (the
     *         caller then keeps Cloth's own screen)
     */
    @Nullable
    // ConfigCategory.getEntries() is deprecated for returning a raw List<Object>, but Cloth offers
    // no replacement: it is the only way to read a category's contents.
    @SuppressWarnings("deprecation")
    public static Screen translate(Component title, @Nullable Screen parent,
                                   Collection<ConfigCategory> categories, @Nullable Runnable savingRunnable,
                                   Screen clothScreen) {
        try {
            if (!BridgePrefs.enabledFor(title.getString())) {
                return null;
            }

            // Edits live here until Save; Cloth's consumers only run then.
            Map<Object, Object> staged = new java.util.HashMap<>();
            List<Runnable> onSave = new ArrayList<>();

            FayberConfigScreen.Builder builder = FayberConfigScreen.builder(title, parent, () -> {
                onSave.forEach(Runnable::run);
                if (savingRunnable != null) {
                    savingRunnable.run();
                }
            });

            boolean multipleCategories = categories.size() > 1;
            for (ConfigCategory category : categories) {
                if (multipleCategories) {
                    builder.category(category.getCategoryKey().getString());
                }
                for (Object raw : category.getEntries()) {
                    if (!(raw instanceof AbstractConfigListEntry<?> entry)) {
                        return null;
                    }
                    if (!addEntry(builder, entry, staged, onSave, "")) {
                        return null;
                    }
                }
            }

            if (BridgePrefs.showFallbackButton()) {
                builder.button("Use original menu",
                        () -> net.minecraft.client.Minecraft.getInstance().setScreen(clothScreen));
                builder.tooltip("Open this mod's own Cloth Config screen instead.");
            }

            return builder.build();
        } catch (Throwable t) {
            // Never let the bridge break somebody else's config screen.
            LOGGER.warn("Cloth bridge failed for '{}', falling back to the original screen", title.getString(), t);
            return null;
        }
    }

    /**
     * Adds one Cloth entry to the Fayber builder.
     *
     * @param prefix label prefix for entries nested in a sub category
     * @return false when the entry kind is not supported (caller aborts the whole translation)
     */
    private static boolean addEntry(FayberConfigScreen.Builder builder, AbstractConfigListEntry<?> entry,
                                    Map<Object, Object> staged, List<Runnable> onSave, String prefix) {
        String label = prefix + entry.getFieldName().getString();

        // Sub categories become a header plus their children, since Fayber screens are one flat
        // scrolling column. The children carry the plumbing, not the sub category itself.
        if (entry instanceof SubCategoryListEntry sub) {
            builder.category(label);
            for (AbstractConfigListEntry<?> child : sub.getValue()) {
                if (!addEntry(builder, child, staged, onSave, "")) {
                    return false;
                }
            }
            return true;
        }

        // Static text: a label row with no value. Fayber has no text-only entry, so it becomes a
        // header, which reads correctly for the section captions these are normally used for.
        if (entry instanceof TextListEntry) {
            builder.category(label);
            return true;
        }

        EntryPlumbing plumbing = PLUMBING.get(entry);
        if (plumbing == null || plumbing.saveConsumer() == null) {
            // No way to persist edits: refuse rather than render a dead control.
            return false;
        }
        Consumer<Object> save = plumbing.saveConsumer();
        onSave.add(() -> {
            if (staged.containsKey(entry)) {
                save.accept(staged.get(entry));
            }
        });

        if (entry instanceof BooleanListEntry e) {
            builder.bool(label, () -> (Boolean) current(staged, e, e.getValue()), v -> staged.put(e, v));
        } else if (entry instanceof IntegerSliderEntry e) {
            int min = intOf(plumbing.min(), 0);
            int max = intOf(plumbing.max(), 100);
            builder.intSlider(label, () -> (Integer) current(staged, e, e.getValue()),
                    v -> staged.put(e, v), min, max, 1);
        } else if (entry instanceof LongSliderEntry e) {
            // Fayber has no long slider; a long range that exceeds int cannot be shown faithfully.
            long min = longOf(plumbing.min(), 0L);
            long max = longOf(plumbing.max(), 100L);
            if (min < Integer.MIN_VALUE || max > Integer.MAX_VALUE) {
                return false;
            }
            builder.intSlider(label, () -> ((Number) current(staged, e, e.getValue())).intValue(),
                    v -> staged.put(e, (long) v), (int) min, (int) max, 1);
        } else if (entry instanceof IntegerListEntry e) {
            int min = intOf(plumbing.min(), Integer.MIN_VALUE);
            int max = intOf(plumbing.max(), Integer.MAX_VALUE);
            if (min == Integer.MIN_VALUE || max == Integer.MAX_VALUE) {
                // Unbounded: a slider would be meaningless, so show it as a text field.
                builder.text(label, () -> String.valueOf(current(staged, e, e.getValue())),
                        v -> parseInt(v).ifPresent(i -> staged.put(e, i)), 12);
            } else {
                builder.intSlider(label, () -> (Integer) current(staged, e, e.getValue()),
                        v -> staged.put(e, v), min, max, 1);
            }
        } else if (entry instanceof LongListEntry e) {
            builder.text(label, () -> String.valueOf(current(staged, e, e.getValue())),
                    v -> parseLong(v).ifPresent(l -> staged.put(e, l)), 20);
        } else if (entry instanceof FloatListEntry e) {
            float min = floatOf(plumbing.min(), Float.NEGATIVE_INFINITY);
            float max = floatOf(plumbing.max(), Float.POSITIVE_INFINITY);
            if (Float.isInfinite(min) || Float.isInfinite(max)) {
                builder.text(label, () -> String.valueOf(current(staged, e, e.getValue())),
                        v -> parseFloat(v).ifPresent(f -> staged.put(e, f)), 16);
            } else {
                builder.floatSlider(label, () -> (Float) current(staged, e, e.getValue()),
                        v -> staged.put(e, v), min, max, step(min, max));
            }
        } else if (entry instanceof DoubleListEntry e) {
            double min = doubleOf(plumbing.min(), Double.NEGATIVE_INFINITY);
            double max = doubleOf(plumbing.max(), Double.POSITIVE_INFINITY);
            if (Double.isInfinite(min) || Double.isInfinite(max)) {
                builder.text(label, () -> String.valueOf(current(staged, e, e.getValue())),
                        v -> parseDouble(v).ifPresent(d -> staged.put(e, d)), 20);
            } else {
                builder.doubleSlider(label, () -> (Double) current(staged, e, e.getValue()),
                        v -> staged.put(e, v), min, max, step(min, max));
            }
        } else if (entry instanceof StringListEntry e) {
            builder.text(label, () -> (String) current(staged, e, e.getValue()),
                    v -> staged.put(e, v), 256);
        } else if (entry instanceof EnumListEntry<?> e) {
            // Only EnumListEntry, not its SelectionListEntry parent: Cloth fills an enum entry with
            // every constant of the type, but a plain SelectionListEntry may hold an arbitrary
            // subset that the entry does not expose, and offering the full enum there would let the
            // user pick values the mod never allowed.
            Object value = e.getValue();
            if (value == null) {
                return false;
            }
            // An enum constant with a body is an anonymous subclass, whose isEnum() is false and
            // whose getEnumConstants() is null; getDeclaringClass() gives the real enum type.
            Object[] values = ((Enum<?>) value).getDeclaringClass().getEnumConstants();
            if (values == null || values.length == 0) {
                return false;
            }
            addCycle(builder, label, e, values, staged);
        } else if (entry instanceof DropdownBoxEntry<?> e) {
            List<?> selections = e.getSelections();
            if (selections == null || selections.isEmpty()) {
                return false;
            }
            addCycle(builder, label, e, selections.toArray(), staged);
        } else {
            // KeyCodeEntry, ColorEntry, the *ListListEntry list editors and anything a future Cloth
            // version adds: not translated yet, so the whole screen stays Cloth's.
            return false;
        }

        tooltipOf(entry).ifPresent(builder::tooltip);
        return true;
    }

    private static void addCycle(FayberConfigScreen.Builder builder, String label, Object entry,
                                 Object[] values, Map<Object, Object> staged) {
        builder.cycle(label,
                () -> current(staged, entry, ((me.shedaniel.clothconfig2.api.ValueHolder<?>) entry).getValue()),
                v -> staged.put(entry, v),
                values,
                ClothBridge::displayName);
    }

    /** The staged edit if the user changed this entry, otherwise Cloth's own current value. */
    private static Object current(Map<Object, Object> staged, Object entry, Object clothValue) {
        return staged.getOrDefault(entry, clothValue);
    }

    /** Enum constants read better title-cased than SCREAMING_CASE. */
    private static String displayName(Object value) {
        if (value == null) {
            return "";
        }
        if (!(value instanceof Enum<?> e)) {
            return String.valueOf(value);
        }
        String[] words = e.name().toLowerCase(java.util.Locale.ROOT).split("_");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(word.charAt(0))).append(word, 1, word.length());
        }
        return out.toString();
    }

    /** Cloth tooltips are an array of lines; Fayber takes one string. */
    private static java.util.Optional<String> tooltipOf(AbstractConfigListEntry<?> entry) {
        if (!(entry instanceof TooltipListEntry<?> tooltipEntry)) {
            return java.util.Optional.empty();
        }
        try {
            return tooltipEntry.getTooltip()
                    .filter(lines -> lines.length > 0)
                    .map(lines -> String.join(" ", java.util.Arrays.stream(lines)
                            .map(Component::getString).toList()));
        } catch (RuntimeException e) {
            // Tooltip suppliers are consumer code and may assume a live screen.
            return java.util.Optional.empty();
        }
    }

    /** A step that gives a slider ~200 positions over its range, rounded to something readable. */
    private static float step(float min, float max) {
        float span = max - min;
        if (span <= 2.0f) {
            return 0.01f;
        }
        return span <= 20.0f ? 0.1f : 1.0f;
    }

    private static double step(double min, double max) {
        double span = max - min;
        if (span <= 2.0) {
            return 0.01;
        }
        return span <= 20.0 ? 0.1 : 1.0;
    }

    private static int intOf(@Nullable Object value, int fallback) {
        return value instanceof Number n ? n.intValue() : fallback;
    }

    private static long longOf(@Nullable Object value, long fallback) {
        return value instanceof Number n ? n.longValue() : fallback;
    }

    private static float floatOf(@Nullable Object value, float fallback) {
        return value instanceof Number n ? n.floatValue() : fallback;
    }

    private static double doubleOf(@Nullable Object value, double fallback) {
        return value instanceof Number n ? n.doubleValue() : fallback;
    }

    private static java.util.OptionalInt parseInt(String text) {
        try {
            return java.util.OptionalInt.of(Integer.parseInt(text.trim()));
        } catch (NumberFormatException e) {
            return java.util.OptionalInt.empty();
        }
    }

    private static java.util.OptionalLong parseLong(String text) {
        try {
            return java.util.OptionalLong.of(Long.parseLong(text.trim()));
        } catch (NumberFormatException e) {
            return java.util.OptionalLong.empty();
        }
    }

    private static java.util.Optional<Float> parseFloat(String text) {
        try {
            return java.util.Optional.of(Float.parseFloat(text.trim()));
        } catch (NumberFormatException e) {
            return java.util.Optional.empty();
        }
    }

    private static java.util.Optional<Double> parseDouble(String text) {
        try {
            return java.util.Optional.of(Double.parseDouble(text.trim()));
        } catch (NumberFormatException e) {
            return java.util.Optional.empty();
        }
    }
}
