package net.fayber.fayberconfig.bridge.mixin;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.impl.builders.AbstractFieldBuilder;
import me.shedaniel.clothconfig2.impl.builders.AbstractRangeFieldBuilder;
import me.shedaniel.clothconfig2.impl.builders.FieldBuilder;
import net.fayber.fayberconfig.bridge.ClothBridge;
import net.fayber.fayberconfig.bridge.EntryPlumbing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

/**
 * Captures the value plumbing of every Cloth entry as it is built. finishBuilding is the one
 * point every entry builder passes through, so a single injection covers all entry kinds.
 * Captured is exactly what the finished entry no longer exposes: the save consumer (public on
 * the builder, private on the entry) and the range bounds. See {@link ClothBridge}.
 */
@Mixin(FieldBuilder.class)
public abstract class FieldBuilderMixin {
    // The generic parameter erases to AbstractConfigListEntry, so the descriptor must say so:
    // a mixin signature of (Object) would silently fail to match at apply time.
    @Inject(method = "finishBuilding(Lme/shedaniel/clothconfig2/api/AbstractConfigListEntry;)"
            + "Lme/shedaniel/clothconfig2/api/AbstractConfigListEntry;", at = @At("RETURN"))
    @SuppressWarnings("unchecked")
    private void fayberconfig$capturePlumbing(AbstractConfigListEntry<?> entry,
                                              CallbackInfoReturnable<AbstractConfigListEntry<?>> cir) {
        Object built = cir.getReturnValue();
        if (built == null) {
            return;
        }
        try {
            Consumer<Object> saveConsumer = null;
            if ((Object) this instanceof AbstractFieldBuilder<?, ?, ?> fieldBuilder) {
                saveConsumer = (Consumer<Object>) fieldBuilder.getSaveConsumer();
            } else if ((Object) this instanceof KeyCodeBuilderAccessor keyCodeBuilder) {
                // KeyCodeBuilder and DropdownMenuBuilder keep their save consumer in their own
                // field, invisible to the AbstractFieldBuilder read above.
                saveConsumer = (Consumer<Object>) keyCodeBuilder.fayberconfig$saveConsumer();
            } else if ((Object) this instanceof DropdownMenuBuilderAccessor dropdownBuilder) {
                saveConsumer = (Consumer<Object>) dropdownBuilder.fayberconfig$saveConsumer();
            }
            Object min = null;
            Object max = null;
            if ((Object) this instanceof AbstractRangeFieldBuilder<?, ?, ?> rangeBuilder) {
                min = ((RangeFieldBuilderAccessor) rangeBuilder).fayberconfig$min();
                max = ((RangeFieldBuilderAccessor) rangeBuilder).fayberconfig$max();
            }
            ClothBridge.PLUMBING.put(built, new EntryPlumbing(saveConsumer, min, max));
        } catch (Throwable t) {
            // Missing plumbing means the entry is not translated; never break Cloth itself.
            ClothBridge.LOGGER.debug("Could not capture entry plumbing", t);
        }
    }
}
