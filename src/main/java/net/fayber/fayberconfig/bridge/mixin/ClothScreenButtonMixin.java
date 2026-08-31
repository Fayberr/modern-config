package net.fayber.fayberconfig.bridge.mixin;

import me.shedaniel.clothconfig2.gui.ClothConfigScreen;
import net.fayber.fayberconfig.bridge.ClothBridge;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pins a "Use Fayber Config" button into the bottom right corner of Cloth's own screen, so the
 * direction is not one-way: after using "Use original menu" there is a way back.
 *
 * <p>{@code build()} returns either this or {@code GlobalizedClothConfigScreen} (a sibling class,
 * not a subclass), so each concrete screen gets its own mixin; the shared logic, including the
 * whether-a-translation-exists check, lives in {@link ClothBridge#addFayberButton}. Re-running
 * {@code init()} on a window resize is safe: the screen clears its widgets first.
 *
 * <p>The shadow uses the erased signature of the generic {@code addRenderableWidget} (the type
 * parameter's first bound), so it matches the descriptor the mixin apply step looks for.
 */
@Mixin(ClothConfigScreen.class)
public abstract class ClothScreenButtonMixin {
    @Shadow
    protected abstract GuiEventListener addRenderableWidget(GuiEventListener widget);

    @Inject(method = "init()V", at = @At("RETURN"))
    private void fayberconfig$addFayberButton(CallbackInfo ci) {
        ClothBridge.addFayberButton((Screen) (Object) this, this::addRenderableWidget);
    }
}
