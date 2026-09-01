package net.fayber.modernconfig;

import com.mojang.brigadier.Command;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Modern Config: slim, one-column config screens for Fayber's mods.
 *
 * <p>Consumer mods add this as an optional dependency, check
 * {@code FabricLoader.getInstance().isModLoaded("modernconfig")} at runtime and build a
 * {@link net.fayber.modernconfig.api.ModernConfigScreen} through
 * {@link net.fayber.modernconfig.api.ModernConfigScreen#builder}. When the mod is absent they
 * fall back to whatever they used before.
 */
public class ModernConfigClient implements ClientModInitializer {
    public static final String MOD_ID = "modernconfig";
    public static final Logger LOGGER = LoggerFactory.getLogger("ModernConfig");

    @Override
    public void onInitializeClient() {
        LOGGER.info("Modern Config initialized");

        registerCommands();

        // Design workbench, only ever active with -Dmodernconfig.preview=true (dev runs).
        if (net.fayber.modernconfig.dev.PreviewHook.enabled()) {
            net.fayber.modernconfig.dev.PreviewHook.register();
        }
    }

    /**
     * {@code /modernconfig demo}: opens the demo screen with one of every entry type, the same
     * screen the dev preview hook auto-opens. Exercises the whole builder API without needing
     * a mod that uses it.
     */
    private static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommands.literal("modernconfig")
                        .then(ClientCommands.literal("demo").executes(context -> {
                            Minecraft client = context.getSource().getClient();
                            // Route through the client thread: setScreen is only safe there.
                            client.execute(() -> client.setScreen(
                                    net.fayber.modernconfig.dev.PreviewHook.demoScreen()));
                            return Command.SINGLE_SUCCESS;
                        }))));
    }
}
