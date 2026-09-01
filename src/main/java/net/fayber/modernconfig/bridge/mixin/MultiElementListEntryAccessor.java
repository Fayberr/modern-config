package net.fayber.modernconfig.bridge.mixin;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.gui.entries.MultiElementListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * Children of an expandable group entry; they are built directly, not through a builder, and
 * only live in this private field. Return type must be exactly List or the mixin won't apply.
 */
@Mixin(MultiElementListEntry.class)
public interface MultiElementListEntryAccessor {
    @Accessor("entries")
    List<AbstractConfigListEntry<?>> modernconfig$entries();
}
