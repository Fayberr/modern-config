package net.fayber.modernconfig.bridge.mixin;

import com.google.common.collect.ImmutableList;
import me.shedaniel.clothconfig2.gui.entries.SelectionListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Only the plain (non-enum) SelectionListEntry needs this, the enum subclass fills the field
 * with all constants on its own. Return type must be ImmutableList exactly or the mixin
 * won't apply.
 */
@Mixin(SelectionListEntry.class)
public interface SelectionListEntryAccessor {
    @Accessor("values")
    ImmutableList<?> modernconfig$values();
}
