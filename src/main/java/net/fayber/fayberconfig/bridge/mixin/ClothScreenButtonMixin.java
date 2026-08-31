package net.fayber.fayberconfig.bridge.mixin;

import me.shedaniel.clothconfig2.gui.ClothConfigScreen;
import net.fayber.fayberconfig.bridge.ClothBridge;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
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
 * <p>The widget goes in through {@code Screen.addRenderableWidget}, widened to accessible by
 * {@code fayberconfig.accesswidener}: a {@code @Shadow} cannot reach a method the target class
 * only inherits, which is exactly how the first attempt of this mixin failed to apply. The
 * injection is {@code require = 0} on purpose: if a future Cloth version reshapes {@code init()},
 * the cost is a missing button, not a broken config screen.
 */
@Mixin(ClothConfigScreen.class)
public abstract class ClothScreenButtonMixin {
    @Inject(method = "init()V", at = @At("RETURN"), require = 0)
    private void fayberconfig$addFayberButton(CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        ClothBridge.addFayberButton(self, self::addRenderableWidget);
    }
}
