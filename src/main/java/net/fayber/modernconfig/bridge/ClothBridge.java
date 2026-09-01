package net.fayber.modernconfig.bridge;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.gui.entries.ColorEntry;
import me.shedaniel.clothconfig2.gui.entries.DoubleListEntry;
import me.shedaniel.clothconfig2.gui.entries.DoubleListListEntry;
import me.shedaniel.clothconfig2.gui.entries.DropdownBoxEntry;
import me.shedaniel.clothconfig2.gui.entries.EmptyEntry;
import me.shedaniel.clothconfig2.gui.entries.EnumListEntry;
import me.shedaniel.clothconfig2.gui.entries.FloatListEntry;
import me.shedaniel.clothconfig2.gui.entries.FloatListListEntry;
import me.shedaniel.clothconfig2.gui.entries.IntegerListEntry;
import me.shedaniel.clothconfig2.gui.entries.IntegerListListEntry;
import me.shedaniel.clothconfig2.gui.entries.IntegerSliderEntry;
import me.shedaniel.clothconfig2.gui.entries.KeyCodeEntry;
import me.shedaniel.clothconfig2.gui.entries.LongListEntry;
import me.shedaniel.clothconfig2.gui.entries.LongListListEntry;
import me.shedaniel.clothconfig2.gui.entries.LongSliderEntry;
import me.shedaniel.clothconfig2.gui.entries.MultiElementListEntry;
import me.shedaniel.clothconfig2.gui.entries.SelectionListEntry;
import me.shedaniel.clothconfig2.gui.entries.StringListEntry;
import me.shedaniel.clothconfig2.gui.entries.StringListListEntry;
import me.shedaniel.clothconfig2.gui.entries.SubCategoryListEntry;
import me.shedaniel.clothconfig2.gui.entries.TextListEntry;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.fayber.moderngui.render.Theme;
import net.fayber.moderngui.widget.FlatButton;
import net.fayber.modernconfig.api.ConfigEntry;
import net.fayber.modernconfig.api.ModernConfigScreen;
import net.fayber.modernconfig.bridge.mixin.MultiElementListEntryAccessor;
import net.fayber.modernconfig.bridge.mixin.SelectionListEntryAccessor;
import net.fayber.modernconfig.bridge.mixin.TextListEntryAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
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
 * Translates a Cloth Config screen into a Modern Config screen.
 *
 * <p>Design constraint that shapes everything here: a Cloth entry publishes its current value
 * ({@code ValueHolder.getValue()}) but not the consumer that persists it, and a slider's bounds
 * live on its builder rather than on the entry. Both are reachable only while the builder runs, so
 * {@link net.fayber.modernconfig.bridge.mixin.FieldBuilderMixin} records them into
 * {@link #PLUMBING} as each entry is built, and this class reads them back when the finished
 * screen is intercepted.
 *
 * <p>Translation is all-or-nothing per screen: if a single entry kind is not understood, the
 * original Cloth screen is returned untouched. A partially translated screen would silently hide
 * options, which is worse than not translating at all.
 *
 * <p>Value flow differs from a normal Modern Config screen. Modern Config entries write live through their
 * setters, but Cloth's save consumers persist to disk, so the bridge writes edits into a staging
 * map and only runs Cloth's consumers on Save. Cancel therefore needs no restore: nothing was
 * written. The captured Cloth screen is kept so "Use original menu" can show it.
 */
public final class ClothBridge {
    /** Validators for the unbounded numeric text fields: mark bad input, never block typing. */
    private static final java.util.function.Predicate<String> INT_LIKE = s -> s.matches("-?\\d*");
    private static final java.util.function.Predicate<String> NUMBER_LIKE =
            s -> s.matches("-?\\d*(\\.\\d*)?([eE][-+]?\\d+)?");
    private static final java.util.function.Predicate<String> HEX_LIKE =
            s -> s.matches("#?[0-9a-fA-F]{0,8}");

    /**
     * Every modifier combination a Cloth key bind can carry, for the modifier cycle. The order
     * of {@code Modifier.of}'s three booleans is not documented, so combinations are deduped by
     * bit value; that also keeps the cycle's {@code equals} matching Cloth's own instances.
     */
    private static final me.shedaniel.clothconfig2.api.Modifier[] MODIFIER_COMBOS = buildModifierCombos();

    public static final Logger LOGGER = LoggerFactory.getLogger("ModernConfig/ClothBridge");

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
     * @return the Modern Config screen, or null when the screen cannot be translated faithfully (the
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
                LOGGER.info("The Cloth bridge is disabled for '{}' by its prefs (showing Cloth's own screen)",
                        title.getString());
                return null;
            }

            Map<Object, Object> staged = new java.util.HashMap<>();
            List<Runnable> onSave = new ArrayList<>();

            ModernConfigScreen.Builder builder = ModernConfigScreen.builder(title, parent, () -> {
                onSave.forEach(Runnable::run);
                if (savingRunnable != null) {
                    savingRunnable.run();
                }
            });

            int entryCount = 0;
            boolean multipleCategories = categories.size() > 1;
            for (ConfigCategory category : categories) {
                // Cloth shows multiple categories as tabs; so does the Modern Config screen. A single
                // category stays a flat list, matching Cloth. Sub categories inside a category
                // are headers, like Cloth's collapsible sections.
                if (multipleCategories) {
                    builder.tab(category.getCategoryKey().getString());
                }
                for (Object raw : category.getEntries()) {
                    entryCount++;
                    if (!(raw instanceof AbstractConfigListEntry<?> entry)) {
                        return refused(title, "an entry is not a standard Cloth entry ("
                                + raw.getClass().getName() + ")");
                    }
                    String reason = addEntry(builder, entry, staged, onSave, "");
                    if (reason != null) {
                        return refused(title, reason);
                    }
                }
            }

            if (BridgePrefs.showFallbackButton()) {
                builder.cornerButton("Use original menu",
                        () -> Minecraft.getInstance().setScreen(clothScreen));
            }

            // The screen translated cleanly, so remember how it was built: Cloth's own screen gets
            // a "Use Modern Config" button (see the screen mixins) that re-runs this translation.
            REQUESTS.put(clothScreen, new TranslationRequest(title, parent, List.copyOf(categories), savingRunnable));
            LOGGER.info("Translated the Cloth config for '{}' ({} entries in {} categories)",
                    title.getString(), entryCount, categories.size());
            return builder.build();
        } catch (Throwable t) {
            // Never let the bridge break somebody else's config screen.
            LOGGER.warn("Cloth bridge failed for '{}', falling back to the original screen", title.getString(), t);
            return null;
        }
    }

    /**
     * Logs why a screen stays on Cloth and returns null (the caller's translation abort). One
     * INFO line per refused screen: silent refusals once went undiagnosed for release builds.
     */
    @Nullable
    private static Screen refused(Component title, String reason) {
        LOGGER.info("Not translating the Cloth config for '{}': {} (showing Cloth's own screen)",
                title.getString(), reason);
        return null;
    }

    /** How a successfully translated Cloth screen was built, for the reverse button. */
    private record TranslationRequest(Component title, @Nullable Screen parent,
                                      List<ConfigCategory> categories, @Nullable Runnable savingRunnable) {
    }

    /** Cloth screens with a working translation, kept only as long as the screen lives. */
    private static final Map<Screen, TranslationRequest> REQUESTS =
            java.util.Collections.synchronizedMap(new WeakHashMap<>());

    /** True when this Cloth screen can be swapped for a Modern Config one. */
    public static boolean hasModernFor(Screen clothScreen) {
        return BridgePrefs.showFallbackButton() && REQUESTS.containsKey(clothScreen);
    }

    /** The Cloth screen mixins call this: rebuild the Modern Config screen and show it. */
    public static void openModernFor(Screen clothScreen) {
        TranslationRequest request;
        synchronized (REQUESTS) {
            request = REQUESTS.get(clothScreen);
        }
        if (request == null) {
            return;
        }
        Screen modern = translate(request.title(), request.parent(), request.categories(),
                request.savingRunnable(), clothScreen);
        if (modern != null) {
            Minecraft.getInstance().setScreen(modern);
        }
    }

    /**
     * Adds the "Use Modern Config" button to a live Cloth screen, bottom right corner. Called from
     * {@code init()} via the screen mixins; a no-op unless this screen has a stored translation.
     */
    public static void addModernButton(Screen clothScreen, Consumer<AbstractWidget> addWidget) {
        if (!hasModernFor(clothScreen)) {
            return;
        }
        try {
            Font font = Minecraft.getInstance().font;
            int w = Math.max(60, font.width("Use Modern Config") + 16);
            int h = 18;
            int x = clothScreen.width - w - 8;
            int y = clothScreen.height - h - 5;
            // Cloth's own Cancel/Save sit centred around height - 26; on a narrow window the corner
            // would reach into them, so stack above that band instead.
            if (x < clothScreen.width / 2 + 160) {
                y = clothScreen.height - 26 - h - 2;
            }
            FlatButton button = new FlatButton(x, y, w, h, Component.literal("Use Modern Config"),
                    () -> openModernFor(clothScreen), FlatButton.Style.GHOST);
            button.theme(Theme.dark());
            addWidget.accept(button);
        } catch (Throwable t) {
            LOGGER.debug("Could not add the Modern Config button to a Cloth screen", t);
        }
    }

    /**
     * Adds one Cloth entry to the Modern Config builder.
     *
     * @param prefix label prefix for entries nested in a sub category
     * @return null when the entry was translated, otherwise a short reason why it cannot be
     *         (the caller aborts the whole translation and logs it)
     */
    @Nullable
    private static String addEntry(ModernConfigScreen.Builder builder, AbstractConfigListEntry<?> entry,
                                   Map<Object, Object> staged, List<Runnable> onSave, String prefix) {
        String label = prefix + entry.getFieldName().getString();

        // Sub categories become a header plus their children, since Modern Config screens are one flat
        // scrolling column. The children carry the plumbing, not the sub category itself.
        if (entry instanceof SubCategoryListEntry sub) {
            builder.category(label);
            for (AbstractConfigListEntry<?> child : sub.getValue()) {
                String reason = addEntry(builder, child, staged, onSave, "");
                if (reason != null) {
                    return reason;
                }
            }
            return null;
        }

        // An expandable group: same flat treatment as a sub category. Groups are built directly
        // rather than through a builder, so their child list lives in a private field reached
        // through the accessor mixin.
        if (entry instanceof MultiElementListEntry<?> group) {
            builder.category(label);
            for (AbstractConfigListEntry<?> child : ((MultiElementListEntryAccessor) (Object) group).modernconfig$entries()) {
                String reason = addEntry(builder, child, staged, onSave, "");
                if (reason != null) {
                    return reason;
                }
            }
            return null;
        }

        // Static text: startTextDescription names every entry with a random UUID (see
        // TextListEntryAccessor) and the real text sits in a private field. Empty entries are
        // skipped so a UUID never leaks out as a header.
        if (entry instanceof TextListEntry textEntry) {
            Component text = ((TextListEntryAccessor) (Object) textEntry).modernconfig$text();
            if (text != null && !text.getString().isBlank()) {
                builder.note(text.getString());
            }
            return null;
        }

        // Spacer row, nothing to translate.
        if (entry instanceof EmptyEntry) {
            return null;
        }

        EntryPlumbing plumbing = PLUMBING.get(entry);
        if (plumbing == null || plumbing.saveConsumer() == null) {
            // No way to persist edits: refuse rather than render a dead control.
            return "'" + label + "' (" + kindOf(entry) + ") has no captured save consumer";
        }
        Consumer<Object> save = plumbing.saveConsumer();
        onSave.add(() -> {
            if (staged.containsKey(entry)) {
                save.accept(staged.get(entry));
            }
        });

        if (entry instanceof BooleanListEntry e) {
            builder.bool(label, () -> (Boolean) current(staged, e, e.getValue()), v -> staged.put(e, v),
                    asBoolean(defaultOf(e)));
        } else if (entry instanceof KeyCodeEntry e) {
            me.shedaniel.clothconfig2.api.ModifierKeyCode bind = e.getValue();
            if (bind == null) {
                return "'" + label + "' (key bind) has no value";
            }
            me.shedaniel.clothconfig2.api.ModifierKeyCode defaultBind =
                    defaultOf(e) instanceof me.shedaniel.clothconfig2.api.ModifierKeyCode declared ? declared : null;
            // Staged as a ModifierKeyCode copy so the key and modifier rows never clobber each other.
            builder.keybind(label,
                    () -> keyCodeOf(currentBind(staged, e, bind)),
                    code -> stageKeyCode(staged, e, currentBind(staged, e, bind), code,
                            e.isAllowKey(), e.isAllowMouse()),
                    defaultBind == null ? null : keyCodeOf(defaultBind));
            if (e.isAllowModifiers()) {
                builder.cycle(label + " modifiers",
                        () -> currentBind(staged, e, bind).getModifier(),
                        m -> staged.put(e, me.shedaniel.clothconfig2.api.ModifierKeyCode
                                .copyOf(currentBind(staged, e, bind)).setModifier(m)),
                        MODIFIER_COMBOS, ClothBridge::modifierName,
                        defaultBind == null ? null : defaultBind.getModifier());
            }
        } else if (entry instanceof ColorEntry e) {
            // Cloth's own widget also edits hex text; 6-digit input is opaque, 8-digit carries
            // the alpha byte through, and only parseable values are staged.
            builder.text(label,
                    () -> ConfigEntry.hexOf(((Number) current(staged, e, e.getValue())).intValue()),
                    v -> ConfigEntry.parseHexColor(v).ifPresent(i -> staged.put(e, i)),
                    10, HEX_LIKE, asHex(defaultOf(e)));
        } else if (entry instanceof IntegerSliderEntry e) {
            int min = intOf(plumbing.min(), 0);
            int max = intOf(plumbing.max(), 100);
            builder.intSlider(label, () -> (Integer) current(staged, e, e.getValue()),
                    v -> staged.put(e, v), min, max, 1, asInt(defaultOf(e)));
        } else if (entry instanceof LongSliderEntry e) {
            long min = longOf(plumbing.min(), 0L);
            long max = longOf(plumbing.max(), 100L);
            if (min >= Integer.MIN_VALUE && max <= Integer.MAX_VALUE) {
                builder.intSlider(label, () -> ((Number) current(staged, e, e.getValue())).intValue(),
                        v -> staged.put(e, (long) v), (int) min, (int) max, 1, asInt(defaultOf(e)));
            } else {
                // Modern Config has no long slider; a range beyond int becomes a text field, which still
                // shows and saves the exact value instead of refusing the whole screen.
                builder.text(label, () -> String.valueOf(current(staged, e, e.getValue())),
                        v -> parseLong(v).ifPresent(l -> staged.put(e, l)), 20, INT_LIKE,
                        asString(defaultOf(e)));
            }
        } else if (entry instanceof IntegerListEntry e) {
            int min = intOf(plumbing.min(), Integer.MIN_VALUE);
            int max = intOf(plumbing.max(), Integer.MAX_VALUE);
            if (min == Integer.MIN_VALUE || max == Integer.MAX_VALUE) {
                // Unbounded: a slider would be meaningless, so this falls back to a text field.
                builder.text(label, () -> String.valueOf(current(staged, e, e.getValue())),
                        v -> parseInt(v).ifPresent(i -> staged.put(e, i)), 12, INT_LIKE,
                        asString(defaultOf(e)));
            } else {
                builder.intSlider(label, () -> (Integer) current(staged, e, e.getValue()),
                        v -> staged.put(e, v), min, max, 1, asInt(defaultOf(e)));
            }
        } else if (entry instanceof LongListEntry e) {
            builder.text(label, () -> String.valueOf(current(staged, e, e.getValue())),
                    v -> parseLong(v).ifPresent(l -> staged.put(e, l)), 20, INT_LIKE,
                    asString(defaultOf(e)));
        } else if (entry instanceof FloatListEntry e) {
            float min = floatOf(plumbing.min(), Float.NEGATIVE_INFINITY);
            float max = floatOf(plumbing.max(), Float.POSITIVE_INFINITY);
            if (Float.isInfinite(min) || Float.isInfinite(max)) {
                builder.text(label, () -> String.valueOf(current(staged, e, e.getValue())),
                        v -> parseFloat(v).ifPresent(f -> staged.put(e, f)), 16, NUMBER_LIKE,
                        asString(defaultOf(e)));
            } else {
                builder.floatSlider(label, () -> (Float) current(staged, e, e.getValue()),
                        v -> staged.put(e, v), min, max, step(min, max), asFloat(defaultOf(e)));
            }
        } else if (entry instanceof DoubleListEntry e) {
            double min = doubleOf(plumbing.min(), Double.NEGATIVE_INFINITY);
            double max = doubleOf(plumbing.max(), Double.POSITIVE_INFINITY);
            if (Double.isInfinite(min) || Double.isInfinite(max)) {
                builder.text(label, () -> String.valueOf(current(staged, e, e.getValue())),
                        v -> parseDouble(v).ifPresent(d -> staged.put(e, d)), 20, NUMBER_LIKE,
                        asString(defaultOf(e)));
            } else {
                builder.doubleSlider(label, () -> (Double) current(staged, e, e.getValue()),
                        v -> staged.put(e, v), min, max, step(min, max), asDouble(defaultOf(e)));
            }
        } else if (entry instanceof StringListEntry e) {
            builder.text(label, () -> (String) current(staged, e, e.getValue()),
                    v -> staged.put(e, v), 256, asString(defaultOf(e)));
        } else if (entry instanceof StringListListEntry e) {
            // The list editor becomes a multi-line text area, one item per line. Items keep
            // their order and are rebuilt from the lines on every edit; empty lists come back
            // as one empty line, matching Cloth's own editor, which always shows one cell.
            List<String> value = e.getValue();
            builder.stringList(label,
                    () -> stringListOf(staged, e, value),
                    v -> staged.put(e, v),
                    asStringList(defaultOf(e)));
        } else if (entry instanceof IntegerListListEntry e) {
            builder.intList(label,
                    () -> listOf(staged, e, e.getValue(), n -> ((Number) n).intValue()),
                    v -> staged.put(e, v), asIntList(defaultOf(e)));
        } else if (entry instanceof FloatListListEntry e) {
            builder.floatList(label,
                    () -> listOf(staged, e, e.getValue(), n -> ((Number) n).floatValue()),
                    v -> staged.put(e, v), asFloatList(defaultOf(e)));
        } else if (entry instanceof DoubleListListEntry e) {
            builder.doubleList(label,
                    () -> listOf(staged, e, e.getValue(), n -> ((Number) n).doubleValue()),
                    v -> staged.put(e, v), asDoubleList(defaultOf(e)));
        } else if (entry instanceof LongListListEntry e) {
            // Modern Config has no long list editor: same one-item-per-line text area as the native
            // number lists, parsed to Long so the erased Cloth consumer receives its type.
            addLinesList(builder, label, e, staged, ClothBridge::boxedLong);
        } else if (entry instanceof EnumListEntry<?> e) {
            // Checked before the SelectionListEntry parent: only an enum entry is guaranteed to
            // hold every constant of its type, a plain selector may expose an arbitrary subset.
            Object value = e.getValue();
            if (value == null) {
                return "'" + label + "' (enum) has no value";
            }
            // An enum constant with a body is an anonymous subclass, whose isEnum() is false and
            // whose getEnumConstants() is null; getDeclaringClass() gives the real enum type.
            Object[] values = ((Enum<?>) value).getDeclaringClass().getEnumConstants();
            if (values == null || values.length == 0) {
                return "'" + label + "' (enum) has no constants";
            }
            addCycle(builder, label, e, values, staged);
        } else if (entry instanceof SelectionListEntry<?> e) {
            // A non-enum selector: the values live in a private field with no public read path,
            // reached through the accessor mixin.
            List<?> values = ((SelectionListEntryAccessor) (Object) e).modernconfig$values();
            if (values == null || values.isEmpty()) {
                return "'" + label + "' (selector) has no values";
            }
            addCycle(builder, label, e, values.toArray(), staged);
        } else if (entry instanceof DropdownBoxEntry<?> e) {
            List<?> selections = e.getSelections();
            if (selections == null || selections.isEmpty()) {
                return "'" + label + "' (dropdown) has no options";
            }
            addCycle(builder, label, e, selections.toArray(), staged);
        } else {
            // Third-party entry kind, a future Cloth version, or NestedListListEntry (no Cloth
            // builder constructs it, so it never gets a save consumer either way).
            // TODO: nested lists have no Modern Config equivalent, this always refuses the whole screen.
            return "'" + label + "' is a " + kindOf(entry) + ", which is not supported";
        }

        tooltipOf(entry).ifPresent(builder::tooltip);
        return null;
    }

    /**
     * The entry kind for log messages. Anonymous subclasses (mod proxies) have an empty simple
     * name, which read as "()" in refusals; the full name at least says where it came from.
     */
    private static String kindOf(Object entry) {
        String simple = entry.getClass().getSimpleName();
        return simple.isEmpty() ? entry.getClass().getName() : simple;
    }

    private static void addCycle(ModernConfigScreen.Builder builder, String label, AbstractConfigListEntry<?> entry,
                                 Object[] values, Map<Object, Object> staged) {
        builder.cycle(label,
                () -> current(staged, entry, ((me.shedaniel.clothconfig2.api.ValueHolder<?>) entry).getValue()),
                v -> staged.put(entry, v),
                values,
                ClothBridge::displayName,
                defaultOf(entry));
    }

    /** The staged edit if the user changed this entry, otherwise Cloth's own current value. */
    private static Object current(Map<Object, Object> staged, Object entry, Object clothValue) {
        return staged.getOrDefault(entry, clothValue);
    }

    /** The staged key bind (a ModifierKeyCode copy) or Cloth's own. */
    private static me.shedaniel.clothconfig2.api.ModifierKeyCode currentBind(
            Map<Object, Object> staged, Object entry, me.shedaniel.clothconfig2.api.ModifierKeyCode fallback) {
        Object value = staged.get(entry);
        return value instanceof me.shedaniel.clothconfig2.api.ModifierKeyCode bind ? bind : fallback;
    }

    /**
     * The staged list or Cloth's own, as one line per item. Works for string lists and boxed
     * number lists alike, which the raw staged values (erased to List) would not distinguish.
     */
    private static List<String> stringListOf(Map<Object, Object> staged, Object entry, @Nullable Object fallback) {
        Object value = staged.get(entry);
        List<?> list = value instanceof List<?> stagedList ? stagedList
                : fallback instanceof List<?> fallbackList ? fallbackList : List.of();
        List<String> lines = new ArrayList<>(list.size());
        for (Object element : list) {
            lines.add(String.valueOf(element));
        }
        return lines;
    }

    /**
     * Renders any entry whose value is a list of parseable items as a one-item-per-line text
     * area. Edits are staged only when every line parses (all or nothing, like Cloth's own
     * editors: a half-typed line never reaches Save).
     */
    private static void addLinesList(ModernConfigScreen.Builder builder, String label,
                                     AbstractConfigListEntry<?> entry, Map<Object, Object> staged,
                                     java.util.function.Function<String, java.util.Optional<?>> parse) {
        builder.stringList(label,
                () -> stringListOf(staged, entry, entry.getValue()),
                value -> {
                    List<Object> parsed = new ArrayList<>(value.size());
                    for (String line : value) {
                        java.util.Optional<?> element = parse.apply(line);
                        if (element.isEmpty()) {
                            return;
                        }
                        parsed.add(element.get());
                    }
                    staged.put(entry, parsed);
                },
                asStringList(defaultOf(entry)));
    }

    /**
     * The entry's declared default, or null when Cloth does not expose one. Read defensively:
     * default suppliers are consumer code and may assume a live screen; a failure only costs
     * this entry its reset button, never the whole translation.
     */
    @Nullable
    private static Object defaultOf(AbstractConfigListEntry<?> entry) {
        try {
            return entry.getDefaultValue().orElse(null);
        } catch (Throwable t) {
            return null;
        }
    }

    private static @Nullable Boolean asBoolean(@Nullable Object value) {
        return value instanceof Boolean b ? b : null;
    }

    private static @Nullable Integer asInt(@Nullable Object value) {
        return value instanceof Number n ? n.intValue() : null;
    }

    private static @Nullable Float asFloat(@Nullable Object value) {
        return value instanceof Number n ? n.floatValue() : null;
    }

    private static @Nullable Double asDouble(@Nullable Object value) {
        return value instanceof Number n ? n.doubleValue() : null;
    }

    private static @Nullable String asString(@Nullable Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static @Nullable String asHex(@Nullable Object value) {
        return value instanceof Number n ? ConfigEntry.hexOf(n.intValue()) : null;
    }

    /** A Cloth default list as display lines; null when the default is not a list. */
    private static @Nullable List<String> asStringList(@Nullable Object value) {
        if (!(value instanceof List<?> list)) {
            return null;
        }
        List<String> out = new ArrayList<>(list.size());
        for (Object element : list) {
            out.add(String.valueOf(element));
        }
        return out;
    }

    private static @Nullable List<Integer> asIntList(@Nullable Object value) {
        return asNumberList(value, Number::intValue);
    }

    private static @Nullable List<Float> asFloatList(@Nullable Object value) {
        return asNumberList(value, Number::floatValue);
    }

    private static @Nullable List<Double> asDoubleList(@Nullable Object value) {
        return asNumberList(value, Number::doubleValue);
    }

    /** A Cloth default list mapped to a native editor's element type; null on any non-number. */
    private static <T> @Nullable List<T> asNumberList(@Nullable Object value,
                                                      java.util.function.Function<Number, T> map) {
        if (!(value instanceof List<?> list)) {
            return null;
        }
        List<T> out = new ArrayList<>(list.size());
        for (Object element : list) {
            if (!(element instanceof Number n)) {
                return null;
            }
            out.add(map.apply(n));
        }
        return out;
    }

    /** The staged list or Cloth's own, element-mapped to the native editor's element type. */
    private static <T> List<T> listOf(Map<Object, Object> staged, Object entry, @Nullable Object fallback,
                                      java.util.function.Function<Object, T> map) {
        Object value = staged.get(entry);
        List<?> raw = value instanceof List<?> stagedList ? stagedList
                : fallback instanceof List<?> fallbackList ? fallbackList : List.of();
        List<T> out = new ArrayList<>(raw.size());
        for (Object element : raw) {
            out.add(map.apply(element));
        }
        return out;
    }

    /** Cloth bind to toolkit code: GLFW keycodes as-is, mouse buttons at 1000 + button. */
    private static int keyCodeOf(me.shedaniel.clothconfig2.api.ModifierKeyCode bind) {
        com.mojang.blaze3d.platform.InputConstants.Key key = bind.getKeyCode();
        if (key == null) {
            return -1;
        }
        if (key.getType() == com.mojang.blaze3d.platform.InputConstants.Type.MOUSE) {
            return net.fayber.moderngui.widget.KeybindField.MOUSE_CODE_BASE + key.getValue();
        }
        if (key.getType() == com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM) {
            return key.getValue();
        }
        // Scan codes have no faithful int form in the toolkit's convention: shows as unbound,
        // and rebinding writes a keysym instead, which is what mods persist anyway.
        return -1;
    }

    /** Toolkit code to Cloth bind, honouring what the entry is allowed to accept. */
    private static void stageKeyCode(Map<Object, Object> staged, Object entry,
                                     me.shedaniel.clothconfig2.api.ModifierKeyCode current, int code,
                                     boolean allowKey, boolean allowMouse) {
        boolean mouse = code >= net.fayber.moderngui.widget.KeybindField.MOUSE_CODE_BASE;
        if (mouse ? !allowMouse : !allowKey) {
            return;
        }
        var key = mouse
                ? com.mojang.blaze3d.platform.InputConstants.Type.MOUSE
                        .getOrCreate(code - net.fayber.moderngui.widget.KeybindField.MOUSE_CODE_BASE)
                : com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(code);
        staged.put(entry, me.shedaniel.clothconfig2.api.ModifierKeyCode.copyOf(current).setKeyCode(key));
    }

    /** All eight modifier combinations, deduped by bit value. */
    private static me.shedaniel.clothconfig2.api.Modifier[] buildModifierCombos() {
        List<me.shedaniel.clothconfig2.api.Modifier> out = new ArrayList<>();
        out.add(me.shedaniel.clothconfig2.api.Modifier.none());
        for (boolean alt : new boolean[]{false, true}) {
            for (boolean ctrl : new boolean[]{false, true}) {
                for (boolean shift : new boolean[]{false, true}) {
                    if (!alt && !ctrl && !shift) {
                        continue; // none() is already in the list
                    }
                    me.shedaniel.clothconfig2.api.Modifier m =
                            me.shedaniel.clothconfig2.api.Modifier.of(alt, ctrl, shift);
                    boolean duplicate = false;
                    for (me.shedaniel.clothconfig2.api.Modifier existing : out) {
                        duplicate |= existing.getValue() == m.getValue();
                    }
                    if (!duplicate) {
                        out.add(m);
                    }
                }
            }
        }
        return out.toArray(new me.shedaniel.clothconfig2.api.Modifier[0]);
    }

    private static String modifierName(me.shedaniel.clothconfig2.api.Modifier modifier) {
        if (modifier.isEmpty()) {
            return "None";
        }
        List<String> parts = new ArrayList<>();
        if (modifier.hasControl()) {
            parts.add("Ctrl");
        }
        if (modifier.hasAlt()) {
            parts.add("Alt");
        }
        if (modifier.hasShift()) {
            parts.add("Shift");
        }
        return String.join(" + ", parts);
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

    /** Cloth tooltips are an array of lines; Modern Config takes one string. */
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

    /** Same parse, boxed: the staged list must carry the element type the erased consumer expects. */
    private static java.util.Optional<?> boxedLong(String text) {
        java.util.OptionalLong parsed = parseLong(text);
        return parsed.isPresent() ? java.util.Optional.of(parsed.getAsLong()) : java.util.Optional.empty();
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
