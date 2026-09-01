package net.fayber.fayberconfig.bridge.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Applies the Cloth bridge mixins only when Cloth Config is installed, otherwise they would
 * target missing classes and crash the game. Running without Cloth is the normal case.
 */
public class ClothBridgeMixinPlugin implements IMixinConfigPlugin {
    private static final String CLOTH_MOD_ID = "cloth-config";

    private boolean clothPresent;

    @Override
    public void onLoad(String mixinPackage) {
        this.clothPresent = FabricLoader.getInstance().isModLoaded(CLOTH_MOD_ID);
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return this.clothPresent;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
