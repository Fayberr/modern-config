package net.fayber.modernconfig.bridge.mixin;

import me.shedaniel.clothconfig2.gui.ClothConfigScreen;
import net.fayber.modernconfig.bridge.ClothBridge;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pins a "Use Modern Config" button into the bottom right corner of Cloth's own screen, so
 * there is a way back after using "Use original menu". Shared logic lives in
 * {@link ClothBridge#addModernButton}. Re-running init() on a window resize is safe, the
 * screen clears its widgets first.
 *
 * <p>The widget goes in through Screen.addRenderableWidget, widened by the access widener:
 * a @Shadow cannot reach a method the target class only inherits. require = 0 on purpose, if
 * a future Cloth version reshapes init() the cost is a missing button, not a broken screen.
 */
@Mixin(ClothConfigScreen.class)
public abstract class ClothScreenButtonMixin {
    @Inject(method = "init()V", at = @At("RETURN"), require = 0)
    private void modernconfig$addModernButton(CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        ClothBridge.addModernButton(self, self::addRenderableWidget);
    }
}
