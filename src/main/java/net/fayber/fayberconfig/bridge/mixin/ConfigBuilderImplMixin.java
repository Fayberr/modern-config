package net.fayber.fayberconfig.bridge.mixin;

import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.impl.ConfigBuilderImpl;
import net.fayber.fayberconfig.bridge.ClothBridge;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/**
 * Swaps a finished Cloth screen for a Fayber Config screen.
 *
 * <p>Injected at {@code RETURN} rather than {@code HEAD} on purpose: letting Cloth build its screen
 * first costs one screen construction but means the original is available as a fallback (the
 * "Use original menu" button) and that a failed translation can return it untouched.
 *
 * <p>{@code ConfigBuilder} itself is an interface whose {@code build()} is abstract, so the mixin
 * targets the concrete {@code ConfigBuilderImpl}.
 */
@Mixin(ConfigBuilderImpl.class)
public abstract class ConfigBuilderImplMixin {
    @Shadow
    private Runnable savingRunnable;

    // categoryMap is final in ConfigBuilderImpl; a @Shadow without @Final is rejected at apply time.
    @Shadow
    @Final
    private Map<String, ConfigCategory> categoryMap;

    @Shadow
    public abstract Component getTitle();

    @Shadow
    public abstract Screen getParentScreen();

    @Inject(method = "build", at = @At("RETURN"), cancellable = true)
    private void fayberconfig$translate(CallbackInfoReturnable<Screen> cir) {
        Screen clothScreen = cir.getReturnValue();
        if (clothScreen == null) {
            return;
        }
        Screen translated = ClothBridge.translate(this.getTitle(), this.getParentScreen(),
                this.categoryMap.values(), this.savingRunnable, clothScreen);
        if (translated != null) {
            cir.setReturnValue(translated);
        }
    }
}
