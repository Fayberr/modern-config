package net.fayber.fayberconfig.bridge.mixin;

import com.google.common.collect.ImmutableList;
import me.shedaniel.clothconfig2.gui.entries.SelectionListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * A plain (non-enum) SelectionListEntry keeps its selectable values in this private field with
 * no public read path; the enum subclass fills it with all constants, which is why the enum
 * branch needs no accessor. The field's declared type is ImmutableList, so the accessor return
 * type must match it exactly or the mixin fails to apply.
 */
@Mixin(SelectionListEntry.class)
public interface SelectionListEntryAccessor {
    @Accessor("values")
    ImmutableList<?> fayberconfig$values();
}
