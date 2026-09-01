package net.fayber.fayberconfig;

import com.mojang.brigadier.Command;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
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

        registerCommands();

        // Design workbench, only ever active with -Dfayberconfig.preview=true (dev runs).
        if (net.fayber.fayberconfig.dev.PreviewHook.enabled()) {
            net.fayber.fayberconfig.dev.PreviewHook.register();
        }
    }

    /**
     * {@code /fayberconfig demo}: opens the demo screen with one of every entry type, the same
     * screen the dev preview hook auto-opens. A support and showcase surface: it exercises the
     * whole builder API (and every reset-to-default button) without needing a mod that uses it.
     */
    private static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommands.literal("fayberconfig")
                        .then(ClientCommands.literal("demo").executes(context -> {
                            Minecraft client = context.getSource().getClient();
                            // Route through the client thread: setScreen is only safe there.
                            client.execute(() -> client.setScreen(
                                    net.fayber.fayberconfig.dev.PreviewHook.demoScreen()));
                            return Command.SINGLE_SUCCESS;
                        }))));
    }
}
