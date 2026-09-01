package net.fayber.modernconfig.bridge.mixin;

import me.shedaniel.clothconfig2.impl.builders.KeyCodeBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Consumer;

/**
 * KeyCodeBuilder extends FieldBuilder directly, so the plumbing capture misses its save
 * consumer. Same trap as DropdownMenuBuilder, see {@link DropdownMenuBuilderAccessor}.
 */
@Mixin(KeyCodeBuilder.class)
public interface KeyCodeBuilderAccessor {
    @Accessor("saveConsumer")
    Consumer<?> modernconfig$saveConsumer();
}
