package net.fayber.fayberconfig.bridge.mixin;

import me.shedaniel.clothconfig2.impl.builders.AbstractRangeFieldBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads the protected {@code min}/{@code max} bounds a range builder holds.
 */
@Mixin(AbstractRangeFieldBuilder.class)
public interface RangeFieldBuilderAccessor {
    @Accessor("min")
    Object fayberconfig$min();

    @Accessor("max")
    Object fayberconfig$max();
}
