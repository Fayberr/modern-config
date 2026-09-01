package net.fayber.fayberconfig.dev;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fayber.fayberconfig.FayberConfigClient;
import net.fayber.fayberconfig.api.FayberConfigScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Function;

/**
 * Design workbench. Opens a demo config screen automatically shortly after the dev client reaches
 * the title screen, so the look can be iterated on headlessly: run the client under Xvfb, grab a
 * frame, inspect the pixels, repeat (see {@code tools/preview.sh}).
 *
 * <p>Completely inert unless the JVM is started with {@code -Dfayberconfig.preview=true}, so this
 * class costs shipped builds one boolean check at init. The same demo screen is reachable in any
 * install through the {@code /fayberconfig demo} command, which is how users exercise every
 * entry type (and the reset-to-default buttons) on a real instance.
 */
public final class PreviewHook {
    /** Sort options for the dropdown demo, in menu order. */
    private static final String[] SORT_OPTIONS = {"Distance", "Name", "Created", "Dimension"};

    private PreviewHook() {
    }

    public static boolean enabled() {
        return Boolean.getBoolean("fayberconfig.preview");
    }

    public static void register() {
        FayberConfigClient.LOGGER.info("Fayber Config preview hook armed");
        // Optional scroll-offset support: -Dfayberconfig.previewScroll=<px> settles the demo
        // list at that scroll offset one second after the screen opens, so captures can check
        // sub-pixel rendering without timing races.
        double scrollTarget = 0.0;
        String scrollProp = System.getProperty("fayberconfig.previewScroll");
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
                    FayberConfigClient.LOGGER.info("PREVIEW: opening demo screen");
                    client.setScreen(demoScreen());
                    return;
                }
                if (target > 0.0 && !this.scrolled && ++this.ticks >= 40) {
                    this.scrolled = true;
                    if (client.screen instanceof FayberConfigScreen screen) {
                        screen.entryList().smoothScrollTo(target);
                        FayberConfigClient.LOGGER.info("PREVIEW: scroll set to " + target);
                    }
                }
            }
        });
    }

    /** A screen with one of every entry type, sized like a real mod's config. */
    public static FayberConfigScreen demoScreen() {
        DemoState s = DemoState.INSTANCE;
        return FayberConfigScreen.builder(Component.literal("Fayber Config Preview"), null, () -> {
                })
                .category("General")
                .intSlider("Max Render Distance", () -> s.renderDistance, v -> s.renderDistance = v, 0, 50000, 100,
                        5000)
                .tooltip("How far away waypoints stay visible.")
                .bool("Always on Top", () -> s.alwaysOnTop, v -> s.alwaysOnTop = v, true)
                .tooltip("Draw waypoints through terrain.")
                .bool("Screen-Edge Arrows", () -> s.edgeArrows, v -> s.edgeArrows = v)
                .select("Sorting", () -> s.sorting, v -> s.sorting = v, SORT_OPTIONS, Function.identity(), "Distance")
                .category("Beam")
                .bool("Enable Beacon Beam", () -> s.beamEnabled, v -> s.beamEnabled = v, true)
                .color("Beam Colour", () -> s.beamColor, v -> s.beamColor = v, 0xFF3B82F6)
                .floatSlider("Beam Width", () -> s.beamWidth, v -> s.beamWidth = v, 0.05f, 2.0f, 0.05f, 0.25f)
                .floatSlider("Beam Opacity", () -> s.beamAlpha, v -> s.beamAlpha = v, 0.1f, 1.0f, 0.05f)
                .floatSlider("Beam Height", () -> s.beamHeight, v -> s.beamHeight = v, 0.0f, 1024.0f, 16.0f, 384.0f)
                .category("Label Card")
                .bool("Floating Labels", () -> s.labels, v -> s.labels = v)
                .floatSlider("Pin Scale", () -> s.pinScale, v -> s.pinScale = v, 0.2f, 3.0f, 0.05f, 1.0f)
                .text("Default Name", () -> s.defaultName, v -> s.defaultName = v, 32, "New Waypoint")
                .tooltip("Name given to quick-added waypoints.")
                .keybind("Quick Add Key", () -> s.quickAddKey, v -> s.quickAddKey = v, GLFW_KEY_B)
                .cycle("Label Corner", () -> s.corner, v -> s.corner = v, CORNERS, Function.identity(), CORNERS[0])
                .category("Lists")
                .stringList("Blacklisted Dimensions", () -> s.dimensions, v -> s.dimensions = v,
                        List.of("minecraft:the_end"))
                .intList("Grid Sizes", () -> s.gridSizes, v -> s.gridSizes = v, List.of(8, 16, 32))
                .floatList("Lane Widths", () -> s.laneWidths, v -> s.laneWidths = v)
                .doubleList("Zoom Levels", () -> s.zoomLevels, v -> s.zoomLevels = v, List.of(0.5, 1.0, 2.0))
                .category("Danger Zone")
                .bool("Enable Death Waypoints", () -> s.death, v -> s.death = v)
                .doubleSlider("Auto-Remove Distance", () -> s.autoRemoveDist, v -> s.autoRemoveDist = v, 1.0, 32.0, 0.5,
                        16.0)
                .button("Reset Everything", () -> {
                })
                .build();
    }

    /** Corner options for the label cycle. */
    private static final String[] CORNERS = {"Top Left", "Top Right", "Bottom Left", "Bottom Right"};
    /** GLFW keycode for B, the quick-add keybind's declared default. */
    private static final int GLFW_KEY_B = 66;

    /** Mutable backing values for the demo screen. */
    private static final class DemoState {
        static final DemoState INSTANCE = new DemoState();

        int renderDistance = 2000;
        boolean alwaysOnTop = true;
        boolean edgeArrows = true;
        String sorting = "Created";
        boolean beamEnabled = true;
        int beamColor = 0xFF22C55E;
        float beamWidth = 0.20f;
        float beamAlpha = 0.65f;
        float beamHeight = 384.0f;
        boolean labels = true;
        float pinScale = 1.0f;
        String defaultName = "New Waypoint";
        int quickAddKey = 79; // GLFW_KEY_O; not the default, so the reset button shows
        String corner = "Bottom Right";
        List<String> dimensions = List.of("minecraft:the_nether");
        List<Integer> gridSizes = List.of(4, 8);
        List<Float> laneWidths = List.of(0.5f, 1.5f);
        List<Double> zoomLevels = List.of(0.75, 1.5);
        boolean death = true;
        double autoRemoveDist = 8.0;
    }
}
