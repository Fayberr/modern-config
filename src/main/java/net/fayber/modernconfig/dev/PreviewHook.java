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
        // -Dmodernconfig.previewTab=<index> switches to that tab once the screen is up.
        final int tabTarget = Integer.getInteger("modernconfig.previewTab", -1);
        // -Dmodernconfig.previewClick=<x>,<y> (GUI px) dispatches one synthetic left click at
        // that screen position through Screen.mouseClicked, the same path a real click takes.
        double clickX = -1;
        double clickY = -1;
        String clickProp = System.getProperty("modernconfig.previewClick");
        if (clickProp != null) {
            String[] parts = clickProp.split(",");
            if (parts.length == 2) {
                try {
                    clickX = Double.parseDouble(parts[0].trim());
                    clickY = Double.parseDouble(parts[1].trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        final double cx = clickX;
        final double cy = clickY;
        // -Dmodernconfig.previewClick2=<x>,<y> fires 30 ticks after the first click, so a run can
        // open the picker and then click inside the SV square at an arbitrary (drag-like) position.
        double click2X = -1;
        double click2Y = -1;
        String click2Prop = System.getProperty("modernconfig.previewClick2");
        if (click2Prop != null) {
            String[] parts = click2Prop.split(",");
            if (parts.length == 2) {
                try {
                    click2X = Double.parseDouble(parts[0].trim());
                    click2Y = Double.parseDouble(parts[1].trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        final double c2x = click2X;
        final double c2y = click2Y;
        ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick() {
            private int ticks = 0;
            private boolean opened = false;
            private boolean tabbed = false;
            private boolean scrolled = false;
            private boolean clicked = false;
            private boolean clicked2 = false;

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
                this.ticks++;
                if (tabTarget >= 0 && !this.tabbed && this.ticks >= 30) {
                    this.tabbed = true;
                    if (client.screen instanceof ModernConfigScreen screen) {
                        screen.selectTab(tabTarget);
                        ModernConfigClient.LOGGER.info("PREVIEW: tab set to " + tabTarget);
                    }
                }
                if (target > 0.0 && !this.scrolled && this.ticks >= 45) {
                    this.scrolled = true;
                    if (client.screen instanceof ModernConfigScreen screen) {
                        screen.entryList().smoothScrollTo(target);
                        ModernConfigClient.LOGGER.info("PREVIEW: scroll set to " + target);
                    }
                }
                if (cx >= 0 && !this.clicked && this.ticks >= 60) {
                    this.clicked = true;
                    if (client.screen instanceof ModernConfigScreen screen) {
                        var event = new net.minecraft.client.input.MouseButtonEvent(cx, cy,
                                new net.minecraft.client.input.MouseButtonInfo(
                                        org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT, 0));
                        boolean taken = screen.mouseClicked(event, false);
                        ModernConfigClient.LOGGER
                                .info("PREVIEW: click at " + cx + "," + cy + " -> " + taken);
                    }
                }
                if (c2x >= 0 && !this.clicked2 && this.ticks >= 90) {
                    this.clicked2 = true;
                    if (client.screen instanceof ModernConfigScreen screen) {
                        var event = new net.minecraft.client.input.MouseButtonEvent(c2x, c2y,
                                new net.minecraft.client.input.MouseButtonInfo(
                                        org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT, 0));
                        boolean taken = screen.mouseClicked(event, false);
                        ModernConfigClient.LOGGER
                                .info("PREVIEW: click2 at " + c2x + "," + c2y + " -> " + taken);
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
                        + "click the colour swatch to open the picker, and Cancel or ESC puts "
                        + "every value back.")
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
                .bool("High Contrast", () -> s.highContrast, v -> s.highContrast = v, true)
                .requiresRestart()
                .tooltip("The change applies after a restart.")
                .tab("Advanced")
                .category("Sliders")
                .intSlider("Range", () -> s.range, v -> s.range = v, 0, 64, 1, 16)
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
        boolean highContrast = true;
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
            this.highContrast = defaults.highContrast;
            this.range = defaults.range;
            this.scale = defaults.scale;
            this.tags = defaults.tags;
            this.ports = defaults.ports;
            this.weights = defaults.weights;
            this.thresholds = defaults.thresholds;
        }
    }
}
