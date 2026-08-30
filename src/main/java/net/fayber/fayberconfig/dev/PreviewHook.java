package net.fayber.fayberconfig.dev;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fayber.fayberconfig.FayberConfigClient;
import net.fayber.fayberconfig.api.FayberConfigScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

/**
 * Design workbench. Opens a demo config screen automatically shortly after the dev client reaches
 * the title screen, so the look can be iterated on headlessly: run the client under Xvfb, grab a
 * frame, inspect the pixels, repeat (see {@code tools/preview.sh}).
 *
 * <p>Completely inert unless the JVM is started with {@code -Dfayberconfig.preview=true}, so this
 * class costs shipped builds one boolean check at init.
 */
public final class PreviewHook {
    private PreviewHook() {
    }

    public static boolean enabled() {
        return Boolean.getBoolean("fayberconfig.preview");
    }

    public static void register() {
        FayberConfigClient.LOGGER.info("Fayber Config preview hook armed");
        ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick() {
            private int ticks = 0;
            private boolean opened = false;

            @Override
            public void onEndTick(net.minecraft.client.Minecraft client) {
                if (this.opened) {
                    return;
                }
                if (!(client.screen instanceof TitleScreen)) {
                    return;
                }
                if (++this.ticks < 20) {
                    return;
                }
                this.opened = true;
                FayberConfigClient.LOGGER.info("PREVIEW: opening demo screen");
                client.setScreen(demoScreen());
            }
        });
    }

    /** A screen with one of every entry type, sized like a real mod's config. */
    public static FayberConfigScreen demoScreen() {
        DemoState s = DemoState.INSTANCE;
        return FayberConfigScreen.builder(Component.literal("Fayber Config Preview"), null, () -> {
                })
                .category("General")
                .intSlider("Max Render Distance", () -> s.renderDistance, v -> s.renderDistance = v, 0, 50000, 100)
                .tooltip("How far away waypoints stay visible.")
                .bool("Always on Top", () -> s.alwaysOnTop, v -> s.alwaysOnTop = v)
                .tooltip("Draw waypoints through terrain.")
                .bool("Screen-Edge Arrows", () -> s.edgeArrows, v -> s.edgeArrows = v)
                .category("Beam")
                .bool("Enable Beacon Beam", () -> s.beamEnabled, v -> s.beamEnabled = v)
                .floatSlider("Beam Width", () -> s.beamWidth, v -> s.beamWidth = v, 0.05f, 2.0f, 0.05f)
                .floatSlider("Beam Opacity", () -> s.beamAlpha, v -> s.beamAlpha = v, 0.1f, 1.0f, 0.05f)
                .floatSlider("Beam Height", () -> s.beamHeight, v -> s.beamHeight = v, 0.0f, 1024.0f, 16.0f)
                .category("Label Card")
                .bool("Floating Labels", () -> s.labels, v -> s.labels = v)
                .floatSlider("Pin Scale", () -> s.pinScale, v -> s.pinScale = v, 0.2f, 3.0f, 0.05f)
                .text("Default Name", () -> s.defaultName, v -> s.defaultName = v, 32)
                .tooltip("Name given to quick-added waypoints.")
                .category("Danger Zone")
                .bool("Enable Death Waypoints", () -> s.death, v -> s.death = v)
                .doubleSlider("Auto-Remove Distance", () -> s.autoRemoveDist, v -> s.autoRemoveDist = v, 1.0, 32.0, 0.5)
                .button("Reset Everything", () -> {
                })
                .build();
    }

    /** Mutable backing values for the demo screen. */
    private static final class DemoState {
        static final DemoState INSTANCE = new DemoState();

        int renderDistance = 2000;
        boolean alwaysOnTop = true;
        boolean edgeArrows = true;
        boolean beamEnabled = true;
        float beamWidth = 0.20f;
        float beamAlpha = 0.65f;
        float beamHeight = 384.0f;
        boolean labels = true;
        float pinScale = 1.0f;
        String defaultName = "New Waypoint";
        boolean death = true;
        double autoRemoveDist = 8.0;
    }
}
