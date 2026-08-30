package net.fayber.fayberconfig;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fayber Config: slim, modern config screens for Fayber's mods.
 *
 * <p>Consumer mods add this as an OPTIONAL dependency (never a hard one): they check
 * {@code FabricLoader.getInstance().isModLoaded("fayberconfig")} at runtime and build a
 * {@link net.fayber.fayberconfig.api.FayberConfigScreen} through
 * {@link net.fayber.fayberconfig.api.FayberConfigScreen#builder}. When the mod is absent they
 * fall back to whatever they used before (Cloth Config, a vanilla screen, or nothing).
 */
public class FayberConfigClient implements ClientModInitializer {
    public static final String MOD_ID = "fayberconfig";
    public static final Logger LOGGER = LoggerFactory.getLogger("FayberConfig");

    @Override
    public void onInitializeClient() {
        LOGGER.info("Fayber Config initialized");

        // Design workbench, only ever active with -Dfayberconfig.preview=true (dev runs).
        if (net.fayber.fayberconfig.dev.PreviewHook.enabled()) {
            net.fayber.fayberconfig.dev.PreviewHook.register();
        }
    }
}
