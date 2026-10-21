package net.fayber.modernconfig.dev;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.fayber.modernconfig.api.ConfigEntry;
import net.fayber.modernconfig.api.ModernConfigScreen;
import net.fayber.modernconfig.gui.ConfigEntryList;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Consumer;

/** Opt-in client smoke for selector-dependent sliders through the real Cloth bridge. */
public final class ConditionalPreview {
    private enum Mode { UNSUPPRESSED, MINIMIZED, SUPPRESSED }

    private ConditionalPreview() { }

    @SuppressWarnings("unchecked")
    public static void run(Minecraft client) {
        try {
            Mode[] savedMode = {Mode.UNSUPPRESSED};
            int[] savedCount = {2};
            var builder = ConfigBuilder.create().setTitle(Component.literal("Conditional smoke"));
            var category = builder.getOrCreateCategory(Component.literal("Effects"));
            var entries = builder.entryBuilder();
            var mode = entries.startEnumSelector(Component.literal("Mode"), Mode.class, savedMode[0])
                    .setSaveConsumer(value -> savedMode[0] = value).build();
            var count = entries.startIntSlider(Component.literal("Particles"), 2, 1, 64)
                    .setSaveConsumer(value -> savedCount[0] = value).build();
            count.setDisplayRequirement(() -> mode.getValue() == Mode.MINIMIZED);
            category.addEntry(mode);
            category.addEntry(count);
            var screen = (ModernConfigScreen) builder.build();
            client.setScreenAndShow(screen);
            List<ConfigEntry> options = (List<ConfigEntry>) field(screen, "entries");
            List<ConfigEntryList.Row> rows = (List<ConfigEntryList.Row>) field(screen, "rows");
            Consumer<Object> setMode = (Consumer<Object>) field(options.get(0), "setter");
            java.util.function.IntConsumer setCount = (java.util.function.IntConsumer) field(options.get(1), "setter");
            screen.tick();
            check(!rows.get(1).isVisible(), "unsuppressed slider hidden");
            setMode.accept(Mode.MINIMIZED);
            screen.tick();
            check(rows.get(1).isVisible(), "minimized slider visible immediately");
            setCount.accept(5);
            setMode.accept(Mode.SUPPRESSED);
            screen.tick();
            check(!rows.get(1).isVisible(), "suppressed slider hidden");
            setMode.accept(Mode.MINIMIZED);
            screen.tick();
            check(rows.get(1).isVisible(), "slider reappears");
            check(savedMode[0] == Mode.UNSUPPRESSED && savedCount[0] == 2, "edits remain staged");
            check(((java.util.function.IntSupplier) field(options.get(1), "getter")).getAsInt() == 5,
                    "count retained while hidden");
            screen.onClose();
            check(savedMode[0] == Mode.UNSUPPRESSED && savedCount[0] == 2, "cancel preserves config");
            // Re-open the translated screen, edit again, then execute its actual save callback.
            client.setScreenAndShow(screen);
            setMode.accept(Mode.MINIMIZED);
            setCount.accept(7);
            var save = ModernConfigScreen.class.getDeclaredMethod("saveAndClose");
            save.setAccessible(true);
            save.invoke(screen);
            check(savedMode[0] == Mode.MINIMIZED && savedCount[0] == 7, "save persists mode and count");
            System.out.println("CONDITIONAL_SMOKE_PASS");
            client.stop();
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.out.println("CONDITIONAL_SMOKE_FAIL");
            client.stop();
        }
    }

    private static Object field(Object object, String name) throws ReflectiveOperationException {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
