package net.fayber.fayberconfig.bridge.mixin;

import me.shedaniel.clothconfig2.gui.GlobalizedClothConfigScreen;
import net.fayber.fayberconfig.bridge.ClothBridge;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The globalized variant of the reverse button; see {@link ClothScreenButtonMixin}. Cloth's
 * {@code build()} returns this screen when the builder was marked globalized, and it is a sibling
 * of {@code ClothConfigScreen}, not a subclass, so it needs its own injection.
 */
@Mixin(GlobalizedClothConfigScreen.class)
public abstract class GlobalizedClothScreenButtonMixin {
    @Inject(method = "init()V", at = @At("RETURN"), require = 0)
    private void fayberconfig$addFayberButton(CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        ClothBridge.addFayberButton(self, self::addRenderableWidget);
    }
}
