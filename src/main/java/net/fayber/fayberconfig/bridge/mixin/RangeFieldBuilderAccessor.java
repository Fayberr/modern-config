package net.fayber.fayberconfig.bridge.mixin;

import me.shedaniel.clothconfig2.impl.builders.AbstractRangeFieldBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads the {@code min}/{@code max} bounds a range builder holds. They are {@code protected}, so a
 * plain cast cannot see them from this package; an accessor keeps it type-safe and avoids
 * reflection into private state.
 */
@Mixin(AbstractRangeFieldBuilder.class)
public interface RangeFieldBuilderAccessor {
    @Accessor("min")
    Object fayberconfig$min();

    @Accessor("max")
    Object fayberconfig$max();
}
