package net.fayber.fayberconfig.bridge.mixin;

import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Consumer;

/**
 * DropdownMenuBuilder extends FieldBuilder directly and the base class holds no save consumer,
 * so before this accessor every dropdown entry was recorded with a null consumer and refused
 * at translation time. Each direct subclass keeps its own consumer field.
 */
@Mixin(DropdownMenuBuilder.class)
public interface DropdownMenuBuilderAccessor {
    @Accessor("saveConsumer")
    Consumer<?> fayberconfig$saveConsumer();
}
