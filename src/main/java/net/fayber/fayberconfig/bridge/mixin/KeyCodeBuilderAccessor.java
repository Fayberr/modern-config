package net.fayber.fayberconfig.bridge.mixin;

import me.shedaniel.clothconfig2.impl.builders.KeyCodeBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Consumer;

/**
 * KeyCodeBuilder extends {@code FieldBuilder} directly, not {@code AbstractFieldBuilder}, so the
 * plumbing capture (which reads the save consumer off {@code AbstractFieldBuilder}) misses it:
 * the consumer lives in this class's own field. Same trap as DropdownMenuBuilder, see
 * {@link DropdownMenuBuilderAccessor}.
 */
@Mixin(KeyCodeBuilder.class)
public interface KeyCodeBuilderAccessor {
    @Accessor("saveConsumer")
    Consumer<?> fayberconfig$saveConsumer();
}
