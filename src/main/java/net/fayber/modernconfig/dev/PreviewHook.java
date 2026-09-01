package net.fayber.modernconfig.dev;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fayber.modernconfig.ModernConfigClient;
import net.fayber.modernconfig.api.ModernConfigScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Function;

/**
 * Design workbench. Opens a demo config screen shortly after the dev client reaches the title
 * screen, so the look can be iterated on headlessly: run the client under Xvfb, grab a frame,
 * inspect, repeat (see {@code tools/preview.sh}). Inert unless the JVM is started with
 * {@code -Dmodernconfig.preview=true}. The same demo screen is reachable in any install via
 * {@code /modernconfig demo}.
 */
public final class PreviewHook {
    /** Theme choices for the dropdown demo, in menu order. */
    private static final String[] THEME_OPTIONS = {"Dark", "Light", "Auto"};

    /** Detail levels for the cycle demo, in cycle order. */
    private static final String[] DETAIL_OPTIONS = {"Low", "Medium", "High"};

    /** GLFW keycode for G, the toggle keybind's declared default. */
    private static final int GLFW_KEY_G = 71;

    private PreviewHook() {
    }

    public static boolean enabled() {
        return Boolean.getBoolean("modernconfig.preview");
    }

    public static void register() {
        ModernConfigClient.LOGGER.info("Modern Config preview hook armed");
        // -Dmodernconfig.previewScroll=<px> settles the demo list at that offset one second
        // after the screen opens, so captures can check rendering without timing races.
        double scrollTarget = 0.0;
        String scrollProp = System.getProperty("modernconfig.previewScroll");
        if (scrollProp != null) {
            try {
                scrollTarget = Double.parseDouble(scrollProp);
            } catch (NumberFormatException ignored) {
            }
        }
        final double target = scrollTarget;
        ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick() {
            private int ticks = 0;
            private boolean opened = false;
            private boolean scrolled = false;

            @Override
            public void onEndTick(net.minecraft.client.Minecraft client) {
                if (!this.opened) {
                    if (!(client.screen instanceof TitleScreen)) {
                        return;
                    }
                    if (++this.ticks < 20) {
                        return;
                    }
                    this.opened = true;
                    ModernConfigClient.LOGGER.info("PREVIEW: opening demo screen");
                    client.setScreen(demoScreen());
                    return;
                }
                if (target > 0.0 && !this.scrolled && ++this.ticks >= 40) {
                    this.scrolled = true;
                    if (client.screen instanceof ModernConfigScreen screen) {
                        screen.entryList().smoothScrollTo(target);
                        ModernConfigClient.LOGGER.info("PREVIEW: scroll set to " + target);
                    }
                }
            }
        });
    }

    /** A screen with one of every entry type, sized like a real mod's config. */
    public static ModernConfigScreen demoScreen() {
        DemoState s = DemoState.INSTANCE;
        return ModernConfigScreen.builder(Component.literal("Modern Config Demo"), null, () -> {
                })
                .tab("General")
                .note("One of every option type. Changes preview live while the screen is open; "
                        + "Cancel or ESC puts every value back.")
                .category("Basics")
                .bool("Enabled", () -> s.enabled, v -> s.enabled = v, true)
                .tooltip("Master switch; the slider below only responds while this is on.")
                .floatSlider("Speed", () -> s.speed, v -> s.speed = v, 0.25f, 4.0f, 0.05f, 1.0f)
                .enabledWhen(() -> s.enabled)
                .tooltip("Greyed out and ignores input while Enabled is off.")
                .cycle("Detail", () -> s.detail, v -> s.detail = v, DETAIL_OPTIONS, Function.identity(), "Medium")
                .select("Theme", () -> s.theme, v -> s.theme = v, THEME_OPTIONS, Function.identity(), "Auto")
                .text("Display Name", () -> s.displayName, v -> s.displayName = v, 32, "Player")
                .error(() -> s.displayName.isBlank() ? "Name cannot be empty." : null)
                .color("Accent Color", () -> s.accentColor, v -> s.accentColor = v, 0xFF3B82F6)
                .keybind("Toggle Key", () -> s.toggleKey, v -> s.toggleKey = v, GLFW_KEY_G)
                .tab("Advanced")
                .category("Sliders")
                .intSlider("Range", () -> s.range, v -> s.range = v, 0, 64, 1, 16)
                .requiresRestart()
                .tooltip("The change applies after a restart.")
                .doubleSlider("Scale", () -> s.scale, v -> s.scale = v, 0.5, 3.0, 0.1, 1.0)
                .category("Lists")
                .stringList("Tags", () -> s.tags, v -> s.tags = v, List.of("alpha"))
                .intList("Ports", () -> s.ports, v -> s.ports = v, List.of(80, 443))
                .floatList("Weights", () -> s.weights, v -> s.weights = v, List.of(0.5f, 1.0f))
                .doubleList("Thresholds", () -> s.thresholds, v -> s.thresholds = v, List.of(0.25, 0.75))
                .button("Sample Action", () -> {
                })
                .cornerButton("Reset Demo", s::reset)
                .build();
    }

    /** Mutable backing values for the demo screen. */
    private static final class DemoState {
        static final DemoState INSTANCE = new DemoState();

        boolean enabled = true;
        float speed = 1.0f;
        String detail = "Medium";
        String theme = "Auto";
        String displayName = "Player";
        int accentColor = 0xFF3B82F6;
        int toggleKey = 79; // GLFW_KEY_O; not the default, so the reset button shows
        int range = 16;
        double scale = 1.0;
        List<String> tags = List.of("alpha");
        List<Integer> ports = List.of(80, 443);
        List<Float> weights = List.of(0.5f, 1.0f);
        List<Double> thresholds = List.of(0.25, 0.75);

        /** Corner-button action: writes every demo value back to its declared default. */
        void reset() {
            DemoState defaults = new DemoState();
            this.enabled = defaults.enabled;
            this.speed = defaults.speed;
            this.detail = defaults.detail;
            this.theme = defaults.theme;
            this.displayName = defaults.displayName;
            this.accentColor = defaults.accentColor;
            this.toggleKey = defaults.toggleKey;
            this.range = defaults.range;
            this.scale = defaults.scale;
            this.tags = defaults.tags;
            this.ports = defaults.ports;
            this.weights = defaults.weights;
            this.thresholds = defaults.thresholds;
        }
    }
}
