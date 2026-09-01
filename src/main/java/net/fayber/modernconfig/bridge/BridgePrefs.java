package net.fayber.modernconfig.bridge;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Settings for the Cloth Config bridge, stored in {@code config/modernconfig-bridge.json}.
 * Parsed by hand to keep the bridge dependency-free, the file is tiny and read once. A missing
 * or unreadable file means defaults, never a crash: this runs inside a mixin on somebody
 * else's config screen.
 */
public final class BridgePrefs {
    private static final String FILE_NAME = "modernconfig-bridge.json";

    private static boolean loaded;
    private static boolean enabled = true;
    private static boolean showFallbackButton = true;
    private static final Set<String> disabledTitles = new HashSet<>();

    private BridgePrefs() {
    }

    /** Whether Cloth screens should be translated at all. */
    public static boolean enabled() {
        load();
        return enabled;
    }

    /** Whether the translated screen offers a button back to the original Cloth screen. */
    public static boolean showFallbackButton() {
        load();
        return showFallbackButton;
    }

    /**
     * Per-screen opt-out: a title listed in {@code disabledTitles} keeps its original Cloth
     * look. Titles are the key because a builder does not know which mod created it.
     */
    public static boolean enabledFor(String title) {
        load();
        return enabled && !disabledTitles.contains(title);
    }

    private static synchronized void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        if (!Files.isRegularFile(path)) {
            return;
        }
        try {
            JsonObject json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            if (json.has("enabled")) {
                enabled = json.get("enabled").getAsBoolean();
            }
            if (json.has("showFallbackButton")) {
                showFallbackButton = json.get("showFallbackButton").getAsBoolean();
            }
            if (json.has("disabledTitles")) {
                json.getAsJsonArray("disabledTitles").forEach(e -> disabledTitles.add(e.getAsString()));
            }
        } catch (IOException | RuntimeException e) {
            ClothBridge.LOGGER.warn("Could not read {}, using defaults", FILE_NAME, e);
        }
    }
}
