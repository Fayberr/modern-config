package net.fayber.fayberconfig.bridge.mixin;

import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Consumer;

/**
 * DropdownMenuBuilder extends {@code FieldBuilder} directly, not {@code AbstractFieldBuilder},
 * so the plumbing capture could never see its save consumer: every dropdown entry was silently
 * recorded with a null consumer and refused at translation time, even though the 1.2.0 code had
 * a whole dropdown branch. The base {@code FieldBuilder} holds no consumer at all; each direct
 * subclass keeps its own.
 */
@Mixin(DropdownMenuBuilder.class)
public interface DropdownMenuBuilderAccessor {
    @Accessor("saveConsumer")
    Consumer<?> fayberconfig$saveConsumer();
}
